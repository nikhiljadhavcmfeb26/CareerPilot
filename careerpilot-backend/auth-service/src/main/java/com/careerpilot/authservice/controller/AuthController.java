package com.careerpilot.authservice.controller;

import com.careerpilot.authservice.dto.AuthResponse;
import com.careerpilot.authservice.dto.ForgotPasswordRequest;
import com.careerpilot.authservice.dto.LoginRequest;
import com.careerpilot.authservice.dto.LogoutRequest;
import com.careerpilot.authservice.dto.RefreshTokenRequest;
import com.careerpilot.authservice.dto.RegisterRequest;
import com.careerpilot.authservice.dto.ResetPasswordRequest;
import com.careerpilot.authservice.service.AuthService;
import com.careerpilot.common.dto.ApiResponse;
import com.careerpilot.common.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok(authService.register(request), "Registration successful");
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request), "Login successful");
    }

    @PostMapping("/refresh-token")
    public ApiResponse<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.ok(authService.refreshToken(request.getRefreshToken()), "Token refreshed");
    }

    /**
     * Logout is deliberately tolerant of an expired access token. It used to
     * require a valid bearer token, which meant that once the 60-minute access
     * token lapsed there was no way left to revoke the 7-day refresh token -
     * the exact situation where revoking matters most.
     *
     * Both paths are supported, in priority order:
     *   1. A valid access token in the SecurityContext -> revoke that user's
     *      session (unchanged behaviour for the happy path).
     *   2. No/expired token but a refreshToken in the body -> revoke by token.
     *
     * Always returns success: logout must never reveal whether a token was
     * real, and a client clearing its own storage should never see an error.
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestBody(required = false) LogoutRequest request) {
        AuthenticatedUser current = currentUserOrNull();

        if (current != null) {
            authService.logout(current.userId());
        } else if (request != null) {
            authService.logoutByRefreshToken(request.getRefreshToken());
        }

        return ApiResponse.success("Logged out successfully");
    }

    private AuthenticatedUser currentUserOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) ? user : null;
    }

    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ApiResponse.success("If that email is registered, an OTP has been sent to it.");
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ApiResponse.success("Password reset successfully.");
    }
}
