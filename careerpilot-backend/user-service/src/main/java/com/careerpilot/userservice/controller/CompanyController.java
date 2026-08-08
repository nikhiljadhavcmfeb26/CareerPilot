package com.careerpilot.userservice.controller;

import com.careerpilot.userservice.dto.CompanyDto;
import com.careerpilot.userservice.dto.CreateCompanyRequest;
import com.careerpilot.userservice.dto.UpdateCompanyRequest;
import com.careerpilot.userservice.service.CompanyService;
import com.careerpilot.common.dto.ApiResponse;
import com.careerpilot.common.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/companies")
public class CompanyController {

    private final CompanyService companyService;

    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @PostMapping
    public ApiResponse<CompanyDto> register(@Valid @RequestBody CreateCompanyRequest request) {
        return ApiResponse.ok(companyService.registerCompany(SecurityUtils.currentUserId(), request), "Company registered");
    }

    @PutMapping
    public ApiResponse<CompanyDto> update(@Valid @RequestBody UpdateCompanyRequest request) {
        return ApiResponse.ok(companyService.updateCompany(SecurityUtils.currentUserId(), request), "Company updated");
    }

    @GetMapping("/my")
    public ApiResponse<CompanyDto> getMy() {
        return ApiResponse.ok(companyService.getMyCompany(SecurityUtils.currentUserId()));
    }

    /** ADMIN MODULE - full employer directory (approved and pending). */
    @GetMapping("/admin/all")
    public ApiResponse<List<CompanyDto>> getAllForAdmin() {
        return ApiResponse.ok(companyService.getAllEmployers());
    }

    @PutMapping("/{id}/revoke-approval")
    public ApiResponse<Void> revokeApproval(@PathVariable int id) {
        companyService.revokeEmployerApproval(id);
        return ApiResponse.success("Employer approval revoked");
    }

    @GetMapping("/pending")
    public ApiResponse<List<CompanyDto>> getPending() {
        return ApiResponse.ok(companyService.getPendingEmployers());
    }

    @PutMapping("/{id}/approve")
    public ApiResponse<Void> approve(@PathVariable int id) {
        companyService.approveEmployer(id);
        return ApiResponse.success("Employer approved");
    }
}
