package com.careerpilot.jobservice.dto.internal;

/** Platform-wide job counts for auth-service's admin dashboard aggregation. */
public record JobStatsResponse(int totalJobs, int publishedJobs) {
}
