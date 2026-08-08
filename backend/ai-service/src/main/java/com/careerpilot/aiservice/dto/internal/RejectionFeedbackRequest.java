package com.careerpilot.aiservice.dto.internal;

/** Matches application-service's AiServiceClient.RejectionFeedbackRequest field-for-field. */
public record RejectionFeedbackRequest(Integer employerUserId, Integer jobId, Integer jobSeekerProfileId,
                                        String candidateEmail, String candidateFirstName) {
}
