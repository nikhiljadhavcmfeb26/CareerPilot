package com.careerpilot.userservice.repository;

import com.careerpilot.userservice.entity.JobSeekerProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JobSeekerProfileRepository extends JpaRepository<JobSeekerProfile, Integer> {
    Optional<JobSeekerProfile> findByUserId(Integer userId);
}
