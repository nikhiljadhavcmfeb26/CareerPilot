package com.careerpilot.aiservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "notification-service", path = "/internal/notifications")
public interface NotificationServiceClient {

    @PostMapping("/ai-feedback")
    void sendAiFeedbackEmail(@RequestBody AiFeedbackEmailRequest request);

    /** Matches notification-service's AiFeedbackEmailRequest exactly. feedback may be null - notification-service's own template falls back to a professional default when it is, per your spec: "If the Gemini API fails, send a professional default feedback email." */
    record AiFeedbackEmailRequest(String email, String firstName, String jobTitle, String feedback) {
    }
}
