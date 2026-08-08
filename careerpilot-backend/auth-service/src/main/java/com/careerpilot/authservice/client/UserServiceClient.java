package com.careerpilot.authservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * user-service isn't built yet as of this file being written (it's next in
 * the build order right after auth-service) - that's fine, Feign clients are
 * just interfaces resolved against Eureka at call time, not compile time.
 * Until user-service is registered, calls through this client will fail with
 * a "no instances available" error, which is expected at this stage.
 */
/**
 * Path widened from "/internal/profiles" to "/internal" so this one client can
 * also reach user-service's company and resume lookups, which the dashboard
 * aggregation needs. Feign allows only one client per service name, and this
 * now matches how application-service and ai-service already declare their
 * user-service clients. The two pre-existing methods keep their exact same
 * effective URLs - the prefix simply moved into the @Mapping annotations.
 */
@FeignClient(name = "user-service", path = "/internal")
public interface UserServiceClient {

    @PostMapping("/profiles")
    void createProfile(@RequestBody CreateProfileRequest request);

    /**
     * Best-effort enrichment for UserDto.employerProfileId/jobSeekerProfileId/
     * isEmployerApproved - see AuthServiceImpl.enrichWithProfile(). Returns
     * null-filled fields rather than 404ing if the user has no profile yet
     * (e.g. an Admin account, which has neither).
     */
    @GetMapping("/profiles/summary/{userId}")
    ProfileSummary getProfileSummary(@PathVariable("userId") Integer userId);

    // --- dashboard aggregation ---

    /** Admin dashboard: companies still awaiting approval. */
    @GetMapping("/companies/pending-count")
    int countPendingCompanies();

    /** Employer dashboard: which company this employer owns, and whether it's approved. 404s if they haven't registered one yet. */
    @GetMapping("/companies/by-employer-user/{userId}")
    CompanyLookup getCompanyForEmployerUser(@PathVariable("userId") Integer userId);

    /** Job seeker dashboard: resolves the profile id that bookmarks and applications are keyed by. 404s for a non-job-seeker. */
    @GetMapping("/job-seeker-profiles/by-user/{userId}")
    JobSeekerProfileLookup getJobSeekerProfileForUser(@PathVariable("userId") Integer userId);

    /** Job seeker dashboard: drives the "upload your resume" prompt. */
    @GetMapping("/resumes/count-by-profile/{jobSeekerProfileId}")
    int countResumesByProfile(@PathVariable("jobSeekerProfileId") Integer jobSeekerProfileId);

    record CreateProfileRequest(Integer userId, String role) {
    }

    record ProfileSummary(Integer employerProfileId, Integer jobSeekerProfileId, Boolean employerApproved) {
    }

    record CompanyLookup(Integer companyId, boolean isApproved) {
    }

    record JobSeekerProfileLookup(Integer jobSeekerProfileId) {
    }
}
