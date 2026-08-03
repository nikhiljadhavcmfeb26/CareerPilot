package com.careerpilot.userservice.repository;

import com.careerpilot.userservice.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Integer> {
    Optional<Company> findByEmployerProfileId(Integer employerProfileId);
    List<Company> findByIsApprovedFalse();
    int countByIsApprovedFalse();
    List<Company> findByNameContainingIgnoreCase(String keyword);

    /** ADMIN MODULE - every registered employer, newest first. */
    List<Company> findAllByOrderByCreatedAtDesc();
}
