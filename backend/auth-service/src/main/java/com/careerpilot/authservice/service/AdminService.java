package com.careerpilot.authservice.service;

import com.careerpilot.authservice.dto.admin.AdminStatsDto;
import com.careerpilot.authservice.dto.admin.AdminSubscriptionDto;
import com.careerpilot.authservice.dto.admin.AdminUserDetailDto;
import com.careerpilot.authservice.dto.admin.AiFeatureSettingsDto;
import com.careerpilot.authservice.dto.internal.AiAccessResponse;

import java.util.List;

public interface AdminService {

    // --- 1. user management ---
    List<AdminUserDetailDto> getUsers(String role, Boolean active, String search);

    AdminUserDetailDto getUser(int userId);

    AdminUserDetailDto setActive(int actingAdminId, int userId, boolean active);

    AdminUserDetailDto block(int actingAdminId, int userId, String reason);

    AdminUserDetailDto unblock(int actingAdminId, int userId);

    // --- 4. subscription management ---
    List<AdminSubscriptionDto> getSubscriptions(String status);

    AdminSubscriptionDto revokeSubscription(int subscriptionId);

    AdminSubscriptionDto extendSubscription(int subscriptionId, int days);

    // --- 5. AI feature management ---
    AiFeatureSettingsDto getAiSettings();

    AiFeatureSettingsDto updateAiSettings(AiFeatureSettingsDto settings);

    AdminUserDetailDto setUserAiEnabled(int actingAdminId, int userId, boolean enabled);

    /** Called by ai-service (via /internal) before every AI request. */
    AiAccessResponse checkAiAccess(int userId, String feature);

    // --- 6. dashboard ---
    AdminStatsDto getStats();
}
