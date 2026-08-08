package com.careerpilot.aiservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "application-service", path = "/internal/applications")
public interface ApplicationServiceClient {

    /** Mirrors application-service's InternalController response for GET /internal/applications/by-job/{jobId}. */
    @GetMapping("/by-job/{jobId}")
    List<ApplicationLookup> getByJob(@PathVariable("jobId") Integer jobId);

    record ApplicationLookup(Integer applicationId, Integer userId, Integer jobSeekerProfileId, Integer resumeId, String status) {
    }
}
