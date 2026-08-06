package com.careerpilot.applicationservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "job-service", path = "/internal/jobs")
public interface JobServiceClient {

    /** Mirrors job-service's InternalController response for GET /internal/jobs/{jobId}. */
    @GetMapping("/{jobId}")
    JobLookup getJob(@PathVariable("jobId") Integer jobId);

    record JobLookup(Integer id, String title, Integer companyId, String status) {
    }
}
