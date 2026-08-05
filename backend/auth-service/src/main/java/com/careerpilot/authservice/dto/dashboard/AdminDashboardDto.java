package com.careerpilot.authservice.dto.dashboard;

/**
 * Field names match exactly what client/src/pages/admin/Dashboard.jsx already
 * reads - this contract is dictated by the existing UI, not invented here.
 *
 * Records (rather than POJOs) are used for all three dashboard DTOs on purpose:
 * Jackson derives JSON property names from record components verbatim, so
 * boolean fields like isApproved/hasResume serialize under those exact names.
 * A POJO with `boolean isApproved` + `isApproved()` would serialize as
 * "approved" and silently break the UI.
 */
public record AdminDashboardDto(
        int totalUsers,
        int totalEmployers,
        int totalJobSeekers,
        int totalJobs,
        int publishedJobs,
        int totalApplications,
        int pendingEmployerApprovals) {
}
