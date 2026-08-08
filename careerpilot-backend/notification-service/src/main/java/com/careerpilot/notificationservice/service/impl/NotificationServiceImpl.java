package com.careerpilot.notificationservice.service.impl;

import com.careerpilot.notificationservice.dto.internal.AiFeedbackEmailRequest;
import com.careerpilot.notificationservice.dto.internal.ApplicationStatusEmailRequest;
import com.careerpilot.notificationservice.dto.internal.OtpEmailRequest;
import com.careerpilot.notificationservice.dto.internal.RegistrationEmailRequest;
import com.careerpilot.notificationservice.entity.NotificationLog;
import com.careerpilot.notificationservice.enums.NotificationStatus;
import com.careerpilot.notificationservice.enums.NotificationType;
import com.careerpilot.notificationservice.repository.NotificationLogRepository;
import com.careerpilot.notificationservice.service.NotificationService;
import com.careerpilot.notificationservice.service.impl.EmailTemplates.EmailContent;
import org.springframework.stereotype.Service;

@Service
public class NotificationServiceImpl implements NotificationService {

    private final EmailService emailService;
    private final NotificationLogRepository notificationLogRepository;

    public NotificationServiceImpl(EmailService emailService, NotificationLogRepository notificationLogRepository) {
        this.emailService = emailService;
        this.notificationLogRepository = notificationLogRepository;
    }

    @Override
    public void sendRegistrationEmail(RegistrationEmailRequest request) {
        EmailContent content = EmailTemplates.registration(request.firstName(), request.role());
        dispatch(NotificationType.REGISTRATION, request.email(), content);
    }

    @Override
    public void sendOtpEmail(OtpEmailRequest request) {
        EmailContent content = EmailTemplates.otp(request.firstName(), request.otp());
        dispatch(NotificationType.OTP, request.email(), content);
    }

    @Override
    public void sendApplicationStatusEmail(ApplicationStatusEmailRequest request) {
        EmailContent content = EmailTemplates.applicationStatus(request.firstName(), request.jobTitle(),
                request.companyName(), request.status());
        dispatch(NotificationType.APPLICATION_STATUS, request.email(), content);
    }

    @Override
    public void sendAiFeedbackEmail(AiFeedbackEmailRequest request) {
        EmailContent content = EmailTemplates.aiFeedback(request.firstName(), request.jobTitle(), request.feedback());
        dispatch(NotificationType.AI_FEEDBACK, request.email(), content);
    }

    private void dispatch(NotificationType type, String recipientEmail, EmailContent content) {
        NotificationLog logEntry = new NotificationLog();
        logEntry.setType(type);
        logEntry.setRecipientEmail(recipientEmail);
        logEntry.setSubject(content.subject());

        try {
            boolean sent = emailService.send(recipientEmail, content.subject(), content.body());
            logEntry.setStatus(sent ? NotificationStatus.SENT : NotificationStatus.SKIPPED_SMTP_NOT_CONFIGURED);
            notificationLogRepository.save(logEntry);
        } catch (EmailService.EmailSendException ex) {
            logEntry.setStatus(NotificationStatus.FAILED);
            logEntry.setErrorMessage(ex.getMessage());
            notificationLogRepository.save(logEntry);
            throw ex;
        }
    }
}
