package com.careerpilot.authservice.dto;

/**
 * Optional body for POST /api/auth/logout. Deliberately NOT @NotBlank: a client
 * that still holds a valid access token doesn't need to send anything, and the
 * body is absent entirely in that case.
 */
public class LogoutRequest {

    private String refreshToken;

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
