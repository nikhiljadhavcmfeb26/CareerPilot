package com.careerpilot.aiservice.dto;

import jakarta.validation.constraints.NotNull;

public class CandidateScreeningRequest {

    @NotNull
    private Integer jobId;

    public Integer getJobId() {
        return jobId;
    }

    public void setJobId(Integer jobId) {
        this.jobId = jobId;
    }
}
