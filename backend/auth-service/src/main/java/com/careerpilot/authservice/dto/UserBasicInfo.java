package com.careerpilot.authservice.dto;

/**
 * Returned by the internal (non-gateway-routed) lookup endpoint other
 * services call via Feign when they need a user's name/email/role but don't
 * own the User table themselves - e.g. Application Service hydrating
 * ApplicantName/ApplicantEmail on the applicant list, which used to be a
 * same-DB join against Users in the monolith.
 */
public class UserBasicInfo {

    private Integer id;
    private String email;
    private String firstName;
    private String lastName;
    private String role;
    private boolean active;

    public UserBasicInfo() {
    }

    public UserBasicInfo(Integer id, String email, String firstName, String lastName, String role, boolean active) {
        this.id = id;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
        this.active = active;
    }

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

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
