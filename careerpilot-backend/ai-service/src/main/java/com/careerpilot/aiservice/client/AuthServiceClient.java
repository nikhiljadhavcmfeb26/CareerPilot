package com.careerpilot.aiservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "auth-service", path = "/internal")
public interface AuthServiceClient {

    /** Mirrors auth-service's InternalController response for GET /internal/users/{id}. */
    @GetMapping("/users/{id}")
    UserBasicInfo getUserBasicInfo(@PathVariable("id") Integer id);

    /**
     * THE PREMIUM GATE. Every AI endpoint in this service checks this before
     * doing anything else - "Users can access AI features only while their
     * Premium subscription is active" per your spec. Mirrors auth-service's
     * InternalController response for GET /internal/subscriptions/{userId}/active.
     */
    @GetMapping("/subscriptions/{userId}/active")
    boolean isPremiumActive(@PathVariable("userId") Integer userId);

    /**
     * ADMIN MODULE - THE AI GATE. One call now answers everything this service
     * needs to know before spending a Gemini request: Premium subscription,
     * the platform-wide AI switches an administrator controls, the per-account
     * AI kill switch, and whether the account is active/blocked. It replaces
     * the four separate checks that would otherwise be needed and keeps the
     * policy in one place (auth-service's AdminServiceImpl.checkAiAccess).
     *
     * Mirrors auth-service's InternalController GET /internal/ai-access/{userId}.
     */
    @GetMapping("/ai-access/{userId}")
    AiAccessResponse checkAiAccess(@PathVariable("userId") Integer userId,
                                    @RequestParam("feature") String feature);

    record UserBasicInfo(Integer id, String email, String firstName, String lastName, String role, boolean active) {
    }

    /** @param reason human-readable denial reason, safe to show the caller; null when allowed. */
    record AiAccessResponse(boolean allowed, String reason, boolean premium) {
    }
}
