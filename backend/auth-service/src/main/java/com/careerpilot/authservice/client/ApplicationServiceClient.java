package com.careerpilot.authservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * New in Phase 3, used only by the dashboard aggregation. Mirrors
 * application-service's ApplicationStatsResponse; each dashboard reads only the
 * fields it needs.
 */
@FeignClient(name = "application-service", path = "/internal/applications")
public interface ApplicationServiceClient {

    /** Admin dashboard: platform-wide application total. */
    @GetMapping("/stats")
    ApplicationStats getStats();

    /** Employer dashboard: applications across the jobs this employer owns. */
    @GetMapping("/stats/by-jobs")
    ApplicationStats getStatsByJobs(@RequestParam("jobIds") List<Integer> jobIds);

    /** Job seeker dashboard: this candidate's own applications. */
    @GetMapping("/stats/by-profile/{jobSeekerProfileId}")
    ApplicationStats getStatsByProfile(@PathVariable("jobSeekerProfileId") Integer jobSeekerProfileId);

    record ApplicationStats(int total, int pending, int shortlisted) {
    }
}
