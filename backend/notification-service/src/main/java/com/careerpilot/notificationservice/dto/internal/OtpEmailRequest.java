package com.careerpilot.notificationservice.dto.internal;

/** Matches auth-service's NotificationServiceClient.OtpEmailRequest field-for-field. */
public record OtpEmailRequest(String email, String firstName, String otp) {
}
