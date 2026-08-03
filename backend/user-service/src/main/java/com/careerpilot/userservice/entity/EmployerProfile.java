package com.careerpilot.userservice.entity;

import com.careerpilot.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * userId is a plain column, not a JPA relationship - the User row it refers
 * to lives in auth-service's own database, a different service entirely.
 * Cross-service references are always soft ids like this one, never a real
 * foreign key; validity is established at write time via the caller
 * (auth-service creates this row through /internal/profiles right after
 * creating the User row).
 */
@Entity
@Table(name = "employer_profiles")
public class EmployerProfile extends BaseEntity {

    @Column(name = "user_id", nullable = false, unique = true)
    private Integer userId;

    @Column(name = "is_approved", nullable = false)
    private boolean isApproved;

    @OneToOne(mappedBy = "employerProfile", fetch = FetchType.LAZY)
    private Company company;

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public boolean isApproved() {
        return isApproved;
    }

    public void setApproved(boolean approved) {
        isApproved = approved;
    }

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }
}
