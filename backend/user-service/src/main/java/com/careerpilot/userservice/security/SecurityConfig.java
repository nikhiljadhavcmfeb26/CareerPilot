package com.careerpilot.userservice.security;

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
                // Feign-only, not gateway-routed - see api-gateway.yml.
                .requestMatchers("/internal/**").permitAll()
                .requestMatchers("/actuator/**").permitAll()

                // Swagger UI + OpenAPI JSON (Phase 7). These are only reachable
                // on the service's own port - there is no /swagger-ui route in
                // api-gateway.yml - so they are not exposed to the public edge.
                .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()

                // ProfilesController: [Authorize(Roles = Roles.JobSeeker)] class-level
                .requestMatchers("/api/profiles/**").hasRole("JobSeeker")

                // ResumesController: [Authorize(Roles = Roles.JobSeeker)] class-level
                // (the download endpoint is a new addition, kept under the same
                // class-level restriction as every other resume route)
                .requestMatchers("/api/resumes/**").hasRole("JobSeeker")

                // CompaniesController: [Authorize] class-level with per-method role checks
                .requestMatchers(HttpMethod.POST, "/api/companies").hasRole("Employer")
                .requestMatchers(HttpMethod.PUT, "/api/companies").hasRole("Employer")
                .requestMatchers(HttpMethod.GET, "/api/companies/my").hasRole("Employer")
                .requestMatchers(HttpMethod.GET, "/api/companies/pending").hasRole("Admin")
                .requestMatchers(HttpMethod.GET, "/api/companies/admin/all").hasRole("Admin")
                .requestMatchers(HttpMethod.PUT, "/api/companies/*/approve").hasRole("Admin")
                .requestMatchers(HttpMethod.PUT, "/api/companies/*/revoke-approval").hasRole("Admin")

                .anyRequest().authenticated())
            .addFilterBefore(new JwtAuthenticationFilter(jwtService), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
