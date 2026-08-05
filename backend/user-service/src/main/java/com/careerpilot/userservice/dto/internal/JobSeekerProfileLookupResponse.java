package com.careerpilot.userservice.dto.internal;

/** For application-service to resolve a JobSeekerProfile's own id from a userId when applying for a job. */
public record JobSeekerProfileLookupResponse(Integer jobSeekerProfileId) {
}
