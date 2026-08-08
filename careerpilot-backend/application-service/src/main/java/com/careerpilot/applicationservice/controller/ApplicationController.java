package com.careerpilot.applicationservice.controller;

import com.careerpilot.applicationservice.dto.ApplicationDto;
import com.careerpilot.applicationservice.dto.CreateApplicationRequest;
import com.careerpilot.applicationservice.dto.UpdateApplicationStatusRequest;
import com.careerpilot.applicationservice.service.ApplicationService;
import com.careerpilot.applicationservice.service.ApplicationService.ResumeFileContent;
import com.careerpilot.common.dto.ApiResponse;
import com.careerpilot.common.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {

    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PostMapping
    public ApiResponse<ApplicationDto> apply(@Valid @RequestBody CreateApplicationRequest request) {
        return ApiResponse.ok(applicationService.apply(SecurityUtils.currentUserId(), request), "Application submitted");
    }

    @PutMapping("/{id}/withdraw")
    public ApiResponse<Void> withdraw(@PathVariable int id) {
        applicationService.withdraw(SecurityUtils.currentUserId(), id);
        return ApiResponse.success("Application withdrawn");
    }

    /**
     * ADMIN MODULE - recruitment monitoring. Returns every application on the
     * platform, newest first, with optional filters. Deliberately lives here
     * rather than being proxied through auth-service's AdminController:
     * application-service owns this table, and it already knows how to hydrate
     * job titles and applicant names for the DTO.
     *
     * @param status optional ApplicationStatus filter (Pending, Shortlisted, ...)
     * @param jobId  optional single-job filter
     */
    @GetMapping("/admin/all")
    public ApiResponse<List<ApplicationDto>> getAllForAdmin(@RequestParam(required = false) String status,
                                                             @RequestParam(required = false) Integer jobId) {
        return ApiResponse.ok(applicationService.getAllApplicationsForAdmin(status, jobId));
    }

    @GetMapping("/my")
    public ApiResponse<List<ApplicationDto>> getMyApplications() {
        return ApiResponse.ok(applicationService.getMyApplications(SecurityUtils.currentUserId()));
    }

    @GetMapping("/job/{jobId}")
    public ApiResponse<List<ApplicationDto>> getJobApplicants(@PathVariable int jobId) {
        return ApiResponse.ok(applicationService.getJobApplicants(SecurityUtils.currentUserId(), jobId));
    }

    @PutMapping("/{id}/status")
    public ApiResponse<Void> updateStatus(@PathVariable int id, @Valid @RequestBody UpdateApplicationStatusRequest request) {
        applicationService.updateStatus(SecurityUtils.currentUserId(), id, request);
        return ApiResponse.success("Status updated");
    }

    @GetMapping("/check/{jobId}")
    public ApiResponse<Map<String, Boolean>> checkApplication(@PathVariable int jobId) {
        boolean applied = applicationService.hasApplied(SecurityUtils.currentUserId(), jobId);
        return ApiResponse.ok(Map.of("applied", applied));
    }

    /**
     * NEW - the mandatory bug fix. Employer clicks "View Resume" on the
     * applicant list; this streams the PDF back directly if (and only if)
     * they own the job this application belongs to.
     */
    @GetMapping("/{id}/resume")
    public ResponseEntity<byte[]> downloadResume(@PathVariable int id) {
        ResumeFileContent file = applicationService.downloadResumeForEmployer(SecurityUtils.currentUserId(), id);

        ContentDisposition disposition = ContentDisposition.inline().filename(file.fileName()).build();

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(file.content());
    }
}
