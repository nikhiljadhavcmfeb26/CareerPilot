package com.careerpilot.common.security;

public record AuthenticatedUser(Integer userId, String email, String role) {
}
