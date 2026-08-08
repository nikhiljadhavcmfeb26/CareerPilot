package com.careerpilot.authservice.service.impl;

import com.careerpilot.authservice.client.UserServiceClient;
import com.careerpilot.authservice.dto.UserDto;
import com.careerpilot.authservice.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    private static final Logger log = LoggerFactory.getLogger(UserMapper.class);

    private final UserServiceClient userServiceClient;

    public UserMapper(UserServiceClient userServiceClient) {
        this.userServiceClient = userServiceClient;
    }

    public UserDto toUserDto(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setPhone(user.getPhone());
        dto.setRole(user.getRole().getName());
        dto.setActive(user.isActive());

        try {
            UserServiceClient.ProfileSummary summary = userServiceClient.getProfileSummary(user.getId());
            if (summary != null) {
                dto.setEmployerProfileId(summary.employerProfileId());
                dto.setJobSeekerProfileId(summary.jobSeekerProfileId());
                dto.setEmployerApproved(summary.employerApproved());
            }
        } catch (Exception ex) {
            log.debug("user-service unavailable while enriching UserDto for user {}, leaving profile fields null", user.getId());
        }

        return dto;
    }
}
