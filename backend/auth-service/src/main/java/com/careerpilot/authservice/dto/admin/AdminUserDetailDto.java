package com.careerpilot.authservice.dto.admin;

import java.time.LocalDateTime;

/**
 * Admin's view of one account. A record, not a POJO, so Jackson emits
 * "isActive"/"isBlocked"/"aiEnabled" verbatim - a POJO getter isActive() would
 * serialize as "active" and quietly break the admin table (the same trap the
 * dashboard DTOs document).
 */
public record AdminUserDetailDto(
        Integer id,
        String email,
        String firstName,
        String lastName,
        String phone,
        String role,
        boolean isActive,
        boolean isBlocked,
        String blockedReason,
        LocalDateTime blockedAt,
        boolean aiEnabled,
        boolean premium,
        LocalDateTime premiumExpiresAt,
        LocalDateTime createdAt) {
}
