package com.careerpilot.authservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Mirrors the original UserDto shape, including EmployerProfileId /
 * JobSeekerProfileId / IsEmployerApproved. Those three now come from a
 * best-effort Feign call to User Service rather than a same-DB join - see
 * AuthServiceImpl.enrichWithProfile(). They come back null if User Service
 * is unreachable rather than failing the whole login/register/me call;
 * nothing in the current React app actually reads these three fields today.
 */
public class UserDto {

    private Integer id;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private String role;
    private boolean isActive;
    private Integer employerProfileId;
    private Integer jobSeekerProfileId;
    private Boolean isEmployerApproved;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    @JsonProperty("isActive")
    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public Integer getEmployerProfileId() {
        return employerProfileId;
    }

    public void setEmployerProfileId(Integer employerProfileId) {
        this.employerProfileId = employerProfileId;
    }

    public Integer getJobSeekerProfileId() {
        return jobSeekerProfileId;
    }

    public void setJobSeekerProfileId(Integer jobSeekerProfileId) {
        this.jobSeekerProfileId = jobSeekerProfileId;
    }

    public Boolean getEmployerApproved() {
        return isEmployerApproved;
    }

    public void setEmployerApproved(Boolean employerApproved) {
        isEmployerApproved = employerApproved;
    }
}
