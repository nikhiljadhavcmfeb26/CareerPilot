package com.careerpilot.authservice.dto;

public class CreateOrderRequest {

    /** Defaults to PREMIUM - it's currently the only paid plan. */
    private String planName = "PREMIUM";

    public String getPlanName() {
        return planName;
    }

    public void setPlanName(String planName) {
        this.planName = planName;
    }
}
