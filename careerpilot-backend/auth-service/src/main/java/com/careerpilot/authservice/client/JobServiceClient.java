package com.careerpilot.authservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * New in Phase 3, used only by the dashboard aggregation. auth-service does not
 * own jobs or bookmarks - it asks job-service for the counts it needs to render
 * a dashboard, exactly as agreed (aggregate in Auth Service over OpenFeign).
 */
@FeignClient(name = "job-service", path = "/internal/jobs")
public interface JobServiceClient {

    /** Admin dashboard: platform-wide job counts. */
    @GetMapping("/stats")
    JobStats getStats();

    /** Employer dashboard: this company's job counts, plus the ids so applications can be counted in one follow-up call. */
    @GetMapping("/stats/by-company/{companyId}")
    EmployerJobStats getStatsByCompany(@PathVariable("companyId") Integer companyId);

    /** Job seeker dashboard: saved-jobs count (bookmarks live in job-service). */
    @GetMapping("/bookmarks/count-by-profile/{jobSeekerProfileId}")
    int countBookmarksByProfile(@PathVariable("jobSeekerProfileId") Integer jobSeekerProfileId);

    record JobStats(int totalJobs, int publishedJobs) {
    }

    record EmployerJobStats(int totalJobs, int publishedJobs, List<Integer> jobIds) {
    }
}
