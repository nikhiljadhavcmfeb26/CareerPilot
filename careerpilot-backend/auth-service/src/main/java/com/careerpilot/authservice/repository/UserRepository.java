package com.careerpilot.authservice.repository;

import com.careerpilot.authservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("select u from User u join fetch u.role order by u.createdAt desc")
    List<User> findAllWithRoles();

    /** Admin dashboard: counts per role, resolved through the role association. */
    int countByRoleName(String roleName);

    /** Admin module: headline account-health counters. */
    int countByIsActiveTrue();

    int countByIsBlockedTrue();
}
