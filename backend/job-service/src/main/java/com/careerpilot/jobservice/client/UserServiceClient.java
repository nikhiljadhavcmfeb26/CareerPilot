package com.careerpilot.jobservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "user-service", path = "/internal")
public interface UserServiceClient {

    /** Mirrors user-service's InternalController response for GET /internal/profiles/summary/{userId}. */
    @GetMapping("/profiles/summary/{userId}")
    ProfileSummary getProfileSummary(@PathVariable("userId") Integer userId);

    /** Mirrors user-service's InternalController response for GET /internal/job-seeker-profiles/by-user/{userId}. 404s if the user has no job seeker profile. */
    @GetMapping("/job-seeker-profiles/by-user/{userId}")
    JobSeekerProfileLookup getJobSeekerProfileForUser(@PathVariable("userId") Integer userId);

    /** Mirrors user-service's InternalController response for GET /internal/companies/by-employer-user/{userId}. 404s if the employer profile exists but has no company yet. */
    @GetMapping("/companies/by-employer-user/{userId}")
    CompanyLookup getCompanyForEmployerUser(@PathVariable("userId") Integer userId);

    /** Mirrors user-service's InternalController response for GET /internal/companies/{companyId}. */
    @GetMapping("/companies/{companyId}")
    CompanySummary getCompanySummary(@PathVariable("companyId") Integer companyId);

    /** New endpoint on user-service (see below) - resolves which company ids match a keyword by name, for JobRepository.cs's Company.Name.Contains(keyword) search clause. */
    @GetMapping("/companies/search-ids")
    List<Integer> searchCompanyIds(@RequestParam("keyword") String keyword);

    record ProfileSummary(Integer employerProfileId, Integer jobSeekerProfileId, Boolean employerApproved) {
    }

    record JobSeekerProfileLookup(Integer jobSeekerProfileId) {
    }

    record CompanyLookup(Integer companyId, boolean isApproved) {
    }

    record CompanySummary(Integer id, String name, String location, String industry, String logoUrl) {
    }
}
