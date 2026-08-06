package com.careerpilot.applicationservice.repository;

import com.careerpilot.applicationservice.entity.JobApplication;
import com.careerpilot.applicationservice.enums.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface JobApplicationRepository extends JpaRepository<JobApplication, Integer> {
    Optional<JobApplication> findByJobIdAndJobSeekerProfileId(Integer jobId, Integer jobSeekerProfileId);
    List<JobApplication> findByJobSeekerProfileIdOrderByAppliedAtDesc(Integer jobSeekerProfileId);
    List<JobApplication> findByJobIdOrderByAppliedAtDesc(Integer jobId);
    int countByJobId(Integer jobId);

    // --- admin monitoring ---
    List<JobApplication> findAllByOrderByAppliedAtDesc();
    List<JobApplication> findByStatusOrderByAppliedAtDesc(ApplicationStatus status);
    List<JobApplication> findByJobIdAndStatusOrderByAppliedAtDesc(Integer jobId, ApplicationStatus status);

    // --- dashboard aggregation (auth-service) ---
    int countByStatus(ApplicationStatus status);
    int countByJobIdIn(Collection<Integer> jobIds);
    int countByJobIdInAndStatus(Collection<Integer> jobIds, ApplicationStatus status);
    int countByJobSeekerProfileId(Integer jobSeekerProfileId);
    int countByJobSeekerProfileIdAndStatus(Integer jobSeekerProfileId, ApplicationStatus status);
}
