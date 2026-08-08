package com.careerpilot.applicationservice.service.impl;

import com.careerpilot.applicationservice.client.AiServiceClient;
import com.careerpilot.applicationservice.client.AuthServiceClient;
import com.careerpilot.applicationservice.client.JobServiceClient;
import com.careerpilot.applicationservice.client.NotificationServiceClient;
import com.careerpilot.applicationservice.client.UserServiceClient;
import com.careerpilot.applicationservice.dto.ApplicationDto;
import com.careerpilot.applicationservice.dto.CreateApplicationRequest;
import com.careerpilot.applicationservice.dto.UpdateApplicationStatusRequest;
import com.careerpilot.applicationservice.entity.JobApplication;
import com.careerpilot.applicationservice.enums.ApplicationStatus;
import com.careerpilot.applicationservice.repository.JobApplicationRepository;
import com.careerpilot.applicationservice.service.ApplicationService;
import com.careerpilot.common.exception.BadRequestException;
import com.careerpilot.common.exception.ResourceNotFoundException;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ApplicationServiceImpl implements ApplicationService {

    private static final Logger log = LoggerFactory.getLogger(ApplicationServiceImpl.class);
    private static final String JOB_PUBLISHED_STATUS = "Published";

    /**
     * Statuses an employer is allowed to set. Pending is the initial state
     * assigned at apply time, and Withdrawn belongs to the candidate - neither
     * is an employer decision, but updateStatus() previously accepted any
     * value in the enum, so an employer could mark an application Withdrawn on
     * the candidate's behalf or silently reset it to Pending.
     */
    private static final Set<ApplicationStatus> EMPLOYER_SETTABLE_STATUSES = EnumSet.of(
            ApplicationStatus.Reviewed, ApplicationStatus.Shortlisted,
            ApplicationStatus.Rejected, ApplicationStatus.Accepted);

    private final JobApplicationRepository applicationRepository;
    private final AuthServiceClient authServiceClient;
    private final UserServiceClient userServiceClient;
    private final JobServiceClient jobServiceClient;
    private final NotificationServiceClient notificationServiceClient;
    private final AiServiceClient aiServiceClient;

    public ApplicationServiceImpl(JobApplicationRepository applicationRepository, AuthServiceClient authServiceClient,
                                   UserServiceClient userServiceClient, JobServiceClient jobServiceClient,
                                   NotificationServiceClient notificationServiceClient, AiServiceClient aiServiceClient) {
        this.applicationRepository = applicationRepository;
        this.authServiceClient = authServiceClient;
        this.userServiceClient = userServiceClient;
        this.jobServiceClient = jobServiceClient;
        this.notificationServiceClient = notificationServiceClient;
        this.aiServiceClient = aiServiceClient;
    }

    @Override
    @Transactional
    public ApplicationDto apply(int userId, CreateApplicationRequest request) {
        int jobSeekerProfileId = getJobSeekerProfileId(userId);
        JobServiceClient.JobLookup job = getJob(request.getJobId());

        if (!JOB_PUBLISHED_STATUS.equals(job.status())) {
            throw new BadRequestException("Job is not accepting applications.");
        }

        JobApplication existing = applicationRepository
                .findByJobIdAndJobSeekerProfileId(request.getJobId(), jobSeekerProfileId)
                .orElse(null);

        if (existing != null && existing.getStatus() != ApplicationStatus.Withdrawn) {
            throw new BadRequestException("Already applied");
        }

        // Resolve the resume BEFORE writing anything: an id the candidate
        // doesn't own must fail the whole apply, not leave a half-written row.
        UserServiceClient.ResumeReference resume = resolveResume(jobSeekerProfileId, request.getResumeId());

        if (existing != null) {
            // Re-apply after a withdrawal - update the existing row in place
            // rather than creating a new one, matching ApplicationService.cs exactly.
            existing.setStatus(ApplicationStatus.Pending);
            existing.setCoverLetter(request.getCoverLetter());
            existing.setResumeId(resume.resumeId());
            existing.setResumeFileName(resume.fileName());
            existing.setAppliedAt(LocalDateTime.now());
            applicationRepository.save(existing);
            return toDto(existing);
        }

        JobApplication application = new JobApplication();
        application.setJobId(request.getJobId());
        application.setJobSeekerProfileId(jobSeekerProfileId);
        application.setUserId(userId);
        application.setResumeId(resume.resumeId());
        application.setResumeFileName(resume.fileName());
        application.setCoverLetter(request.getCoverLetter());
        application.setStatus(ApplicationStatus.Pending);
        application.setAppliedAt(LocalDateTime.now());

        applicationRepository.save(application);
        return toDto(application);
    }

    /**
     * THE MISSING LINK for the employer resume feature. The React apply form
     * never sent a resumeId, so every application was written with
     * resume_id = NULL - which meant the employer's "View Resume" endpoint
     * always answered "this applicant did not attach a resume", and AI
     * candidate screening skipped every applicant. The frontend now sends one;
     * this validates it belongs to the caller, and falls back to their default
     * resume when it's absent. Applying with no resume at all is still allowed
     * (a candidate who has uploaded none), exactly as before.
     */
    private UserServiceClient.ResumeReference resolveResume(int jobSeekerProfileId, Integer requestedResumeId) {
        try {
            UserServiceClient.ResumeReference reference =
                    userServiceClient.resolveResumeForApplication(jobSeekerProfileId, requestedResumeId);
            return reference != null ? reference : new UserServiceClient.ResumeReference(null, null);
        } catch (FeignException.BadRequest ex) {
            throw new BadRequestException("You can only attach your own resume.");
        } catch (FeignException.NotFound ex) {
            throw new BadRequestException("The selected resume could not be found.");
        } catch (Exception ex) {
            log.error("user-service unavailable while resolving resume for profile {}", jobSeekerProfileId, ex);
            throw new BadRequestException("Could not attach your resume right now. Please try again.");
        }
    }

    @Override
    @Transactional
    public void withdraw(int userId, int applicationId) {
        int jobSeekerProfileId = getJobSeekerProfileId(userId);
        JobApplication application = findApplication(applicationId);

        if (!application.getJobSeekerProfileId().equals(jobSeekerProfileId)) {
            throw new BadRequestException("You can only withdraw your own applications.");
        }

        application.setStatus(ApplicationStatus.Withdrawn);
        applicationRepository.save(application);
    }

    /**
     * toDto() makes 3-4 cross-service calls per row, so mapping a list one row
     * at a time meant a 50-application page fired 150+ HTTP requests. Both list
     * endpoints now share a per-request cache: a candidate's applications
     * usually repeat the same handful of jobs, and an employer's applicant list
     * is entirely one job. Single-row paths (apply/withdraw) are unchanged.
     */
    @Override
    public List<ApplicationDto> getMyApplications(int userId) {
        int jobSeekerProfileId = getJobSeekerProfileId(userId);
        LookupCache cache = new LookupCache();
        return applicationRepository.findByJobSeekerProfileIdOrderByAppliedAtDesc(jobSeekerProfileId)
                .stream().map(a -> toDto(a, cache)).toList();
    }

    @Override
    public List<ApplicationDto> getJobApplicants(int userId, int jobId) {
        Integer companyId = getEmployerCompanyId(userId, "Only employers can view applicants.");
        JobServiceClient.JobLookup job = getJob(jobId);

        if (!job.companyId().equals(companyId)) {
            throw new BadRequestException("You can only view applicants for your jobs.");
        }

        LookupCache cache = new LookupCache();
        return applicationRepository.findByJobIdOrderByAppliedAtDesc(jobId).stream()
                .map(a -> toDto(a, cache)).toList();
    }

    @Override
    @Transactional
    public void updateStatus(int userId, int applicationId, UpdateApplicationStatusRequest request) {
        Integer companyId = getEmployerCompanyId(userId, "Only employers can update application status.");
        JobApplication application = findApplication(applicationId);
        JobServiceClient.JobLookup job = getJob(application.getJobId());

        if (!job.companyId().equals(companyId)) {
            throw new BadRequestException("You can only update applications for your jobs.");
        }

        if (request.getStatus() == null || !EMPLOYER_SETTABLE_STATUSES.contains(request.getStatus())) {
            throw new BadRequestException("Invalid application status.");
        }

        if (application.getStatus() == ApplicationStatus.Withdrawn) {
            throw new BadRequestException("This application was withdrawn by the candidate.");
        }

        // No-op updates used to re-fire the status email (and, for Rejected,
        // regenerate AI feedback and email the candidate all over again).
        if (application.getStatus() == request.getStatus()) {
            return;
        }

        application.setStatus(request.getStatus());
        applicationRepository.save(application);

        if (application.getStatus() == ApplicationStatus.Rejected) {
            triggerRejectionFeedbackBestEffort(userId, application);
        } else {
            sendStatusEmailBestEffort(application, job);
        }
    }

    /**
     * Per your spec: "When an employer rejects a candidate, generate
     * professional feedback... automatically email the generated feedback to
     * the candidate." ai-service decides internally whether the employer's
     * subscription allows a Gemini-personalized version; either way
     * notification-service ends up sending exactly one email for the
     * rejection (personalized or its built-in professional default) - so
     * this replaces the generic status email for this one status rather than
     * sending both.
     */
    private void triggerRejectionFeedbackBestEffort(int employerUserId, JobApplication application) {
        try {
            AuthServiceClient.UserBasicInfo candidate = authServiceClient.getUserBasicInfo(application.getUserId());
            if (candidate == null) {
                return;
            }
            aiServiceClient.triggerRejectionFeedback(new AiServiceClient.RejectionFeedbackRequest(
                    employerUserId, application.getJobId(), application.getJobSeekerProfileId(),
                    candidate.email(), candidate.firstName()));
        } catch (Exception ex) {
            log.error("Failed to trigger AI rejection feedback for application {}", application.getId(), ex);
        }
    }

    /**
     * New - ApplicationService.cs's UpdateStatusAsync has no email
     * side-effect at all in the original; this is the "Application Status
     * Email" capability from your spec. Best-effort: a status update should
     * still succeed even if the email can't be sent right now.
     */
    private void sendStatusEmailBestEffort(JobApplication application, JobServiceClient.JobLookup job) {
        try {
            AuthServiceClient.UserBasicInfo applicant = authServiceClient.getUserBasicInfo(application.getUserId());
            String companyName = null;
            try {
                UserServiceClient.CompanySummary company = userServiceClient.getCompanySummary(job.companyId());
                companyName = company != null ? company.name() : null;
            } catch (Exception ex) {
                log.debug("user-service unavailable while resolving company name for status email on application {}", application.getId());
            }

            if (applicant != null) {
                notificationServiceClient.sendApplicationStatusEmail(new NotificationServiceClient.ApplicationStatusEmailRequest(
                        applicant.email(), applicant.firstName(), job.title(), companyName, application.getStatus().name()));
            }
        } catch (Exception ex) {
            log.error("Failed to send application status email for application {}", application.getId(), ex);
        }
    }

    @Override
    public boolean hasApplied(int userId, int jobId) {
        int jobSeekerProfileId = getJobSeekerProfileId(userId);
        return applicationRepository.findByJobIdAndJobSeekerProfileId(jobId, jobSeekerProfileId)
                .map(a -> a.getStatus() != ApplicationStatus.Withdrawn)
                .orElse(false);
    }

    /**
     * THE RESUME-ACCESS BUG FIX. Authorization happens entirely in this
     * service, before user-service is ever contacted: resolve the caller's
     * own company, load the application, resolve the job it belongs to, and
     * confirm that job's company matches the caller's. Only then is
     * user-service's internal resume endpoint called. If any of those steps
     * fail, no file content is ever fetched.
     */
    @Override
    public ResumeFileContent downloadResumeForEmployer(int userId, int applicationId) {
        Integer companyId = getEmployerCompanyId(userId, "Only employers can view applicant resumes.");
        JobApplication application = findApplication(applicationId);
        JobServiceClient.JobLookup job = getJob(application.getJobId());

        if (!job.companyId().equals(companyId)) {
            throw new BadRequestException("You can only view resumes for applicants to your own jobs.");
        }

        if (application.getResumeId() == null) {
            throw new BadRequestException("This applicant did not attach a resume.");
        }

        UserServiceClient.ResumeDownload download;
        try {
            download = userServiceClient.downloadResume(application.getResumeId());
        } catch (FeignException.NotFound ex) {
            throw new ResourceNotFoundException("Resume not found.");
        } catch (Exception ex) {
            log.error("user-service unavailable while downloading resume {} for application {}", application.getResumeId(), applicationId, ex);
            throw new BadRequestException("Could not retrieve the resume right now. Please try again.");
        }

        byte[] content = Base64.getDecoder().decode(download.base64Content());
        return new ResumeFileContent(download.fileName(), download.contentType(), content);
    }

    private JobApplication findApplication(int applicationId) {
        return applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found."));
    }

    private JobServiceClient.JobLookup getJob(int jobId) {
        try {
            return jobServiceClient.getJob(jobId);
        } catch (FeignException.NotFound ex) {
            throw new ResourceNotFoundException("Job not found.");
        } catch (Exception ex) {
            log.error("job-service unavailable while resolving job {}", jobId, ex);
            throw new BadRequestException("Could not verify the job right now. Please try again.");
        }
    }

    /** Mirrors ApplicationService.cs's inline "get the user's JobSeekerProfile or throw" logic, used by Apply/Withdraw/GetMyApplications/HasApplied. */
    private int getJobSeekerProfileId(int userId) {
        UserServiceClient.JobSeekerProfileLookup lookup;
        try {
            lookup = userServiceClient.getJobSeekerProfileForUser(userId);
        } catch (FeignException.NotFound ex) {
            throw new BadRequestException("Only job seekers can apply for jobs.");
        } catch (Exception ex) {
            log.error("user-service unavailable while resolving job seeker profile for user {}", userId, ex);
            throw new BadRequestException("Could not verify your account right now. Please try again.");
        }
        if (lookup == null || lookup.jobSeekerProfileId() == null) {
            throw new BadRequestException("Only job seekers can apply for jobs.");
        }
        return lookup.jobSeekerProfileId();
    }

    /**
     * Mirrors ApplicationService.cs's inline employer/company resolution used
     * by GetJobApplicantsAsync and UpdateStatusAsync - both throw
     * "Company not found." for a missing company (note: this is a different
     * literal message than JobService.cs's "Register your company first." for
     * the equivalent case - preserved as its own inconsistency rather than
     * harmonized, since the instruction was to preserve existing behavior).
     */
    private Integer getEmployerCompanyId(int userId, String noEmployerProfileMessage) {
        UserServiceClient.ProfileSummary summary;
        try {
            summary = userServiceClient.getProfileSummary(userId);
        } catch (Exception ex) {
            log.error("user-service unavailable while resolving employer profile for user {}", userId, ex);
            throw new BadRequestException("Could not verify your employer account right now. Please try again.");
        }

        if (summary == null || summary.employerProfileId() == null) {
            throw new BadRequestException(noEmployerProfileMessage);
        }

        try {
            return userServiceClient.getCompanyForEmployerUser(userId).companyId();
        } catch (FeignException.NotFound ex) {
            throw new BadRequestException("Company not found.");
        } catch (Exception ex) {
            log.error("user-service unavailable while resolving company for employer user {}", userId, ex);
            throw new BadRequestException("Could not verify your company right now. Please try again.");
        }
    }

    /**
     * ADMIN MODULE - platform-wide recruitment monitoring.
     *
     * Reuses the shared LookupCache so a page of 200 applications spanning 10
     * jobs makes ~10 job lookups rather than 200; without it this endpoint
     * would be the worst N+1 in the platform.
     *
     * An unrecognised status string is rejected rather than silently ignored -
     * a typo in a filter that quietly returns everything is how an admin ends
     * up trusting the wrong numbers.
     */
    @Override
    public List<ApplicationDto> getAllApplicationsForAdmin(String status, Integer jobId) {
        ApplicationStatus statusFilter = parseStatusFilter(status);

        List<JobApplication> applications;
        if (jobId != null && statusFilter != null) {
            applications = applicationRepository.findByJobIdAndStatusOrderByAppliedAtDesc(jobId, statusFilter);
        } else if (jobId != null) {
            applications = applicationRepository.findByJobIdOrderByAppliedAtDesc(jobId);
        } else if (statusFilter != null) {
            applications = applicationRepository.findByStatusOrderByAppliedAtDesc(statusFilter);
        } else {
            applications = applicationRepository.findAllByOrderByAppliedAtDesc();
        }

        LookupCache cache = new LookupCache();
        return applications.stream().map(a -> toDto(a, cache)).toList();
    }

    private ApplicationStatus parseStatusFilter(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        for (ApplicationStatus candidate : ApplicationStatus.values()) {
            if (candidate.name().equalsIgnoreCase(status.trim())) {
                return candidate;
            }
        }
        throw new BadRequestException("Unknown application status: " + status);
    }

    private ApplicationDto toDto(JobApplication application) {
        return toDto(application, new LookupCache());
    }

    private ApplicationDto toDto(JobApplication application, LookupCache cache) {
        ApplicationDto dto = new ApplicationDto();
        dto.setId(application.getId());
        dto.setJobId(application.getJobId());
        dto.setCoverLetter(application.getCoverLetter());
        dto.setStatus(application.getStatus());
        dto.setAppliedAt(application.getAppliedAt());
        dto.setResumeId(application.getResumeId());
        dto.setResumeFileName(application.getResumeFileName());

        JobServiceClient.JobLookup job = cache.job(application.getJobId(), id -> {
            try {
                return jobServiceClient.getJob(id);
            } catch (Exception ex) {
                log.debug("job-service unavailable while hydrating job title for application {}", application.getId());
                return null;
            }
        });

        if (job != null) {
            dto.setJobTitle(job.title());
            String companyName = cache.company(job.companyId(), id -> {
                try {
                    UserServiceClient.CompanySummary company = userServiceClient.getCompanySummary(id);
                    return company != null ? company.name() : null;
                } catch (Exception ex) {
                    log.debug("user-service unavailable while hydrating company name for application {}", application.getId());
                    return null;
                }
            });
            dto.setCompanyName(companyName);
        }

        AuthServiceClient.UserBasicInfo applicant = cache.user(application.getUserId(), id -> {
            try {
                return authServiceClient.getUserBasicInfo(id);
            } catch (Exception ex) {
                log.debug("auth-service unavailable while hydrating applicant identity for application {}", application.getId());
                return null;
            }
        });

        if (applicant != null) {
            dto.setApplicantName(applicant.firstName() + " " + applicant.lastName());
            dto.setApplicantEmail(applicant.email());
        }

        return dto;
    }

    /**
     * Request-scoped memoisation for the three cross-service lookups toDto()
     * performs. Deliberately a plain local object rather than a Spring cache:
     * it lives for exactly one list call, so there is nothing to invalidate and
     * no chance of serving stale data across requests.
     *
     * computeIfAbsent isn't used because these suppliers legitimately return
     * null on a downstream failure, and computeIfAbsent would retry every time.
     */
    private static final class LookupCache {

        private final Map<Integer, JobServiceClient.JobLookup> jobs = new HashMap<>();
        private final Map<Integer, String> companies = new HashMap<>();
        private final Map<Integer, AuthServiceClient.UserBasicInfo> users = new HashMap<>();

        JobServiceClient.JobLookup job(Integer id, java.util.function.Function<Integer, JobServiceClient.JobLookup> loader) {
            if (id == null) {
                return null;
            }
            if (!jobs.containsKey(id)) {
                jobs.put(id, loader.apply(id));
            }
            return jobs.get(id);
        }

        String company(Integer id, java.util.function.Function<Integer, String> loader) {
            if (id == null) {
                return null;
            }
            if (!companies.containsKey(id)) {
                companies.put(id, loader.apply(id));
            }
            return companies.get(id);
        }

        AuthServiceClient.UserBasicInfo user(Integer id, java.util.function.Function<Integer, AuthServiceClient.UserBasicInfo> loader) {
            if (id == null) {
                return null;
            }
            if (!users.containsKey(id)) {
                users.put(id, loader.apply(id));
            }
            return users.get(id);
        }
    }
}
