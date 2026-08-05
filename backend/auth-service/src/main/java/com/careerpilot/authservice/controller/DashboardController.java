package com.careerpilot.authservice.controller;

import com.careerpilot.authservice.dto.dashboard.AdminDashboardDto;
import com.careerpilot.authservice.dto.dashboard.EmployerDashboardDto;
import com.careerpilot.authservice.dto.dashboard.JobSeekerDashboardDto;
import com.careerpilot.authservice.service.DashboardService;
import com.careerpilot.common.dto.ApiResponse;
import com.careerpilot.common.security.SecurityUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The three dashboard endpoints the React app has always called
 * (client/src/api/services.js -> dashboardApi) but which no service previously
 * implemented, so every user's landing page rendered zeros.
 *
 * Hosted here in auth-service and aggregated over OpenFeign, per the agreed
 * decision. Role enforcement lives in SecurityConfig alongside every other
 * route in this service; the paths deliberately mirror the existing
 * "/dashboard/admin | /employer | /jobseeker" contract the client already uses,
 * so no frontend URL changes were needed.
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/admin")
    public ApiResponse<AdminDashboardDto> admin() {
        return ApiResponse.ok(dashboardService.getAdminDashboard());
    }

    @GetMapping("/employer")
    public ApiResponse<EmployerDashboardDto> employer() {
        return ApiResponse.ok(dashboardService.getEmployerDashboard(SecurityUtils.currentUserId()));
    }

    @GetMapping("/jobseeker")
    public ApiResponse<JobSeekerDashboardDto> jobSeeker() {
        return ApiResponse.ok(dashboardService.getJobSeekerDashboard(SecurityUtils.currentUserId()));
    }
}
