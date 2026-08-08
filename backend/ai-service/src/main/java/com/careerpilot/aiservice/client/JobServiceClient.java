package com.careerpilot.aiservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "job-service", path = "/internal/jobs")
public interface JobServiceClient {

    /** Mirrors job-service's InternalController response for GET /internal/jobs/{jobId}, now including description/requirements. */
    @GetMapping("/{jobId}")
    JobLookup getJob(@PathVariable("jobId") Integer jobId);

    /** Mirrors job-service's InternalController response for GET /internal/jobs/published - the 50 most recent, for job recommendations. */
    @GetMapping("/published")
    List<JobLookup> getPublishedJobs();

    record JobLookup(Integer id, String title, Integer companyId, String status, String description, String requirements) {
    }
}
