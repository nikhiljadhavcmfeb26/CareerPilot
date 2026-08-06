package com.careerpilot.applicationservice.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Names and ordinal order match CareerPilot.Domain.Enums.ApplicationStatus
 * exactly. Same numeric-or-string read tolerance as job-service's enums
 * (JobType/ExperienceLevel), applied here defensively for
 * UpdateApplicationStatusDto.Status - consistent with the pattern already
 * established for the other enum request fields in this codebase.
 */
public enum ApplicationStatus {
    Pending,
    Reviewed,
    Shortlisted,
    Rejected,
    Accepted,
    Withdrawn;

    @JsonCreator
    public static ApplicationStatus fromJson(JsonNode node) {
        if (node.isNumber()) {
            return values()[node.asInt()];
        }
        return valueOf(node.asText());
    }
}
