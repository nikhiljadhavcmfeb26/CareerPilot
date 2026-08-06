package com.careerpilot.applicationservice.dto.internal;

/**
 * Application counts for auth-service's dashboard aggregation. The same shape
 * serves all three dashboards; each one reads only the fields it needs
 * (admin uses total, employer uses total + pending, job seeker uses all three).
 */
public record ApplicationStatsResponse(int total, int pending, int shortlisted) {

    public static ApplicationStatsResponse empty() {
        return new ApplicationStatsResponse(0, 0, 0);
    }
}
