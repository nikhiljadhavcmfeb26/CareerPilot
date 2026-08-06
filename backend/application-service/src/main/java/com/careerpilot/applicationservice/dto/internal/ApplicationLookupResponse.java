package com.careerpilot.applicationservice.dto.internal;

/**
 * For ai-service's candidate screening feature - one row per applicant to a
 * job, with enough identifiers (userId for name/email via auth-service,
 * resumeId for the actual resume via user-service) to screen each one
 * without ai-service needing its own copy of this service's data.
 */
public record ApplicationLookupResponse(Integer applicationId, Integer userId, Integer jobSeekerProfileId, Integer resumeId, String status) {
}
