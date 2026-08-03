package com.careerpilot.notificationservice.service;

import com.careerpilot.notificationservice.dto.internal.AiFeedbackEmailRequest;
import com.careerpilot.notificationservice.dto.internal.ApplicationStatusEmailRequest;
import com.careerpilot.notificationservice.dto.internal.OtpEmailRequest;
import com.careerpilot.notificationservice.dto.internal.RegistrationEmailRequest;

public interface NotificationService {
    void sendRegistrationEmail(RegistrationEmailRequest request);
    void sendOtpEmail(OtpEmailRequest request);
    void sendApplicationStatusEmail(ApplicationStatusEmailRequest request);
    void sendAiFeedbackEmail(AiFeedbackEmailRequest request);
}
