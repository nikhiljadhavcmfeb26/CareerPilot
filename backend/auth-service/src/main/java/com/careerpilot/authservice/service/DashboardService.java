package com.careerpilot.authservice.service;

import com.careerpilot.authservice.dto.dashboard.AdminDashboardDto;
import com.careerpilot.authservice.dto.dashboard.EmployerDashboardDto;
import com.careerpilot.authservice.dto.dashboard.JobSeekerDashboardDto;

public interface DashboardService {

    AdminDashboardDto getAdminDashboard();

    EmployerDashboardDto getEmployerDashboard(int userId);

    JobSeekerDashboardDto getJobSeekerDashboard(int userId);
}
