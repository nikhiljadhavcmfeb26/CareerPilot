package com.careerpilot.jobservice.dto.internal;

import java.util.List;

/**
 * One employer's job counts plus the ids of those jobs, for auth-service's
 * employer dashboard aggregation. The ids are included so auth-service can
 * resolve the employer's application totals with a single follow-up call to
 * application-service instead of one call per job.
 */
public record EmployerJobStatsResponse(int totalJobs, int publishedJobs, List<Integer> jobIds) {
}
