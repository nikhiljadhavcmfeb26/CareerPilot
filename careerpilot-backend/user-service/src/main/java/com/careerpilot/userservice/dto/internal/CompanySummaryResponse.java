package com.careerpilot.userservice.dto.internal;

/** For job-service to embed CompanyName (and friends) into JobDto without owning the Companies table itself. */
public record CompanySummaryResponse(Integer id, String name, String location, String industry, String logoUrl) {
}
