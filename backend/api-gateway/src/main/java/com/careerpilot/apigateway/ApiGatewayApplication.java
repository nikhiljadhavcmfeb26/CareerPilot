package com.careerpilot.apigateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Spring Cloud Gateway: the single entry point the React client talks to.
 * Registers with Eureka so its routes can target "lb://service-id" instead
 * of hardcoded addresses, and pulls its full route table + CORS policy from
 * api-gateway.yml on the config server.
 */
@EnableDiscoveryClient
@SpringBootApplication
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
