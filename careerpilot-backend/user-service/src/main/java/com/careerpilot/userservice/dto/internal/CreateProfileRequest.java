package com.careerpilot.userservice.dto.internal;

/** Mirrors auth-service's UserServiceClient.CreateProfileRequest exactly - both sides must agree on this shape independently, standard Feign practice. */
public record CreateProfileRequest(Integer userId, String role) {
}
