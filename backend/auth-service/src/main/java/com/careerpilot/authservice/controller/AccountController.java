package com.careerpilot.authservice.controller;

import com.careerpilot.authservice.dto.ChangePasswordRequest;
import com.careerpilot.authservice.dto.UpdateUserRequest;
import com.careerpilot.authservice.dto.UserDto;
import com.careerpilot.authservice.dto.UserListDto;
import com.careerpilot.authservice.service.AccountService;
import com.careerpilot.common.dto.ApiResponse;
import com.careerpilot.common.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/me")
    public ApiResponse<UserDto> getMe() {
        return ApiResponse.ok(accountService.getCurrentUser(SecurityUtils.currentUserId()));
    }

    @PutMapping("/me")
    public ApiResponse<UserDto> updateMe(@Valid @RequestBody UpdateUserRequest request) {
        return ApiResponse.ok(accountService.updateProfile(SecurityUtils.currentUserId(), request), "Profile updated");
    }

    /**
     * Settings -> Security -> Change Password. Scoped to the caller's own
     * account via SecurityUtils, so there is no user id in the path and one
     * user can never change another's password.
     */
    @PutMapping("/me/password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        accountService.changePassword(SecurityUtils.currentUserId(), request);
        return ApiResponse.success("Password changed successfully");
    }

    @GetMapping
    public ApiResponse<List<UserListDto>> getAll() {
        return ApiResponse.ok(accountService.getAllUsers());
    }

    @PutMapping("/{id}/deactivate")
    public ApiResponse<Void> deactivate(@PathVariable int id) {
        accountService.deactivateUser(id);
        return ApiResponse.success("User deactivated");
    }

    @PutMapping("/{id}/activate")
    public ApiResponse<Void> activate(@PathVariable int id) {
        accountService.activateUser(id);
        return ApiResponse.success("User activated");
    }
}
