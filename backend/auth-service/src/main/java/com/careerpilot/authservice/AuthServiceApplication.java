package com.careerpilot.authservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * scanBasePackages covers both this service's own package
 * (com.careerpilot.authservice) and the shared com.careerpilot.common
 * module (ApiResponse/exceptions/JWT filter/BaseEntity), which otherwise
 * wouldn't be picked up by Spring Boot's default component/entity scan.
 */
@EnableDiscoveryClient
@EnableFeignClients
@EnableJpaAuditing
@SpringBootApplication(scanBasePackages = "com.careerpilot")
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
