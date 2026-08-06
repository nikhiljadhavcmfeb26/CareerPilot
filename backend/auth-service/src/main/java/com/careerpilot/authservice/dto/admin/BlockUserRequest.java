package com.careerpilot.authservice.dto.admin;

import jakarta.validation.constraints.Size;

public class BlockUserRequest {

    @Size(max = 500, message = "Reason must be 500 characters or fewer")
    private String reason;

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
