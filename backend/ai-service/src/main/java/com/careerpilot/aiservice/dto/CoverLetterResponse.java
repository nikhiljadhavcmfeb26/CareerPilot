package com.careerpilot.aiservice.dto;

/** Matches AI FEATURE 2 from your spec: a generated cover letter the frontend lets the user edit and copy. */
public class CoverLetterResponse {

    private String coverLetter;

    public CoverLetterResponse() {
    }

    public CoverLetterResponse(String coverLetter) {
        this.coverLetter = coverLetter;
    }

    public String getCoverLetter() {
        return coverLetter;
    }

    public void setCoverLetter(String coverLetter) {
        this.coverLetter = coverLetter;
    }
}
