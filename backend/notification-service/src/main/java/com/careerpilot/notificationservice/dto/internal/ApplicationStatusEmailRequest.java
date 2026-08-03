package com.careerpilot.notificationservice.dto.internal;

/** All-new - the .NET app never sends application status emails today; matches the "Application Status Email" responsibility from your spec. */
public record ApplicationStatusEmailRequest(String email, String firstName, String jobTitle, String companyName, String status) {
}
