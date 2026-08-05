package com.careerpilot.userservice.service;

import com.careerpilot.userservice.dto.JobSeekerProfileDto;
import com.careerpilot.userservice.dto.UpdateJobSeekerProfileRequest;

public interface ProfileService {
    JobSeekerProfileDto getProfile(int userId);
    JobSeekerProfileDto updateProfile(int userId, UpdateJobSeekerProfileRequest request);
}
