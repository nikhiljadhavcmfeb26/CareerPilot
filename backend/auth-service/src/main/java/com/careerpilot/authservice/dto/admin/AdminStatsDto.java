package com.careerpilot.authservice.dto.admin;

/** Backs the admin dashboard tiles. */
public record AdminStatsDto(
        int totalUsers,
        int totalJobSeekers,
        int totalEmployers,
        int totalAdmins,
        int activeUsers,
        int blockedUsers,
        int totalJobs,
        int publishedJobs,
        int totalApplications,
        int pendingApplications,
        int shortlistedApplications,
        int premiumSubscribers,
        int pendingEmployerApprovals) {
}
