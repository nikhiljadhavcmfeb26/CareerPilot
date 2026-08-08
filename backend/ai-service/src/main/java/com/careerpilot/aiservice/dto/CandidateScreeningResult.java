package com.careerpilot.aiservice.dto;

import com.careerpilot.aiservice.enums.ScreeningRecommendation;

import java.util.List;

/** Matches AI FEATURE 3 from your spec: Match Score, Skill Match Percentage, Strengths, Weaknesses, Missing Skills, Recommendation - one per applicant, sorted by matchScore descending by the caller. */
public class CandidateScreeningResult {

    private Integer applicationId;
    private String applicantName;
    private int matchScore;
    private int skillMatchPercentage;
    private List<String> strengths;
    private List<String> weaknesses;
    private List<String> missingSkills;
    private ScreeningRecommendation recommendation;

    public Integer getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(Integer applicationId) {
        this.applicationId = applicationId;
    }

    public String getApplicantName() {
        return applicantName;
    }

    public void setApplicantName(String applicantName) {
        this.applicantName = applicantName;
    }

    public int getMatchScore() {
        return matchScore;
    }

    public void setMatchScore(int matchScore) {
        this.matchScore = matchScore;
    }

    public int getSkillMatchPercentage() {
        return skillMatchPercentage;
    }

    public void setSkillMatchPercentage(int skillMatchPercentage) {
        this.skillMatchPercentage = skillMatchPercentage;
    }

    public List<String> getStrengths() {
        return strengths;
    }

    public void setStrengths(List<String> strengths) {
        this.strengths = strengths;
    }

    public List<String> getWeaknesses() {
        return weaknesses;
    }

    public void setWeaknesses(List<String> weaknesses) {
        this.weaknesses = weaknesses;
    }

    public List<String> getMissingSkills() {
        return missingSkills;
    }

    public void setMissingSkills(List<String> missingSkills) {
        this.missingSkills = missingSkills;
    }

    public ScreeningRecommendation getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(ScreeningRecommendation recommendation) {
        this.recommendation = recommendation;
    }
}
