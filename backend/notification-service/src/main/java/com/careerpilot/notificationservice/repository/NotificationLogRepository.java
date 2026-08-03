package com.careerpilot.notificationservice.repository;

import com.careerpilot.notificationservice.entity.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, Integer> {
}
