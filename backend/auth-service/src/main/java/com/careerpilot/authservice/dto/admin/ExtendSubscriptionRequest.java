package com.careerpilot.authservice.dto.admin;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public class ExtendSubscriptionRequest {

    /** Bounded so a typo cannot grant a century of Premium. */
    @Min(value = 1, message = "days must be at least 1")
    @Max(value = 730, message = "days must be 730 or fewer")
    private int days = 30;

    public int getDays() {
        return days;
    }

    public void setDays(int days) {
        this.days = days;
    }
}
