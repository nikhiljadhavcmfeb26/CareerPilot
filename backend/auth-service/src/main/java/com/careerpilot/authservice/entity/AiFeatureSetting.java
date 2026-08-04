package com.careerpilot.authservice.entity;

import com.careerpilot.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;


@Entity
@Table(name = "ai_feature_settings")
public class AiFeatureSetting extends BaseEntity {

    /** Master switch. When false, no AI endpoint works for anyone. */
    @Column(name = "ai_enabled", nullable = false)
    private boolean aiEnabled = true;

    @Column(name = "resume_feedback_enabled", nullable = false)
    private boolean resumeFeedbackEnabled = true;

    @Column(name = "cover_letter_enabled", nullable = false)
    private boolean coverLetterEnabled = true;

    @Column(name = "job_recommendations_enabled", nullable = false)
    private boolean jobRecommendationsEnabled = true;

    @Column(name = "candidate_screening_enabled", nullable = false)
    private boolean candidateScreeningEnabled = true;

    @Column(name = "rejection_feedback_enabled", nullable = false)
    private boolean rejectionFeedbackEnabled = true;

    /** When false, AI features stop being Premium-only. */
    @Column(name = "require_premium", nullable = false)
    private boolean requirePremium = true;

    public boolean isAiEnabled() {
        return aiEnabled;
    }

    public void setAiEnabled(boolean aiEnabled) {
        this.aiEnabled = aiEnabled;
    }

    public boolean isResumeFeedbackEnabled() {
        return resumeFeedbackEnabled;
    }

    public void setResumeFeedbackEnabled(boolean resumeFeedbackEnabled) {
        this.resumeFeedbackEnabled = resumeFeedbackEnabled;
    }

    public boolean isCoverLetterEnabled() {
        return coverLetterEnabled;
    }

    public void setCoverLetterEnabled(boolean coverLetterEnabled) {
        this.coverLetterEnabled = coverLetterEnabled;
    }

    public boolean isJobRecommendationsEnabled() {
        return jobRecommendationsEnabled;
    }

    public void setJobRecommendationsEnabled(boolean jobRecommendationsEnabled) {
        this.jobRecommendationsEnabled = jobRecommendationsEnabled;
    }

    public boolean isCandidateScreeningEnabled() {
        return candidateScreeningEnabled;
    }

    public void setCandidateScreeningEnabled(boolean candidateScreeningEnabled) {
        this.candidateScreeningEnabled = candidateScreeningEnabled;
    }

    public boolean isRejectionFeedbackEnabled() {
        return rejectionFeedbackEnabled;
    }

    public void setRejectionFeedbackEnabled(boolean rejectionFeedbackEnabled) {
        this.rejectionFeedbackEnabled = rejectionFeedbackEnabled;
    }

    public boolean isRequirePremium() {
        return requirePremium;
    }

    public void setRequirePremium(boolean requirePremium) {
        this.requirePremium = requirePremium;
    }
}
