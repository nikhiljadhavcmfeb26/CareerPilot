package com.careerpilot.jobservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * application-service is next in the build order, not built yet as of this
 * file - same "resolves at compile time, nothing to call until it's
 * registered with Eureka" story as every other not-yet-built Feign target so
 * far. JobDto.applicationCount falls back to 0 until then.
 */
@FeignClient(name = "application-service", path = "/internal/applications")
public interface ApplicationServiceClient {

    @GetMapping("/count-by-job/{jobId}")
    int countByJob(@PathVariable("jobId") Integer jobId);
}
