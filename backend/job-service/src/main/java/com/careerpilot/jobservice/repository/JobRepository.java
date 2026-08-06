package com.careerpilot.jobservice.repository;

import com.careerpilot.jobservice.entity.Job;
import com.careerpilot.jobservice.enums.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface JobRepository extends JpaRepository<Job, Integer>, JpaSpecificationExecutor<Job> {
    List<Job> findByCompanyIdOrderByCreatedAtDesc(Integer companyId);
    int countByCompanyId(Integer companyId);
    int countByStatus(JobStatus status);
    List<Job> findTop50ByStatusOrderByPublishedAtDesc(JobStatus status);
}
