package com.careerpilot.jobservice.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.JsonNode;

/** Names and ordinal order match CareerPilot.Domain.Enums.JobStatus exactly. Currently only ever a response field, but kept consistent with the other two enums. */
public enum JobStatus {
    Draft,
    Published,
    Closed;

    @JsonCreator
    public static JobStatus fromJson(JsonNode node) {
        if (node.isNumber()) {
            return values()[node.asInt()];
        }
        return valueOf(node.asText());
    }
}
