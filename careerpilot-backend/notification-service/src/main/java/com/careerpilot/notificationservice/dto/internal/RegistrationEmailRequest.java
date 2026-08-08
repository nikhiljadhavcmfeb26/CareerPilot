package com.careerpilot.notificationservice.dto.internal;

/** Matches auth-service's NotificationServiceClient.RegistrationEmailRequest field-for-field. */
public record RegistrationEmailRequest(String email, String firstName, String role) {
}
