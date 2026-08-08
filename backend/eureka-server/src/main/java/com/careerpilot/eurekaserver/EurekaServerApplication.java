package com.careerpilot.eurekaserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Netflix Eureka discovery server. Every other CareerPilot service registers
 * here on startup and looks up its peers here (the API Gateway routes to
 * "lb://auth-service", "lb://user-service", etc. rather than hardcoded
 * host:port pairs).
 *
 * Its own server config (port, self-preservation, eviction interval) lives in
 * eureka-server.yml in the config repo, not in this module - only the
 * "who am I / where's the config server" bootstrap lives locally.
 */
@EnableEurekaServer
@SpringBootApplication
public class EurekaServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
    }
}
