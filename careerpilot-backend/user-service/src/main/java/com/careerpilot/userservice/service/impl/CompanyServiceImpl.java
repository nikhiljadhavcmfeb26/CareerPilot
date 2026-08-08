package com.careerpilot.userservice.service.impl;

import com.careerpilot.userservice.client.JobServiceClient;
import com.careerpilot.userservice.dto.CompanyDto;
import com.careerpilot.userservice.dto.CreateCompanyRequest;
import com.careerpilot.userservice.dto.UpdateCompanyRequest;
import com.careerpilot.userservice.entity.Company;
import com.careerpilot.userservice.entity.EmployerProfile;
import com.careerpilot.userservice.repository.CompanyRepository;
import com.careerpilot.userservice.repository.EmployerProfileRepository;
import com.careerpilot.userservice.service.CompanyService;
import com.careerpilot.common.exception.BadRequestException;
import com.careerpilot.common.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CompanyServiceImpl implements CompanyService {

    private static final Logger log = LoggerFactory.getLogger(CompanyServiceImpl.class);

    private final CompanyRepository companyRepository;
    private final EmployerProfileRepository employerProfileRepository;
    private final JobServiceClient jobServiceClient;

    public CompanyServiceImpl(CompanyRepository companyRepository, EmployerProfileRepository employerProfileRepository,
                               JobServiceClient jobServiceClient) {
        this.companyRepository = companyRepository;
        this.employerProfileRepository = employerProfileRepository;
        this.jobServiceClient = jobServiceClient;
    }

    @Override
    @Transactional
    public CompanyDto registerCompany(int userId, CreateCompanyRequest request) {
        EmployerProfile employerProfile = findEmployerProfile(userId, "Only employers can register a company.");

        if (companyRepository.findByEmployerProfileId(employerProfile.getId()).isPresent()) {
            throw new BadRequestException("Company already registered.");
        }

        Company company = new Company();
        company.setName(request.getName());
        company.setDescription(request.getDescription());
        company.setWebsite(request.getWebsite());
        company.setIndustry(request.getIndustry());
        company.setLocation(request.getLocation());
        company.setEmployerProfile(employerProfile);
        company.setApproved(false);

        companyRepository.save(company);
        return toDto(company);
    }

    @Override
    @Transactional
    public CompanyDto updateCompany(int userId, UpdateCompanyRequest request) {
        EmployerProfile employerProfile = findEmployerProfile(userId, "Only employers can update company.");
        Company company = companyRepository.findByEmployerProfileId(employerProfile.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Company not found."));

        company.setName(request.getName());
        company.setDescription(request.getDescription());
        company.setWebsite(request.getWebsite());
        company.setIndustry(request.getIndustry());
        company.setLocation(request.getLocation());

        companyRepository.save(company);
        return toDto(company);
    }

    @Override
    public CompanyDto getMyCompany(int userId) {
        EmployerProfile employerProfile = findEmployerProfile(userId, "Only employers have companies.");
        Company company = companyRepository.findByEmployerProfileId(employerProfile.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Company not found."));
        return toDto(company);
    }

    @Override
    public List<CompanyDto> getPendingEmployers() {
        return companyRepository.findByIsApprovedFalse().stream().map(this::toDto).toList();
    }

    @Override
    public List<CompanyDto> getAllEmployers() {
        return companyRepository.findAllByOrderByCreatedAtDesc().stream().map(this::toDto).toList();
    }

    /**
     * ADMIN MODULE. Approval was previously a one-way door - once granted it
     * could never be taken back without editing the database, which made
     * "remove invalid or fake jobs" a whack-a-mole exercise because the
     * employer could simply publish more. Un-approving stops new publishes
     * (JobServiceImpl.publishJob checks isApproved) without touching jobs that
     * are already live; delete those individually via /api/jobs/admin/{id}.
     */
    @Override
    @Transactional
    public void revokeEmployerApproval(int companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found."));
        company.setApproved(false);
        companyRepository.save(company);
    }

    @Override
    @Transactional
    public void approveEmployer(int companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found."));

        company.setApproved(true);
        companyRepository.save(company);

        // Same-database relationship (Company and EmployerProfile both live in
        // user-service), so this is a direct fetch rather than the .NET
        // version's loop over every user to find a profile-id match - same
        // outcome, no behavior change, just a more direct query.
        EmployerProfile employerProfile = company.getEmployerProfile();
        if (employerProfile != null) {
            employerProfile.setApproved(true);
            employerProfileRepository.save(employerProfile);
        }
    }

    private EmployerProfile findEmployerProfile(int userId, String errorMessage) {
        return employerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException(errorMessage));
    }

    private CompanyDto toDto(Company company) {
        CompanyDto dto = new CompanyDto();
        dto.setId(company.getId());
        dto.setName(company.getName());
        dto.setDescription(company.getDescription());
        dto.setWebsite(company.getWebsite());
        dto.setIndustry(company.getIndustry());
        dto.setLocation(company.getLocation());
        dto.setLogoUrl(company.getLogoUrl());
        dto.setApproved(company.isApproved());

        try {
            dto.setJobCount(jobServiceClient.countByCompany(company.getId()));
        } catch (Exception ex) {
            log.debug("job-service unavailable while counting jobs for company {}, defaulting to 0", company.getId());
            dto.setJobCount(0);
        }

        return dto;
    }
}
