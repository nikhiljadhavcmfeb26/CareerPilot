package com.careerpilot.jobservice.service.impl;

import com.careerpilot.common.dto.PagedResult;
import com.careerpilot.common.exception.BadRequestException;
import com.careerpilot.common.exception.ResourceNotFoundException;
import com.careerpilot.jobservice.client.ApplicationServiceClient;
import com.careerpilot.jobservice.client.UserServiceClient;
import com.careerpilot.jobservice.dto.CreateJobRequest;
import com.careerpilot.jobservice.dto.JobDto;
import com.careerpilot.jobservice.dto.UpdateJobRequest;
import com.careerpilot.jobservice.entity.Job;
import com.careerpilot.jobservice.enums.ExperienceLevel;
import com.careerpilot.jobservice.enums.JobStatus;
import com.careerpilot.jobservice.enums.JobType;
import com.careerpilot.jobservice.repository.JobRepository;
import com.careerpilot.jobservice.repository.JobSpecifications;
import com.careerpilot.jobservice.service.JobService;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class JobServiceImpl implements JobService {

    private static final Logger log = LoggerFactory.getLogger(JobServiceImpl.class);

    private final JobRepository jobRepository;
    private final UserServiceClient userServiceClient;
    private final ApplicationServiceClient applicationServiceClient;

    public JobServiceImpl(JobRepository jobRepository, UserServiceClient userServiceClient,
                           ApplicationServiceClient applicationServiceClient) {
        this.jobRepository = jobRepository;
        this.userServiceClient = userServiceClient;
        this.applicationServiceClient = applicationServiceClient;
    }

    @Override
    @Transactional
    public JobDto createJob(int userId, CreateJobRequest request) {
        EmployerCompany company = getEmployerCompany(userId);

        Job job = new Job();
        job.setCompanyId(company.companyId());
        job.setTitle(request.getTitle());
        job.setDescription(request.getDescription());
        job.setRequirements(request.getRequirements());
        job.setLocation(request.getLocation());
        job.setJobType(request.getJobType());
        job.setSalaryMin(request.getSalaryMin());
        job.setSalaryMax(request.getSalaryMax());
        job.setExperienceLevel(request.getExperienceLevel());
        job.setStatus(JobStatus.Draft);

        jobRepository.save(job);
        return toDto(job);
    }

    @Override
    @Transactional
    public JobDto updateJob(int userId, int jobId, UpdateJobRequest request) {
        EmployerCompany company = getEmployerCompany(userId);
        Job job = findJob(jobId);

        if (!job.getCompanyId().equals(company.companyId())) {
            throw new BadRequestException("You can only edit your own jobs.");
        }

        job.setTitle(request.getTitle());
        job.setDescription(request.getDescription());
        job.setRequirements(request.getRequirements());
        job.setLocation(request.getLocation());
        job.setJobType(request.getJobType());
        job.setSalaryMin(request.getSalaryMin());
        job.setSalaryMax(request.getSalaryMax());
        job.setExperienceLevel(request.getExperienceLevel());

        jobRepository.save(job);
        return toDto(job);
    }

    @Override
    @Transactional
    public void deleteJob(int userId, int jobId) {
        EmployerCompany company = getEmployerCompany(userId);
        Job job = findJob(jobId);

        if (!job.getCompanyId().equals(company.companyId())) {
            throw new BadRequestException("You can only delete your own jobs.");
        }

        jobRepository.delete(job);
    }

    @Override
    @Transactional
    public void adminDeleteJob(int jobId) {
        Job job = findJob(jobId);
        jobRepository.delete(job);
    }

    @Override
    public JobDto getJobById(int jobId) {
        return toDto(findJob(jobId));
    }

    @Override
    public PagedResult<JobDto> searchJobs(String keyword, String location, JobType jobType,
                                           ExperienceLevel experienceLevel, int page, int pageSize) {
        List<Integer> matchingCompanyIds = List.of();
        if (keyword != null && !keyword.isBlank()) {
            try {
                matchingCompanyIds = userServiceClient.searchCompanyIds(keyword);
            } catch (Exception ex) {
                log.warn("user-service unavailable while resolving company-name matches for keyword '{}', falling back to title/description only", keyword);
            }
        }

        var spec = JobSpecifications.search(keyword, location, jobType, experienceLevel, JobStatus.Published, matchingCompanyIds);
        Page<Job> result = jobRepository.findAll(spec, PageRequest.of(Math.max(page - 1, 0), pageSize));

        List<JobDto> items = result.getContent().stream().map(this::toDto).toList();
        return new PagedResult<>(items, page, pageSize, result.getTotalElements());
    }

    @Override
    public List<JobDto> getMyJobs(int userId) {
        EmployerCompany company = getEmployerCompany(userId);
        return jobRepository.findByCompanyIdOrderByCreatedAtDesc(company.companyId()).stream().map(this::toDto).toList();
    }

    @Override
    public List<JobDto> getAllJobsAdmin() {
        return jobRepository.findAll().stream().map(this::toDto).toList();
    }

    @Override
    @Transactional
    public void publishJob(int userId, int jobId) {
        EmployerCompany company = getEmployerCompany(userId);
        Job job = findJob(jobId);

        if (!job.getCompanyId().equals(company.companyId())) {
            throw new BadRequestException("You can only publish your own jobs.");
        }
        if (!company.isApproved()) {
            throw new BadRequestException("Company must be approved before publishing jobs.");
        }

        job.setStatus(JobStatus.Published);
        job.setPublishedAt(LocalDateTime.now());
        jobRepository.save(job);
    }

    @Override
    @Transactional
    public void closeJob(int userId, int jobId) {
        EmployerCompany company = getEmployerCompany(userId);
        Job job = findJob(jobId);

        if (!job.getCompanyId().equals(company.companyId())) {
            throw new BadRequestException("You can only close your own jobs.");
        }

        job.setStatus(JobStatus.Closed);
        jobRepository.save(job);
    }

    private Job findJob(int jobId) {
        return jobRepository.findById(jobId).orElseThrow(() -> new ResourceNotFoundException("Job not found."));
    }

    /**
     * Mirrors JobService.cs's private GetEmployerCompanyAsync, which threw
     * two distinct messages depending on exactly what was missing ("Only
     * employers can manage jobs." vs "Register your company first."). That
     * distinction is preserved with two separate Feign calls rather than one,
     * since a single 404 from user-service can't otherwise tell us which of
     * the two cases actually happened.
     */
    private EmployerCompany getEmployerCompany(int userId) {
        UserServiceClient.ProfileSummary summary;
        try {
            summary = userServiceClient.getProfileSummary(userId);
        } catch (Exception ex) {
            log.error("user-service unavailable while resolving employer profile for user {}", userId, ex);
            throw new BadRequestException("Could not verify your employer account right now. Please try again.");
        }

        if (summary == null || summary.employerProfileId() == null) {
            throw new BadRequestException("Only employers can manage jobs.");
        }

        try {
            UserServiceClient.CompanyLookup lookup = userServiceClient.getCompanyForEmployerUser(userId);
            return new EmployerCompany(lookup.companyId(), lookup.isApproved());
        } catch (FeignException.NotFound ex) {
            throw new BadRequestException("Register your company first.");
        } catch (Exception ex) {
            log.error("user-service unavailable while resolving company for employer user {}", userId, ex);
            throw new BadRequestException("Could not verify your company right now. Please try again.");
        }
    }

    private JobDto toDto(Job job) {
        JobDto dto = new JobDto();
        dto.setId(job.getId());
        dto.setTitle(job.getTitle());
        dto.setDescription(job.getDescription());
        dto.setRequirements(job.getRequirements());
        dto.setLocation(job.getLocation());
        dto.setJobType(job.getJobType());
        dto.setSalaryMin(job.getSalaryMin());
        dto.setSalaryMax(job.getSalaryMax());
        dto.setExperienceLevel(job.getExperienceLevel());
        dto.setStatus(job.getStatus());
        dto.setCreatedAt(job.getCreatedAt());
        dto.setPublishedAt(job.getPublishedAt());
        dto.setCompanyId(job.getCompanyId());

        try {
            UserServiceClient.CompanySummary summary = userServiceClient.getCompanySummary(job.getCompanyId());
            dto.setCompanyName(summary != null ? summary.name() : null);
        } catch (Exception ex) {
            log.debug("user-service unavailable while hydrating company name for job {}", job.getId());
        }

        try {
            dto.setApplicationCount(applicationServiceClient.countByJob(job.getId()));
        } catch (Exception ex) {
            dto.setApplicationCount(0);
        }

        return dto;
    }

    private record EmployerCompany(Integer companyId, boolean isApproved) {
    }
}
