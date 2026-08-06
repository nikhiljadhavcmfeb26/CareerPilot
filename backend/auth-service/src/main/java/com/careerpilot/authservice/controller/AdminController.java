package com.careerpilot.authservice.controller;

import com.careerpilot.authservice.dto.admin.AdminStatsDto;
import com.careerpilot.authservice.dto.admin.AdminSubscriptionDto;
import com.careerpilot.authservice.dto.admin.AdminUserDetailDto;
import com.careerpilot.authservice.dto.admin.AiFeatureSettingsDto;
import com.careerpilot.authservice.dto.admin.BlockUserRequest;
import com.careerpilot.authservice.dto.admin.ExtendSubscriptionRequest;
import com.careerpilot.authservice.service.AdminService;
import com.careerpilot.common.dto.ApiResponse;
import com.careerpilot.common.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * ADMIN MODULE - everything under /api/admin/** is Admin-only.
 *
 * Authorization is enforced in three independent places, in line with how the
 * rest of the platform already works:
 *   1. api-gateway validates the JWT at the edge (routes /api/admin/** here),
 *   2. this service's SecurityConfig requires hasRole("Admin") for the whole
 *      prefix, so no individual method can accidentally be left open,
 *   3. AdminServiceImpl re-checks the acting admin's identity for the
 *      self-lockout rules.
 *
 * Job and application administration are NOT proxied through here - they live
 * on the services that own that data (job-service: /api/jobs/admin/**,
 * application-service: /api/applications/admin/**), also Admin-only. Adding a
 * proxy would have meant auth-service holding opinions about job records it
 * has no other reason to know about.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    // --- 6. dashboard ---

    @GetMapping("/stats")
    public ApiResponse<AdminStatsDto> stats() {
        return ApiResponse.ok(adminService.getStats());
    }

    // --- 1. user management ---

    /**
     * @param role   optional filter: JobSeeker | Employer | Admin
     * @param active optional filter on the activation flag
     * @param search optional case-insensitive match on email or full name
     */
    @GetMapping("/users")
    public ApiResponse<List<AdminUserDetailDto>> users(@RequestParam(required = false) String role,
                                                        @RequestParam(required = false) Boolean active,
                                                        @RequestParam(required = false) String search) {
        return ApiResponse.ok(adminService.getUsers(role, active, search));
    }

    @GetMapping("/users/{id}")
    public ApiResponse<AdminUserDetailDto> user(@PathVariable int id) {
        return ApiResponse.ok(adminService.getUser(id));
    }

    @PutMapping("/users/{id}/activate")
    public ApiResponse<AdminUserDetailDto> activate(@PathVariable int id) {
        return ApiResponse.ok(adminService.setActive(SecurityUtils.currentUserId(), id, true), "User activated");
    }

    @PutMapping("/users/{id}/deactivate")
    public ApiResponse<AdminUserDetailDto> deactivate(@PathVariable int id) {
        return ApiResponse.ok(adminService.setActive(SecurityUtils.currentUserId(), id, false), "User deactivated");
    }

    @PutMapping("/users/{id}/block")
    public ApiResponse<AdminUserDetailDto> block(@PathVariable int id,
                                                  @Valid @RequestBody(required = false) BlockUserRequest request) {
        String reason = request != null ? request.getReason() : null;
        return ApiResponse.ok(adminService.block(SecurityUtils.currentUserId(), id, reason), "User blocked");
    }

    @PutMapping("/users/{id}/unblock")
    public ApiResponse<AdminUserDetailDto> unblock(@PathVariable int id) {
        return ApiResponse.ok(adminService.unblock(SecurityUtils.currentUserId(), id), "User unblocked");
    }

    // --- 4. subscription management ---

    /** @param status optional filter: CREATED | PAID | FAILED */
    @GetMapping("/subscriptions")
    public ApiResponse<List<AdminSubscriptionDto>> subscriptions(@RequestParam(required = false) String status) {
        return ApiResponse.ok(adminService.getSubscriptions(status));
    }

    @PutMapping("/subscriptions/{id}/revoke")
    public ApiResponse<AdminSubscriptionDto> revoke(@PathVariable int id) {
        return ApiResponse.ok(adminService.revokeSubscription(id), "Subscription revoked");
    }

    @PutMapping("/subscriptions/{id}/extend")
    public ApiResponse<AdminSubscriptionDto> extend(@PathVariable int id,
                                                     @Valid @RequestBody(required = false) ExtendSubscriptionRequest request) {
        int days = request != null ? request.getDays() : 30;
        return ApiResponse.ok(adminService.extendSubscription(id, days), "Subscription extended by " + days + " days");
    }

    // --- 5. AI feature management ---

    @GetMapping("/ai-settings")
    public ApiResponse<AiFeatureSettingsDto> aiSettings() {
        return ApiResponse.ok(adminService.getAiSettings());
    }

    @PutMapping("/ai-settings")
    public ApiResponse<AiFeatureSettingsDto> updateAiSettings(@RequestBody AiFeatureSettingsDto request) {
        return ApiResponse.ok(adminService.updateAiSettings(request), "AI settings updated");
    }

    @PutMapping("/users/{id}/ai/enable")
    public ApiResponse<AdminUserDetailDto> enableAi(@PathVariable int id) {
        return ApiResponse.ok(adminService.setUserAiEnabled(SecurityUtils.currentUserId(), id, true),
                "AI access enabled for user");
    }

    @PutMapping("/users/{id}/ai/disable")
    public ApiResponse<AdminUserDetailDto> disableAi(@PathVariable int id) {
        return ApiResponse.ok(adminService.setUserAiEnabled(SecurityUtils.currentUserId(), id, false),
                "AI access disabled for user");
    }
}
