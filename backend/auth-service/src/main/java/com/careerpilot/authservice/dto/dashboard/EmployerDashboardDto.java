package com.careerpilot.authservice.dto.dashboard;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Field names match client/src/pages/employer/Dashboard.jsx exactly.
 *
 * isApproved is pinned with @JsonProperty for the same reason ResumeDto,
 * UserDto and UserListDto pin theirs: Jackson's bean naming turns a boolean
 * accessor isApproved() into the property "approved". Every other
 * browser-facing boolean in this codebase is pinned; these two were the only
 * ones relying on Jackson's record-component naming instead. If that naming
 * ever differs, the employer's "pending approval" banner inverts silently -
 * shown to approved employers, hidden from unapproved ones.
 */
public record EmployerDashboardDto(
        @JsonProperty("isApproved") boolean isApproved,
        int totalJobs,
        int publishedJobs,
        int totalApplications,
        int pendingApplications) {
}
