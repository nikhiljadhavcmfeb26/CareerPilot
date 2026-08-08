package com.careerpilot.userservice.entity;

import com.careerpilot.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "companies")
public class Company extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(length = 2000)
    private String description;

    private String website;

    private String industry;

    private String location;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "is_approved", nullable = false)
    private boolean isApproved;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employer_profile_id", nullable = false, unique = true)
    private EmployerProfile employerProfile;

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

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public boolean isApproved() {
        return isApproved;
    }

    public void setApproved(boolean approved) {
        isApproved = approved;
    }

    public EmployerProfile getEmployerProfile() {
        return employerProfile;
    }

    public void setEmployerProfile(EmployerProfile employerProfile) {
        this.employerProfile = employerProfile;
    }
}
