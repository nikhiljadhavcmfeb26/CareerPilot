package com.careerpilot.jobservice.entity;

import com.careerpilot.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Job is a real same-DB FK (both Job and Bookmark live in job-service).
 * jobSeekerProfileId is a plain column - JobSeekerProfile lives in
 * user-service's own database, a cross-service reference.
 */
@Entity
@Table(name = "bookmarks", uniqueConstraints = @UniqueConstraint(columnNames = {"job_id", "job_seeker_profile_id"}))
public class Bookmark extends BaseEntity {

    @Column(name = "job_seeker_profile_id", nullable = false)
    private Integer jobSeekerProfileId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    public Integer getJobSeekerProfileId() {
        return jobSeekerProfileId;
    }

    public void setJobSeekerProfileId(Integer jobSeekerProfileId) {
        this.jobSeekerProfileId = jobSeekerProfileId;
    }

    public Job getJob() {
        return job;
    }

    public void setJob(Job job) {
        this.job = job;
    }
}
