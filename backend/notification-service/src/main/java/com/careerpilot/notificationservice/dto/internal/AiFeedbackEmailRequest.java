package com.careerpilot.notificationservice.dto.internal;

/** All-new - for AI Service (next/last in the build order) to call once a rejection feedback is generated. feedback may be null, in which case a professional default message is sent instead - see NotificationServiceImpl. */
public record AiFeedbackEmailRequest(String email, String firstName, String jobTitle, String feedback) {
}
