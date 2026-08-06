package com.careerpilot.userservice.dto.internal;

/** Mirrors auth-service's UserServiceClient.ProfileSummary exactly. */
public record ProfileSummaryResponse(Integer employerProfileId, Integer jobSeekerProfileId, Boolean employerApproved) {
}
