package com.careerpilot.authservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * notification-service is last in the build order, so this client has
 * nothing to resolve against until then. Every call site using this client
 * wraps the call in a try/catch and logs rather than failing the request -
 * a registration or password reset should still succeed even if the email
 * can't be sent right now, exactly like the .NET EmailService already
 * degrades gracefully when SMTP isn't configured.
 */
@FeignClient(name = "notification-service", path = "/internal/notifications")
public interface NotificationServiceClient {

    @PostMapping("/registration")
    void sendRegistrationEmail(@RequestBody RegistrationEmailRequest request);

    @PostMapping("/otp")
    void sendOtpEmail(@RequestBody OtpEmailRequest request);

    record RegistrationEmailRequest(String email, String firstName, String role) {
    }

    record OtpEmailRequest(String email, String firstName, String otp) {
    }
}
