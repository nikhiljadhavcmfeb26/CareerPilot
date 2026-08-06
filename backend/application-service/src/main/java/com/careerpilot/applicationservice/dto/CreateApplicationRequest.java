package com.careerpilot.applicationservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CreateApplicationRequest {

    @NotNull(message = "jobId is required")
    private Integer jobId;

    /** Optional: falls back to the candidate's default resume when absent. */
    private Integer resumeId;

    // Matches JobApplication.coverLetter's column length - without this a long
    // cover letter failed with a raw database truncation error instead of a 400.
    @Size(max = 4000, message = "Cover letter must be at most 4000 characters")
    private String coverLetter;

    public Integer getJobId() {
        return jobId;
    }

    public void setJobId(Integer jobId) {
        this.jobId = jobId;
    }

    public Integer getResumeId() {
        return resumeId;
    }

    public void setResumeId(Integer resumeId) {
        this.resumeId = resumeId;
    }

    public String getCoverLetter() {
        return coverLetter;
    }

    public void setCoverLetter(String coverLetter) {
        this.coverLetter = coverLetter;
    }
}
