package com.careerpilot.aiservice.dto;

import jakarta.validation.constraints.NotNull;

/** Uses the calling candidate's default resume (see ResumeService.downloadDefaultForProfile) - only the target job needs specifying. */
public class CoverLetterRequest {

    @NotNull
    private Integer jobId;

    public Integer getJobId() {
        return jobId;
    }

    public void setJobId(Integer jobId) {
        this.jobId = jobId;
    }
}
