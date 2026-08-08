package com.careerpilot.userservice.dto.internal;

/**
 * A resume's identity WITHOUT its file content - just enough for
 * application-service to record which resume an applicant attached.
 *
 * Deliberately separate from ResumeDownloadResponse: that one carries the
 * whole PDF base64-encoded, which would be wasteful (and slow) to transfer
 * just to validate an id at apply time.
 */
public record ResumeReferenceResponse(Integer resumeId, String fileName) {
}
