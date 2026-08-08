package com.careerpilot.authservice.dto.dashboard;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Field names match client/src/pages/jobseeker/Dashboard.jsx exactly.
 * hasResume is pinned for the same reason as EmployerDashboardDto.isApproved -
 * see the note there.
 */
public record JobSeekerDashboardDto(
        @JsonProperty("hasResume") boolean hasResume,
        int appliedJobs,
        int savedJobs,
        int pendingApplications,
        int shortlistedApplications) {
}
