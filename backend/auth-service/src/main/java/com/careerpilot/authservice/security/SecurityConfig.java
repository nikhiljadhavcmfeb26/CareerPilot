package com.careerpilot.authservice.security;

import com.careerpilot.common.security.JwtAuthenticationFilter;
import com.careerpilot.common.security.JwtService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // Deliberate upgrade from the .NET app's SHA-256 + static salt hash
        // (CareerPilot.Infrastructure.Services.PasswordHasher) to BCrypt,
        // Spring Security's standard adaptive hash. This is invisible at the
        // API boundary - nothing about the register/login contract changes -
        // and every service is starting from a fresh database anyway, so
        // there's no existing hash population to stay compatible with.
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtService jwtService) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Feign-only, not reachable through the gateway (no /internal/**
                // route exists in api-gateway.yml) - trusted by network topology.
                .requestMatchers("/internal/**").permitAll()
                .requestMatchers("/actuator/**").permitAll()

                // Swagger UI + OpenAPI JSON (Phase 7). These are only reachable
                // on the service's own port - there is no /swagger-ui route in
                // api-gateway.yml - so they are not exposed to the public edge.
                .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()

                .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login",
                        "/api/auth/refresh-token", "/api/auth/forgot-password",
                        "/api/auth/reset-password").permitAll()

                // Logout is permitAll so that a caller whose access token has
                // already expired can still revoke their refresh token. The
                // JwtAuthenticationFilter still populates the SecurityContext
                // when a valid token IS present, so AuthController.logout()
                // takes the authenticated path whenever it can; otherwise it
                // falls back to revoking by the refresh token in the body.
                .requestMatchers(HttpMethod.POST, "/api/auth/logout").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/subscriptions/webhook").permitAll()

                .requestMatchers("/api/users", "/api/users/*/deactivate", "/api/users/*/activate").hasRole("Admin")

                // ADMIN MODULE. Locked down by prefix rather than per method,
                // so a new endpoint added to AdminController can never be
                // accidentally left reachable by a non-admin.
                .requestMatchers("/api/admin/**").hasRole("Admin")

                // Dashboard aggregation (Phase 3). Each dashboard is scoped to
                // one role; the service itself additionally resolves the
                // caller's own company/profile, so an employer can never see
                // another employer's numbers.
                .requestMatchers(HttpMethod.GET, "/api/dashboard/admin").hasRole("Admin")
                .requestMatchers(HttpMethod.GET, "/api/dashboard/employer").hasRole("Employer")
                .requestMatchers(HttpMethod.GET, "/api/dashboard/jobseeker").hasRole("JobSeeker")

                .anyRequest().authenticated())
            .addFilterBefore(new JwtAuthenticationFilter(jwtService), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
