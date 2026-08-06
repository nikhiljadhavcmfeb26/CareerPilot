package com.careerpilot.applicationservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * The .NET app never sends this email today (ApplicationService.cs's
 * UpdateStatusAsync has no email side-effect at all) - this is a new
 * capability, matching the "Application Status Email" responsibility your
 * spec assigns to Notification Service. Wrapped in try/catch at the call
 * site in ApplicationServiceImpl - a status update should still succeed even
 * if the email can't be sent right now.
 */
@FeignClient(name = "notification-service", path = "/internal/notifications")
public interface NotificationServiceClient {

    @PostMapping("/application-status")
    void sendApplicationStatusEmail(@RequestBody ApplicationStatusEmailRequest request);

    record ApplicationStatusEmailRequest(String email, String firstName, String jobTitle, String companyName, String status) {
    }
}
