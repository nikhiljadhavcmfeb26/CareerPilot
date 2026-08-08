package com.careerpilot.aiservice.dto;

/** New per this request's explicit "Job recommendation support" ask - not in the original four AI features from the migration spec, but a natural, clearly-scoped extension using the same resume-matching machinery as Resume Feedback. */
public class JobRecommendationResult {

    private Integer jobId;
    private String jobTitle;
    private String companyName;
    private int matchScore;
    private String reasoning;

    public Integer getJobId() {
        return jobId;
    }

    public void setJobId(Integer jobId) {
        this.jobId = jobId;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public int getMatchScore() {
        return matchScore;
    }

    public void setMatchScore(int matchScore) {
        this.matchScore = matchScore;
    }

    public String getReasoning() {
        return reasoning;
    }

    public void setReasoning(String reasoning) {
        this.reasoning = reasoning;
    }
}
