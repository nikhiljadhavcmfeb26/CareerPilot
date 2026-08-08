package com.careerpilot.aiservice.security;

import com.careerpilot.common.security.JwtAuthenticationFilter;
import com.careerpilot.common.security.JwtService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtService jwtService) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/internal/**").permitAll()
                .requestMatchers("/actuator/**").permitAll()

                // Swagger UI + OpenAPI JSON (Phase 7). These are only reachable
                // on the service's own port - there is no /swagger-ui route in
                // api-gateway.yml - so they are not exposed to the public edge.
                .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()

                .requestMatchers(HttpMethod.POST, "/api/ai/resume-feedback").hasRole("JobSeeker")
                .requestMatchers(HttpMethod.POST, "/api/ai/cover-letter").hasRole("JobSeeker")
                .requestMatchers(HttpMethod.POST, "/api/ai/job-recommendations").hasRole("JobSeeker")
                .requestMatchers(HttpMethod.POST, "/api/ai/candidate-screening").hasRole("Employer")

                .anyRequest().authenticated())
            .addFilterBefore(new JwtAuthenticationFilter(jwtService), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
