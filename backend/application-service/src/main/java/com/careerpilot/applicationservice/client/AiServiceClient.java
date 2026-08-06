package com.careerpilot.applicationservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * ai-service is last in the build order, not built yet as of this file -
 * same "resolves at compile time, nothing to call until it's registered
 * with Eureka" story as every other not-yet-built Feign target. Per your
 * spec: "When an employer rejects a candidate, generate professional
 * feedback... automatically email the generated feedback to the candidate."
 * That's triggered here, in updateStatus(), not as a separate action the
 * employer has to remember to take. ai-service itself decides whether the
 * employer's subscription is premium enough to actually run this (see
 * ai-service's SubscriptionClient) - this call site doesn't know or care.
 */
@FeignClient(name = "ai-service", path = "/internal/ai")
public interface AiServiceClient {

    @PostMapping("/rejection-feedback")
    void triggerRejectionFeedback(@RequestBody RejectionFeedbackRequest request);

    record RejectionFeedbackRequest(Integer employerUserId, Integer jobId, Integer jobSeekerProfileId,
                                     String candidateEmail, String candidateFirstName) {
    }
}
