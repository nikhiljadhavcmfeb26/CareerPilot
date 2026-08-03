package com.careerpilot.jobservice.repository;

import com.careerpilot.jobservice.entity.Bookmark;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookmarkRepository extends JpaRepository<Bookmark, Integer> {
    List<Bookmark> findByJobSeekerProfileId(Integer jobSeekerProfileId);
    int countByJobSeekerProfileId(Integer jobSeekerProfileId);
    Optional<Bookmark> findByJobIdAndJobSeekerProfileId(Integer jobId, Integer jobSeekerProfileId);
}
