package com.careerpilot.applicationservice.service;

import com.careerpilot.applicationservice.dto.ApplicationDto;
import com.careerpilot.applicationservice.dto.CreateApplicationRequest;
import com.careerpilot.applicationservice.dto.UpdateApplicationStatusRequest;

import java.util.List;

public interface ApplicationService {

    /** ADMIN MODULE - platform-wide application monitoring. Admin-only; see SecurityConfig. */
    List<ApplicationDto> getAllApplicationsForAdmin(String status, Integer jobId);


    ApplicationDto apply(int userId, CreateApplicationRequest request);

    void withdraw(int userId, int applicationId);

    List<ApplicationDto> getMyApplications(int userId);

    List<ApplicationDto> getJobApplicants(int userId, int jobId);

    void updateStatus(int userId, int applicationId, UpdateApplicationStatusRequest request);

    boolean hasApplied(int userId, int jobId);

    /**
     * NEW - the mandatory bug fix. Only the employer who owns the job this
     * application belongs to may reach this; the ownership check happens
     * here before ever calling out to user-service for the file itself.
     */
    ResumeFileContent downloadResumeForEmployer(int userId, int applicationId);

    record ResumeFileContent(String fileName, String contentType, byte[] content) {
    }
}
