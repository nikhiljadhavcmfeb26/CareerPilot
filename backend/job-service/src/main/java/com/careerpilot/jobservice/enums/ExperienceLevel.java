package com.careerpilot.jobservice.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.JsonNode;

/** Names and ordinal order match CareerPilot.Domain.Enums.ExperienceLevel exactly. Same numeric-tolerant read as JobType. */
public enum ExperienceLevel {
    Entry,
    Mid,
    Senior,
    Lead,
    Executive;

    @JsonCreator
    public static ExperienceLevel fromJson(JsonNode node) {
        if (node.isNumber()) {
            return values()[node.asInt()];
        }
        return valueOf(node.asText());
    }
}
