package com.careerpilot.userservice.dto;

import jakarta.validation.constraints.NotBlank;

public class CreateCompanyRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String description;

    private String website;

    @NotBlank
    private String industry;

    @NotBlank
    private String location;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getIndustry() {
        return industry;
    }

    public void setIndustry(String industry) {
        this.industry = industry;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }
}
