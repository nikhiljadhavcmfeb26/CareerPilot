package com.careerpilot.jobservice.dto;

import jakarta.validation.constraints.NotNull;

public class CreateBookmarkRequest {

    @NotNull(message = "jobId is required")
    private Integer jobId;

    public Integer getJobId() {
        return jobId;
    }

    public void setJobId(Integer jobId) {
        this.jobId = jobId;
    }
}
