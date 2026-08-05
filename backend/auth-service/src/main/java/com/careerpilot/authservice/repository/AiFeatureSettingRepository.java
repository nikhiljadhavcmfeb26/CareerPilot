package com.careerpilot.authservice.repository;

import com.careerpilot.authservice.entity.AiFeatureSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AiFeatureSettingRepository extends JpaRepository<AiFeatureSetting, Integer> {

    /** There is only ever one row; this is how every caller reads it. */
    Optional<AiFeatureSetting> findFirstByOrderByIdAsc();
}
