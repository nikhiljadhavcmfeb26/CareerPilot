package com.careerpilot.userservice.service;

import com.careerpilot.userservice.dto.CompanyDto;
import com.careerpilot.userservice.dto.CreateCompanyRequest;
import com.careerpilot.userservice.dto.UpdateCompanyRequest;

import java.util.List;

public interface CompanyService {
    CompanyDto registerCompany(int userId, CreateCompanyRequest request);
    CompanyDto updateCompany(int userId, UpdateCompanyRequest request);
    CompanyDto getMyCompany(int userId);
    List<CompanyDto> getPendingEmployers();

    /**
     * ADMIN MODULE - every employer, not just the ones awaiting approval, so
     * an admin can revoke an approval they already granted rather than only
     * ever seeing the pending queue.
     */
    List<CompanyDto> getAllEmployers();

    void revokeEmployerApproval(int companyId);
    void approveEmployer(int companyId);
}
