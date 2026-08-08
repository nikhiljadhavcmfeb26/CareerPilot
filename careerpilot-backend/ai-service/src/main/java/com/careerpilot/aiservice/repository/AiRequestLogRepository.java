package com.careerpilot.aiservice.repository;

import com.careerpilot.aiservice.entity.AiRequestLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiRequestLogRepository extends JpaRepository<AiRequestLog, Integer> {
}
