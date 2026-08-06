package com.careerpilot.applicationservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "auth-service", path = "/internal")
public interface AuthServiceClient {

    /** Mirrors auth-service's InternalController response for GET /internal/users/{id}. */
    @GetMapping("/users/{id}")
    UserBasicInfo getUserBasicInfo(@PathVariable("id") Integer id);

    record UserBasicInfo(Integer id, String email, String firstName, String lastName, String role, boolean active) {
    }
}
