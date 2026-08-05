package com.careerpilot.authservice.controller;

import com.careerpilot.authservice.dto.UserBasicInfo;
import com.careerpilot.authservice.dto.internal.AiAccessResponse;
import com.careerpilot.authservice.service.AdminService;
import com.careerpilot.authservice.entity.User;
import com.careerpilot.authservice.repository.UserRepository;
import com.careerpilot.authservice.service.SubscriptionService;
import com.careerpilot.common.exception.ResourceNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Service-to-service only. There's no route for /internal/** in
 * api-gateway.yml, so these are unreachable from outside the platform's own
 * network - callers are other Spring services via OpenFeign + Eureka, not
 * the React client. Kept unauthenticated (permitAll in SecurityConfig)
 * since the caller doesn't hold a user's JWT to forward.
 */
@RestController
@RequestMapping("/internal")
public class InternalController {

    private final UserRepository userRepository;
    private final SubscriptionService subscriptionService;
    private final AdminService adminService;

    public InternalController(UserRepository userRepository, SubscriptionService subscriptionService,
                               AdminService adminService) {
        this.userRepository = userRepository;
        this.subscriptionService = subscriptionService;
        this.adminService = adminService;
    }

    /** Used by application-service to hydrate ApplicantName/ApplicantEmail on the applicant list. */
    @GetMapping("/users/{id}")
    public UserBasicInfo getUserBasicInfo(@PathVariable("id") Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
        return new UserBasicInfo(user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(),
                user.getRole().getName(), user.isActive());
    }

    /**
     * Used by ai-service to gate premium features before calling Gemini.
     * Retained for the rejection-feedback path and any external caller that
     * only needs the raw subscription answer - the richer /ai-access check
     * below is what the user-facing AI endpoints now use.
     */
    @GetMapping("/subscriptions/{userId}/active")
    public boolean isPremiumActive(@PathVariable("userId") Integer userId) {
        return subscriptionService.isPremiumActive(userId);
    }

    /**
     * ADMIN MODULE. The single authoritative "may this user use this AI
     * feature right now?" check. Folds together the Premium subscription, the
     * platform-wide AI switches, the per-account AI kill switch, and the
     * account's active/blocked state - so ai-service makes one call instead of
     * four, and there is exactly one place where the policy lives.
     *
     * @param feature one of RESUME_FEEDBACK, COVER_LETTER, JOB_RECOMMENDATIONS,
     *                CANDIDATE_SCREENING, REJECTION_FEEDBACK
     */
    @GetMapping("/ai-access/{userId}")
    public AiAccessResponse checkAiAccess(@PathVariable("userId") Integer userId,
                                           @RequestParam(value = "feature", required = false) String feature) {
        return adminService.checkAiAccess(userId, feature);
    }
}
