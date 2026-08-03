package com.careerpilot.jobservice.security;

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

                // Most specific job routes first - Spring Security matches
                // top-down, first match wins.
                .requestMatchers(HttpMethod.GET, "/api/jobs/my").hasRole("Employer")
                .requestMatchers(HttpMethod.GET, "/api/jobs/admin/all").hasRole("Admin")
                .requestMatchers(HttpMethod.DELETE, "/api/jobs/admin/*").hasRole("Admin")
                .requestMatchers(HttpMethod.POST, "/api/jobs").hasRole("Employer")
                .requestMatchers(HttpMethod.PUT, "/api/jobs/*/publish").hasRole("Employer")
                .requestMatchers(HttpMethod.PUT, "/api/jobs/*/close").hasRole("Employer")
                .requestMatchers(HttpMethod.PUT, "/api/jobs/*").hasRole("Employer")
                .requestMatchers(HttpMethod.DELETE, "/api/jobs/*").hasRole("Employer")

                // [AllowAnonymous] on Search + GetById in the original.
                .requestMatchers(HttpMethod.GET, "/api/jobs", "/api/jobs/*").permitAll()

                // BookmarksController: [Authorize(Roles = Roles.JobSeeker)] class-level
                .requestMatchers("/api/bookmarks/**").hasRole("JobSeeker")

                .anyRequest().authenticated())
            .addFilterBefore(new JwtAuthenticationFilter(jwtService), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
