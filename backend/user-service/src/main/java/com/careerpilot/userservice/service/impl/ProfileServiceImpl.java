package com.careerpilot.userservice.service.impl;

import com.careerpilot.userservice.client.AuthServiceClient;
import com.careerpilot.userservice.dto.JobSeekerProfileDto;
import com.careerpilot.userservice.dto.UpdateJobSeekerProfileRequest;
import com.careerpilot.userservice.entity.JobSeekerProfile;
import com.careerpilot.userservice.repository.JobSeekerProfileRepository;
import com.careerpilot.userservice.service.ProfileService;
import com.careerpilot.common.exception.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileServiceImpl implements ProfileService {

    private static final Logger log = LoggerFactory.getLogger(ProfileServiceImpl.class);

    private final JobSeekerProfileRepository jobSeekerProfileRepository;
    private final AuthServiceClient authServiceClient;

    public ProfileServiceImpl(JobSeekerProfileRepository jobSeekerProfileRepository, AuthServiceClient authServiceClient) {
        this.jobSeekerProfileRepository = jobSeekerProfileRepository;
        this.authServiceClient = authServiceClient;
    }

    @Override
    public JobSeekerProfileDto getProfile(int userId) {
        JobSeekerProfile profile = jobSeekerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException("Only job seekers have profiles."));
        return toDto(profile);
    }

    @Override
    @Transactional
    public JobSeekerProfileDto updateProfile(int userId, UpdateJobSeekerProfileRequest request) {
        JobSeekerProfile profile = jobSeekerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException("Only job seekers can update profile."));

        profile.setHeadline(request.getHeadline());
        profile.setSummary(request.getSummary());
        profile.setSkills(request.getSkills());
        profile.setExperience(request.getExperience());
        profile.setEducation(request.getEducation());
        profile.setLocation(request.getLocation());

        jobSeekerProfileRepository.save(profile);
        return toDto(profile);
    }

    private JobSeekerProfileDto toDto(JobSeekerProfile profile) {
        JobSeekerProfileDto dto = new JobSeekerProfileDto();
        dto.setId(profile.getId());
        dto.setHeadline(profile.getHeadline());
        dto.setSummary(profile.getSummary());
        dto.setSkills(profile.getSkills());
        dto.setExperience(profile.getExperience());
        dto.setEducation(profile.getEducation());
        dto.setLocation(profile.getLocation());

        try {
            AuthServiceClient.UserBasicInfo info = authServiceClient.getUserBasicInfo(profile.getUserId());
            if (info != null) {
                dto.setFirstName(info.firstName());
                dto.setLastName(info.lastName());
                dto.setEmail(info.email());
            }
        } catch (Exception ex) {
            log.warn("auth-service unavailable while hydrating profile name/email for user {}", profile.getUserId());
        }

        return dto;
    }
}
