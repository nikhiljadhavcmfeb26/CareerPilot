package com.careerpilot.applicationservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "user-service", path = "/internal")
public interface UserServiceClient {

    /** Mirrors user-service's InternalController response for GET /internal/profiles/summary/{userId}. */
    @GetMapping("/profiles/summary/{userId}")
    ProfileSummary getProfileSummary(@PathVariable("userId") Integer userId);

    /** Mirrors user-service's InternalController response for GET /internal/job-seeker-profiles/by-user/{userId}. 404s if the user has no job seeker profile - this is how "Only job seekers can apply for jobs." gets enforced. */
    @GetMapping("/job-seeker-profiles/by-user/{userId}")
    JobSeekerProfileLookup getJobSeekerProfileForUser(@PathVariable("userId") Integer userId);

    /** Mirrors user-service's InternalController response for GET /internal/companies/by-employer-user/{userId}. 404s if the employer has no company yet. */
    @GetMapping("/companies/by-employer-user/{userId}")
    CompanyLookup getCompanyForEmployerUser(@PathVariable("userId") Integer userId);

    /** Mirrors user-service's InternalController response for GET /internal/companies/{companyId}. */
    @GetMapping("/companies/{companyId}")
    CompanySummary getCompanySummary(@PathVariable("companyId") Integer companyId);

    /**
     * THE RESUME-ACCESS BUG FIX: called only after this service has already
     * verified the requesting employer owns the job the application belongs
     * to (see ApplicationServiceImpl.downloadResumeForEmployer). user-service
     * performs no authorization check of its own on this endpoint - the
     * decision was already made here, which is why this is an internal,
     * non-gateway-routed call rather than something the browser can reach
     * directly.
     */
    @GetMapping("/resumes/{id}/download")
    ResumeDownload downloadResume(@PathVariable("id") Integer id);

    /**
     * Validates that a resume the applicant picked really belongs to them, or
     * falls back to their default. Identity only - no file content crosses the
     * wire here, unlike downloadResume above.
     */
    @GetMapping("/resumes/resolve-for-application/{jobSeekerProfileId}")
    ResumeReference resolveResumeForApplication(@PathVariable("jobSeekerProfileId") Integer jobSeekerProfileId,
                                                 @RequestParam(value = "resumeId", required = false) Integer resumeId);

    record JobSeekerProfileLookup(Integer jobSeekerProfileId) {
    }

    record ProfileSummary(Integer employerProfileId, Integer jobSeekerProfileId, Boolean employerApproved) {
    }

    record CompanyLookup(Integer companyId, boolean isApproved) {
    }

    record CompanySummary(Integer id, String name, String location, String industry, String logoUrl) {
    }

    record ResumeDownload(String fileName, String contentType, String base64Content) {
    }

    record ResumeReference(Integer resumeId, String fileName) {
    }
}
