package com.careerpilot.applicationservice.security;

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

                // ADMIN MODULE - must come before the employer patterns below,
                // because Spring Security matches top-down and first match wins.
                .requestMatchers(HttpMethod.GET, "/api/applications/admin/all").hasRole("Admin")

                // Employer-side (most specific patterns first)
                .requestMatchers(HttpMethod.GET, "/api/applications/job/*").hasRole("Employer")
                .requestMatchers(HttpMethod.GET, "/api/applications/*/resume").hasRole("Employer")
                .requestMatchers(HttpMethod.PUT, "/api/applications/*/status").hasRole("Employer")

                // Job seeker side
                .requestMatchers(HttpMethod.GET, "/api/applications/my").hasRole("JobSeeker")
                .requestMatchers(HttpMethod.GET, "/api/applications/check/*").hasRole("JobSeeker")
                .requestMatchers(HttpMethod.POST, "/api/applications").hasRole("JobSeeker")
                .requestMatchers(HttpMethod.PUT, "/api/applications/*/withdraw").hasRole("JobSeeker")

                .anyRequest().authenticated())
            .addFilterBefore(new JwtAuthenticationFilter(jwtService), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
