package com.careerpilot.jobservice.dto;

import jakarta.validation.constraints.Size;

import com.careerpilot.jobservice.enums.ExperienceLevel;
import com.careerpilot.jobservice.enums.JobType;

import java.math.BigDecimal;

/**
 * Deliberately has NO validation annotations, and the controller does not
 * apply @Valid to it - this matches JobsController.cs's Update action
 * exactly, which (unlike Create) never runs UpdateJobDto through any
 * validator. Preserved as-is rather than "fixed", since the instruction was
 * to preserve existing business logic, not improve on it.
 */
public class UpdateJobRequest {

    // Length caps mirror the Job entity's columns (title/location default 255,
    // description/requirements 4000). Without them an over-long value reached
    // the database and surfaced as a 500 rather than a 400.
    @Size(max = 200)
    private String title;

    @Size(max = 4000)
    private String description;

    @Size(max = 4000)
    private String requirements;

    @Size(max = 255)
    private String location;
    private JobType jobType;
    private BigDecimal salaryMin;
    private BigDecimal salaryMax;
    private ExperienceLevel experienceLevel;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRequirements() {
        return requirements;
    }

    public void setRequirements(String requirements) {
        this.requirements = requirements;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public JobType getJobType() {
        return jobType;
    }

    public void setJobType(JobType jobType) {
        this.jobType = jobType;
    }

    public BigDecimal getSalaryMin() {
        return salaryMin;
    }

    public void setSalaryMin(BigDecimal salaryMin) {
        this.salaryMin = salaryMin;
    }

    public BigDecimal getSalaryMax() {
        return salaryMax;
    }

    public void setSalaryMax(BigDecimal salaryMax) {
        this.salaryMax = salaryMax;
    }

    public ExperienceLevel getExperienceLevel() {
        return experienceLevel;
    }

    public void setExperienceLevel(ExperienceLevel experienceLevel) {
        this.experienceLevel = experienceLevel;
    }
}
