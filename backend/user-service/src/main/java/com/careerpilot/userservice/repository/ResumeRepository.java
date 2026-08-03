package com.careerpilot.userservice.repository;

import com.careerpilot.userservice.entity.Resume;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ResumeRepository extends JpaRepository<Resume, Integer> {
    List<Resume> findByJobSeekerProfileIdOrderByUploadedAtDesc(Integer jobSeekerProfileId);
    int countByJobSeekerProfileId(Integer jobSeekerProfileId);
    Optional<Resume> findByJobSeekerProfileIdAndIsDefaultTrue(Integer jobSeekerProfileId);
}
