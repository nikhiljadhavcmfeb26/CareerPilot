package com.careerpilot.applicationservice.entity;

import com.careerpilot.applicationservice.enums.ApplicationStatus;
import com.careerpilot.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * jobId, jobSeekerProfileId, and resumeId are all plain columns, not JPA
 * relationships - Job lives in job-service's database, JobSeekerProfile and
 * Resume live in user-service's. Every cross-service reference here is
 * validated at write time via Feign, not enforced by a DB-level FK.
 */
@Entity
@Table(name = "job_applications")
public class JobApplication extends BaseEntity {

    @Column(name = "job_id", nullable = false)
    private Integer jobId;

    @Column(name = "job_seeker_profile_id", nullable = false)
    private Integer jobSeekerProfileId;

    /**
     * Denormalized from the JWT at apply-time. The original's
     * ApplicantName/ApplicantEmail come from JobSeekerProfile.User, a
     * same-DB navigation - user-service only exposes a userId->profileId
     * lookup (/internal/job-seeker-profiles/by-user/{userId}), not the
     * reverse, so resolving auth-service's UserBasicInfo needs this id
     * directly rather than a second cross-service hop through user-service
     * just to find it. Never used for authorization - every ownership check
     * in this service still keys off jobSeekerProfileId, exactly like the
     * original.
     */
    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "resume_id")
    private Integer resumeId;

    /**
     * Denormalized at apply time from user-service, same rationale as userId
     * above. The employer's applicant list needs a filename to label the
     * "View Resume" link; resolving it per row would mean one extra
     * cross-service call for every applicant on the page. The file itself is
     * still fetched on demand, and only after the ownership check in
     * downloadResumeForEmployer().
     */
    @Column(name = "resume_file_name")
    private String resumeFileName;

    @Column(name = "cover_letter", length = 4000)
    private String coverLetter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationStatus status = ApplicationStatus.Pending;

    @Column(name = "applied_at", nullable = false)
    private LocalDateTime appliedAt = LocalDateTime.now();

    public Integer getJobId() {
        return jobId;
    }

    public void setJobId(Integer jobId) {
        this.jobId = jobId;
    }

    public Integer getJobSeekerProfileId() {
        return jobSeekerProfileId;
    }

    public void setJobSeekerProfileId(Integer jobSeekerProfileId) {
        this.jobSeekerProfileId = jobSeekerProfileId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getResumeId() {
        return resumeId;
    }

    public void setResumeId(Integer resumeId) {
        this.resumeId = resumeId;
    }

    public String getResumeFileName() {
        return resumeFileName;
    }

    public void setResumeFileName(String resumeFileName) {
        this.resumeFileName = resumeFileName;
    }

    public String getCoverLetter() {
        return coverLetter;
    }

    public void setCoverLetter(String coverLetter) {
        this.coverLetter = coverLetter;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public void setStatus(ApplicationStatus status) {
        this.status = status;
    }

    public LocalDateTime getAppliedAt() {
        return appliedAt;
    }

    public void setAppliedAt(LocalDateTime appliedAt) {
        this.appliedAt = appliedAt;
    }
}
