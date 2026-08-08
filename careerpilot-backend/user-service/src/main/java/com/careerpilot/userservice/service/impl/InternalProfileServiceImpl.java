package com.careerpilot.userservice.service.impl;

import com.careerpilot.userservice.dto.internal.CompanyLookupResponse;
import com.careerpilot.userservice.dto.internal.CompanySummaryResponse;
import com.careerpilot.userservice.dto.internal.CreateProfileRequest;
import com.careerpilot.userservice.dto.internal.JobSeekerProfileLookupResponse;
import com.careerpilot.userservice.dto.internal.ProfileSummaryResponse;
import com.careerpilot.userservice.entity.Company;
import com.careerpilot.userservice.entity.EmployerProfile;
import com.careerpilot.userservice.entity.JobSeekerProfile;
import com.careerpilot.userservice.repository.CompanyRepository;
import com.careerpilot.userservice.repository.EmployerProfileRepository;
import com.careerpilot.userservice.repository.JobSeekerProfileRepository;
import com.careerpilot.userservice.service.InternalProfileService;
import com.careerpilot.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class InternalProfileServiceImpl implements InternalProfileService {

    private static final String ROLE_EMPLOYER = "Employer";
    private static final String ROLE_JOB_SEEKER = "JobSeeker";

    private final EmployerProfileRepository employerProfileRepository;
    private final JobSeekerProfileRepository jobSeekerProfileRepository;
    private final CompanyRepository companyRepository;

    public InternalProfileServiceImpl(EmployerProfileRepository employerProfileRepository,
                                       JobSeekerProfileRepository jobSeekerProfileRepository,
                                       CompanyRepository companyRepository) {
        this.employerProfileRepository = employerProfileRepository;
        this.jobSeekerProfileRepository = jobSeekerProfileRepository;
        this.companyRepository = companyRepository;
    }

    @Override
    @Transactional
    public void createProfile(CreateProfileRequest request) {
        if (ROLE_EMPLOYER.equals(request.role())) {
            EmployerProfile profile = new EmployerProfile();
            profile.setUserId(request.userId());
            profile.setApproved(false);
            employerProfileRepository.save(profile);
        } else if (ROLE_JOB_SEEKER.equals(request.role())) {
            JobSeekerProfile profile = new JobSeekerProfile();
            profile.setUserId(request.userId());
            jobSeekerProfileRepository.save(profile);
        }
        // Any other role (e.g. Admin) gets no profile - matches the .NET
        // AuthService.RegisterAsync, which only attaches a profile for
        // Employer/JobSeeker registrations.
    }

    @Override
    public ProfileSummaryResponse getProfileSummary(int userId) {
        Optional<EmployerProfile> employerProfile = employerProfileRepository.findByUserId(userId);
        Integer employerProfileId = employerProfile.map(EmployerProfile::getId).orElse(null);
        Boolean employerApproved = employerProfile.map(EmployerProfile::isApproved).orElse(null);
        Integer jobSeekerProfileId = jobSeekerProfileRepository.findByUserId(userId).map(JobSeekerProfile::getId).orElse(null);
        return new ProfileSummaryResponse(employerProfileId, jobSeekerProfileId, employerApproved);
    }

    @Override
    public JobSeekerProfileLookupResponse getJobSeekerProfileForUser(int userId) {
        JobSeekerProfile profile = jobSeekerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Job seeker profile not found for this user."));
        return new JobSeekerProfileLookupResponse(profile.getId());
    }

    @Override
    public CompanyLookupResponse getCompanyForEmployerUser(int userId) {
        EmployerProfile employerProfile = employerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Employer profile not found for this user."));
        Company company = companyRepository.findByEmployerProfileId(employerProfile.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Company not found for this employer."));
        return new CompanyLookupResponse(company.getId(), company.isApproved());
    }

    @Override
    public CompanySummaryResponse getCompanySummary(int companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found."));
        return new CompanySummaryResponse(company.getId(), company.getName(), company.getLocation(),
                company.getIndustry(), company.getLogoUrl());
    }

    @Override
    public List<Integer> searchCompanyIds(String keyword) {
        return companyRepository.findByNameContainingIgnoreCase(keyword).stream()
                .map(Company::getId)
                .toList();
    }
}
