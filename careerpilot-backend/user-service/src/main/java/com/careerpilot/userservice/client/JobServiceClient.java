package com.careerpilot.userservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * job-service is next in the build order, not built yet as of this file -
 * same story as auth-service's Feign clients were when they were written
 * first: this resolves fine at compile time, and simply has nothing to call
 * until job-service registers with Eureka. CompanyDto.jobCount falls back to
 * 0 until then (see CompanyServiceImpl).
 */
@FeignClient(name = "job-service", path = "/internal/jobs")
public interface JobServiceClient {

    @GetMapping("/count-by-company/{companyId}")
    int countByCompany(@PathVariable("companyId") Integer companyId);
}
