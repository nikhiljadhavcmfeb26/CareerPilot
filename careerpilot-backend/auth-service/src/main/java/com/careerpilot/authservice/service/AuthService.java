package com.careerpilot.authservice.service;

import com.careerpilot.authservice.dto.AuthResponse;
import com.careerpilot.authservice.dto.ForgotPasswordRequest;
import com.careerpilot.authservice.dto.LoginRequest;
import com.careerpilot.authservice.dto.RegisterRequest;
import com.careerpilot.authservice.dto.ResetPasswordRequest;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refreshToken(String refreshToken);

    void logout(int userId);

    /**
     * Revoke a session using only the refresh token, for the case where the
     * caller's access token has already expired (see AuthController.logout).
     */
    void logoutByRefreshToken(String refreshToken);

    void forgotPassword(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);
}
