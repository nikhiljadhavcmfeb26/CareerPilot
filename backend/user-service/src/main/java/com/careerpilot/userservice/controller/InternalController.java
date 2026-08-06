package com.careerpilot.userservice.controller;

import com.careerpilot.userservice.dto.internal.CompanyLookupResponse;
import com.careerpilot.userservice.dto.internal.CompanySummaryResponse;
import com.careerpilot.userservice.dto.internal.CreateProfileRequest;
import com.careerpilot.userservice.dto.internal.JobSeekerProfileLookupResponse;
import com.careerpilot.userservice.dto.internal.ProfileSummaryResponse;
import com.careerpilot.userservice.dto.internal.ResumeDownloadResponse;
import com.careerpilot.userservice.dto.internal.ResumeReferenceResponse;
import com.careerpilot.userservice.repository.CompanyRepository;
import com.careerpilot.userservice.repository.ResumeRepository;
import com.careerpilot.userservice.service.InternalProfileService;
import com.careerpilot.userservice.service.ResumeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Service-to-service only, same story as auth-service's InternalController -
 * no /internal/** route exists in api-gateway.yml, so none of this is
 * reachable from the React client.
 */
@RestController
@RequestMapping("/internal")
public class InternalController {

    private final InternalProfileService internalProfileService;
    private final ResumeService resumeService;
    private final CompanyRepository companyRepository;
    private final ResumeRepository resumeRepository;

    public InternalController(InternalProfileService internalProfileService, ResumeService resumeService,
                              CompanyRepository companyRepository, ResumeRepository resumeRepository) {
        this.internalProfileService = internalProfileService;
        this.resumeService = resumeService;
        this.companyRepository = companyRepository;
        this.resumeRepository = resumeRepository;
    }

    /** Called by auth-service right after it creates a new User row. */
    @PostMapping("/profiles")
    public void createProfile(@RequestBody CreateProfileRequest request) {
        internalProfileService.createProfile(request);
    }

    /** Called by auth-service to enrich UserDto (employerProfileId/jobSeekerProfileId/isEmployerApproved). */
    @GetMapping("/profiles/summary/{userId}")
    public ProfileSummaryResponse getProfileSummary(@PathVariable("userId") Integer userId) {
        return internalProfileService.getProfileSummary(userId);
    }

    /** Called by application-service when a job seeker applies for a job. */
    @GetMapping("/job-seeker-profiles/by-user/{userId}")
    public JobSeekerProfileLookupResponse getJobSeekerProfileForUser(@PathVariable("userId") Integer userId) {
        return internalProfileService.getJobSeekerProfileForUser(userId);
    }

    /** Called by job-service to resolve the calling employer's company + approval status. */
    @GetMapping("/companies/by-employer-user/{userId}")
    public CompanyLookupResponse getCompanyForEmployerUser(@PathVariable("userId") Integer userId) {
        return internalProfileService.getCompanyForEmployerUser(userId);
    }

    /** Called by job-service to embed company name/location/etc into JobDto. */
    @GetMapping("/companies/{companyId}")
    public CompanySummaryResponse getCompanySummary(@PathVariable("companyId") Integer companyId) {
        return internalProfileService.getCompanySummary(companyId);
    }

    /** Called by job-service's keyword search to replicate matching on Company.Name, which used to be a same-DB join. */
    @GetMapping("/companies/search-ids")
    public List<Integer> searchCompanyIds(@RequestParam("keyword") String keyword) {
        return internalProfileService.searchCompanyIds(keyword);
    }

    /** Called by auth-service's admin dashboard aggregation - companies still awaiting approval. */
    @GetMapping("/companies/pending-count")
    public int countPendingCompanies() {
        return companyRepository.countByIsApprovedFalse();
    }

    /** Called by auth-service's job seeker dashboard aggregation to drive the "upload your resume" prompt. */
    @GetMapping("/resumes/count-by-profile/{jobSeekerProfileId}")
    public int countResumesByProfile(@PathVariable("jobSeekerProfileId") Integer jobSeekerProfileId) {
        return resumeRepository.countByJobSeekerProfileId(jobSeekerProfileId);
    }

    /** Called by application-service for the employer-facing resume download, after IT has already verified job ownership. */
    @GetMapping("/resumes/{id}/download")
    public ResumeDownloadResponse downloadResume(@PathVariable("id") Integer id) {
        return resumeService.downloadForInternalCaller(id);
    }

    /**
     * Called by application-service when a job seeker applies, to validate the
     * resume they picked actually belongs to them (or to fall back to their
     * default). Returns identity only, never file content.
     */
    @GetMapping("/resumes/resolve-for-application/{jobSeekerProfileId}")
    public ResumeReferenceResponse resolveResumeForApplication(
            @PathVariable("jobSeekerProfileId") Integer jobSeekerProfileId,
            @RequestParam(value = "resumeId", required = false) Integer resumeId) {
        return resumeService.resolveForApplication(jobSeekerProfileId, resumeId);
    }

    /** Called by ai-service, which operates on the calling candidate's default resume rather than an explicit resumeId. */
    @GetMapping("/resumes/default-by-profile/{jobSeekerProfileId}")
    public ResumeDownloadResponse downloadDefaultResume(@PathVariable("jobSeekerProfileId") Integer jobSeekerProfileId) {
        return resumeService.downloadDefaultForProfile(jobSeekerProfileId);
    }
}
