package com.careerpilot.aiservice.controller;

import com.careerpilot.aiservice.dto.internal.RejectionFeedbackRequest;
import com.careerpilot.aiservice.service.AiService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Service-to-service only, same story as every other service's
 * InternalController - no /internal/** route exists in api-gateway.yml.
 */
@RestController
@RequestMapping("/internal/ai")
public class InternalController {

    private final AiService aiService;

    public InternalController(AiService aiService) {
        this.aiService = aiService;
    }

    /** AI FEATURE 4 - AI Rejection Feedback. Called by application-service's updateStatus() when an employer rejects a candidate. */
    @PostMapping("/rejection-feedback")
    public void rejectionFeedback(@RequestBody RejectionFeedbackRequest request) {
        aiService.generateRejectionFeedback(request);
    }
}
