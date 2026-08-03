package com.careerpilot.notificationservice.entity;

import com.careerpilot.common.entity.BaseEntity;
import com.careerpilot.notificationservice.enums.NotificationStatus;
import com.careerpilot.notificationservice.enums.NotificationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * All-new - the .NET app has no persistence around emails at all (EmailService
 * just sends and returns). Notification Service otherwise has no state of its
 * own to store, so this audit log is what satisfies "one database per
 * microservice" for it - a reasonable, genuinely useful use of that
 * requirement rather than an empty schema.
 */
@Entity
@Table(name = "notification_logs")
public class NotificationLog extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Column(name = "recipient_email", nullable = false)
    private String recipientEmail;

    @Column(nullable = false)
    private String subject;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationStatus status;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    public NotificationType getType() {
        return type;
    }

    public void setType(NotificationType type) {
        this.type = type;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public void setRecipientEmail(String recipientEmail) {
        this.recipientEmail = recipientEmail;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public void setStatus(NotificationStatus status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
