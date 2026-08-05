package com.careerpilot.userservice.service;

import com.careerpilot.userservice.dto.internal.CompanyLookupResponse;
import com.careerpilot.userservice.dto.internal.CompanySummaryResponse;
import com.careerpilot.userservice.dto.internal.CreateProfileRequest;
import com.careerpilot.userservice.dto.internal.JobSeekerProfileLookupResponse;
import com.careerpilot.userservice.dto.internal.ProfileSummaryResponse;

import java.util.List;

/** All-new - backs the /internal/** endpoints other services call over Feign. None of this exists in the .NET app since it was never split into services. */
public interface InternalProfileService {

    void createProfile(CreateProfileRequest request);

    ProfileSummaryResponse getProfileSummary(int userId);

    JobSeekerProfileLookupResponse getJobSeekerProfileForUser(int userId);

    CompanyLookupResponse getCompanyForEmployerUser(int userId);

    CompanySummaryResponse getCompanySummary(int companyId);

    /** For job-service's keyword search, which (per JobRepository.cs's SearchAsync) also matches on Company.Name - a same-DB join in the original, a Feign lookup now that Company lives here. */
    List<Integer> searchCompanyIds(String keyword);
}
