package com.careerpilot.notificationservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * No @EnableFeignClients here - unlike every other business service, this
 * one never calls out to another service, only ever gets called. It still
 * needs @EnableDiscoveryClient so the others can find it via Eureka.
 */
@EnableDiscoveryClient
@EnableJpaAuditing
@SpringBootApplication(scanBasePackages = "com.careerpilot")
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
