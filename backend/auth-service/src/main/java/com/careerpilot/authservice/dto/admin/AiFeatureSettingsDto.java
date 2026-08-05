package com.careerpilot.authservice.dto.admin;

/** Read/write shape of the platform-wide AI switches. */
public record AiFeatureSettingsDto(
        boolean aiEnabled,
        boolean resumeFeedbackEnabled,
        boolean coverLetterEnabled,
        boolean jobRecommendationsEnabled,
        boolean candidateScreeningEnabled,
        boolean rejectionFeedbackEnabled,
        boolean requirePremium) {
}
