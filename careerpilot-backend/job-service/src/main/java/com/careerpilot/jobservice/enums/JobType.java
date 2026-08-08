package com.careerpilot.jobservice.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Names (and ordinal order) match CareerPilot.Domain.Enums.JobType exactly.
 * Jackson's default enum serialization is the constant name as a JSON
 * string, which is what the .NET side produces too via the globally
 * registered JsonStringEnumConverter - so JobCard.jsx rendering
 * {job.jobType} as "FullTime"/"Remote"/etc keeps working unmodified.
 *
 * CreateJob.jsx sends jobType as a plain NUMBER in the request body
 * (jobType: Number(e.target.value)), not a string - .NET's
 * JsonStringEnumConverter tolerates numeric input on read even though it
 * always writes string names. fromJson() replicates that read-side
 * tolerance; without it, Jackson would reject a numeric jobType outright.
 */
public enum JobType {
    FullTime,
    PartTime,
    Contract,
    Remote,
    Internship;

    @JsonCreator
    public static JobType fromJson(JsonNode node) {
        if (node.isNumber()) {
            return values()[node.asInt()];
        }
        return valueOf(node.asText());
    }
}
