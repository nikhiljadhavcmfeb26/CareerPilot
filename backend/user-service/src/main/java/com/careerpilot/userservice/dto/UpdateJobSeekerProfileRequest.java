package com.careerpilot.userservice.dto;

import jakarta.validation.constraints.Size;

public class UpdateJobSeekerProfileRequest {

    // Length caps mirror the JobSeekerProfile entity's columns.
    @Size(max = 255)
    private String headline;

    @Size(max = 2000)
    private String summary;

    @Size(max = 1000)
    private String skills;

    @Size(max = 2000)
    private String experience;

    @Size(max = 1000)
    private String education;

    @Size(max = 255)
    private String location;

    public String getHeadline() {
        return headline;
    }

    public void setHeadline(String headline) {
        this.headline = headline;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getSkills() {
        return skills;
    }

    public void setSkills(String skills) {
        this.skills = skills;
    }

    public String getExperience() {
        return experience;
    }

    public void setExperience(String experience) {
        this.experience = experience;
    }

    public String getEducation() {
        return education;
    }

    public void setEducation(String education) {
        this.education = education;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }
}
