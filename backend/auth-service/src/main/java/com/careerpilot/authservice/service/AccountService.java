package com.careerpilot.authservice.service;

import com.careerpilot.authservice.dto.ChangePasswordRequest;
import com.careerpilot.authservice.dto.UpdateUserRequest;
import com.careerpilot.authservice.dto.UserDto;
import com.careerpilot.authservice.dto.UserListDto;

import java.util.List;

/** Ported from the .NET UserService - it's account/role management, so it lives in Auth Service. See the routing correction note in the README. */
public interface AccountService {

    UserDto getCurrentUser(int userId);

    UserDto updateProfile(int userId, UpdateUserRequest request);

    /**
     * Changes the caller's own password after verifying the current one.
     * Lives here rather than in AuthService because it operates on an already
     * authenticated account, exactly like updateProfile - AuthService owns the
     * unauthenticated flows (register/login/forgot/reset).
     */
    void changePassword(int userId, ChangePasswordRequest request);

    List<UserListDto> getAllUsers();

    void deactivateUser(int userId);

    void activateUser(int userId);
}
