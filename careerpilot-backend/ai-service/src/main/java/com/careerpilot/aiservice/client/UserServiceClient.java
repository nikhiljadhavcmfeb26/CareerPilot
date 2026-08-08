package com.careerpilot.aiservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service", path = "/internal")
public interface UserServiceClient {

    /** Mirrors user-service's InternalController response for GET /internal/job-seeker-profiles/by-user/{userId}. */
    @GetMapping("/job-seeker-profiles/by-user/{userId}")
    JobSeekerProfileLookup getJobSeekerProfileForUser(@PathVariable("userId") Integer userId);

    /** Mirrors user-service's InternalController response for GET /internal/companies/by-employer-user/{userId}. */
    @GetMapping("/companies/by-employer-user/{userId}")
    CompanyLookup getCompanyForEmployerUser(@PathVariable("userId") Integer userId);

    /** Mirrors user-service's InternalController response for GET /internal/companies/{companyId}. */
    @GetMapping("/companies/{companyId}")
    CompanySummary getCompanySummary(@PathVariable("companyId") Integer companyId);

    /** For resume-feedback, cover-letter, and job-recommendations, all of which operate on the calling candidate's default resume. */
    @GetMapping("/resumes/default-by-profile/{jobSeekerProfileId}")
    ResumeDownload downloadDefaultResume(@PathVariable("jobSeekerProfileId") Integer jobSeekerProfileId);

    /** For candidate-screening, which needs a specific applicant's resume by id. */
    @GetMapping("/resumes/{id}/download")
    ResumeDownload downloadResume(@PathVariable("id") Integer id);

    record JobSeekerProfileLookup(Integer jobSeekerProfileId) {
    }

    record CompanyLookup(Integer companyId, boolean isApproved) {
    }

    record CompanySummary(Integer id, String name, String location, String industry, String logoUrl) {
    }

    record ResumeDownload(String fileName, String contentType, String base64Content) {
    }
}
