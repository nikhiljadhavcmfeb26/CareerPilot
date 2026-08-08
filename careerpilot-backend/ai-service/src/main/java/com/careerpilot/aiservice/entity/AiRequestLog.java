package com.careerpilot.aiservice.entity;

import com.careerpilot.aiservice.enums.AiRequestType;
import com.careerpilot.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/** All-new - the .NET app has no AI features at all, so nothing to preserve here. Satisfies "one database per microservice" with something genuinely useful: an audit trail of AI usage, per user, per feature. */
@Entity
@Table(name = "ai_request_logs")
public class AiRequestLog extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AiRequestType type;

    @Column(nullable = false)
    private boolean success;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public AiRequestType getType() {
        return type;
    }

    public void setType(AiRequestType type) {
        this.type = type;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
