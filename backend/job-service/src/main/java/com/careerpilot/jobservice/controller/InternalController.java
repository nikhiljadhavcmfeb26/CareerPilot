package com.careerpilot.jobservice.controller;

import com.careerpilot.common.exception.ResourceNotFoundException;
import com.careerpilot.jobservice.dto.internal.EmployerJobStatsResponse;
import com.careerpilot.jobservice.dto.internal.JobLookupResponse;
import com.careerpilot.jobservice.dto.internal.JobStatsResponse;
import com.careerpilot.jobservice.entity.Job;
import com.careerpilot.jobservice.enums.JobStatus;
import com.careerpilot.jobservice.repository.BookmarkRepository;
import com.careerpilot.jobservice.repository.JobRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Service-to-service only, same story as every other service's
 * InternalController - no /internal/** route exists in api-gateway.yml.
 */
@RestController
@RequestMapping("/internal/jobs")
public class InternalController {

    private final JobRepository jobRepository;
    private final BookmarkRepository bookmarkRepository;

    public InternalController(JobRepository jobRepository, BookmarkRepository bookmarkRepository) {
        this.jobRepository = jobRepository;
        this.bookmarkRepository = bookmarkRepository;
    }

    /** Called by user-service to populate CompanyDto.jobCount. */
    @GetMapping("/count-by-company/{companyId}")
    public int countByCompany(@PathVariable("companyId") Integer companyId) {
        return jobRepository.countByCompanyId(companyId);
    }

    /** Called by application-service to validate a job exists/is published before accepting an application, and to hydrate ApplicationDto.JobTitle. */
    @GetMapping("/{jobId}")
    public JobLookupResponse getJob(@PathVariable("jobId") Integer jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found."));
        return new JobLookupResponse(job.getId(), job.getTitle(), job.getCompanyId(), job.getStatus().name(),
                job.getDescription(), job.getRequirements());
    }

    /** Called by auth-service's admin dashboard aggregation. */
    @GetMapping("/stats")
    public JobStatsResponse getStats() {
        return new JobStatsResponse((int) jobRepository.count(), jobRepository.countByStatus(JobStatus.Published));
    }

    /**
     * Called by auth-service's employer dashboard aggregation. Returns the job
     * ids as well as the counts so auth-service can ask application-service for
     * this employer's application totals in a single follow-up call, rather
     * than one call per job.
     */
    @GetMapping("/stats/by-company/{companyId}")
    public EmployerJobStatsResponse getStatsByCompany(@PathVariable("companyId") Integer companyId) {
        List<Job> jobs = jobRepository.findByCompanyIdOrderByCreatedAtDesc(companyId);
        int published = (int) jobs.stream().filter(j -> j.getStatus() == JobStatus.Published).count();
        return new EmployerJobStatsResponse(jobs.size(), published, jobs.stream().map(Job::getId).toList());
    }

    /** Called by auth-service's job seeker dashboard aggregation - bookmarks live in this service. */
    @GetMapping("/bookmarks/count-by-profile/{jobSeekerProfileId}")
    public int countBookmarksByProfile(@PathVariable("jobSeekerProfileId") Integer jobSeekerProfileId) {
        return bookmarkRepository.countByJobSeekerProfileId(jobSeekerProfileId);
    }

    /** Called by ai-service's job recommendations feature - the 50 most recently published jobs to compare a candidate's resume against. */
    @GetMapping("/published")
    public List<JobLookupResponse> getPublishedJobs() {
        return jobRepository.findTop50ByStatusOrderByPublishedAtDesc(JobStatus.Published).stream()
                .map(job -> new JobLookupResponse(job.getId(), job.getTitle(), job.getCompanyId(), job.getStatus().name(),
                        job.getDescription(), job.getRequirements()))
                .toList();
    }
}
