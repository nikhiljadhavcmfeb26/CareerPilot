package com.careerpilot.userservice.repository;

import com.careerpilot.userservice.entity.EmployerProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmployerProfileRepository extends JpaRepository<EmployerProfile, Integer> {
    Optional<EmployerProfile> findByUserId(Integer userId);
}
