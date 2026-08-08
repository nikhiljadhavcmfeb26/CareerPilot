package com.careerpilot.userservice.dto.internal;

/** For job-service to resolve which company the calling employer owns, and whether it's approved (needed before publishing a job). */
public record CompanyLookupResponse(Integer companyId, boolean isApproved) {
}
