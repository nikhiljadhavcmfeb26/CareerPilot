package com.careerpilot.userservice.controller;

import com.careerpilot.userservice.dto.JobSeekerProfileDto;
import com.careerpilot.userservice.dto.UpdateJobSeekerProfileRequest;
import com.careerpilot.userservice.service.ProfileService;
import com.careerpilot.common.dto.ApiResponse;
import com.careerpilot.common.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profiles")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public ApiResponse<JobSeekerProfileDto> get() {
        return ApiResponse.ok(profileService.getProfile(SecurityUtils.currentUserId()));
    }

    @PutMapping
    public ApiResponse<JobSeekerProfileDto> update(@Valid @RequestBody UpdateJobSeekerProfileRequest request) {
        return ApiResponse.ok(profileService.updateProfile(SecurityUtils.currentUserId(), request), "Profile updated");
    }
}
