package com.careerpilot.notificationservice.controller;

import com.careerpilot.notificationservice.dto.internal.AiFeedbackEmailRequest;
import com.careerpilot.notificationservice.dto.internal.ApplicationStatusEmailRequest;
import com.careerpilot.notificationservice.dto.internal.OtpEmailRequest;
import com.careerpilot.notificationservice.dto.internal.RegistrationEmailRequest;
import com.careerpilot.notificationservice.service.NotificationService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Service-to-service only, same story as every other service's
 * InternalController - no /internal/** route exists in api-gateway.yml, so
 * none of this is reachable from the React client. Every call site on the
 * calling side (auth-service, application-service, ai-service) wraps these
 * in a try/catch and treats a failure as non-fatal to the surrounding
 * request - see the client interfaces in those services.
 */
@RestController
@RequestMapping("/internal/notifications")
public class InternalController {

    private final NotificationService notificationService;

    public InternalController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /** Called by auth-service right after a new User row is created. */
    @PostMapping("/registration")
    public void registration(@RequestBody RegistrationEmailRequest request) {
        notificationService.sendRegistrationEmail(request);
    }

    /** Called by auth-service from ForgotPasswordAsync - the one email that exists in the original app today. */
    @PostMapping("/otp")
    public void otp(@RequestBody OtpEmailRequest request) {
        notificationService.sendOtpEmail(request);
    }

    /** Called by application-service after an employer updates an application's status. */
    @PostMapping("/application-status")
    public void applicationStatus(@RequestBody ApplicationStatusEmailRequest request) {
        notificationService.sendApplicationStatusEmail(request);
    }

    /** Called by ai-service (not built yet) after generating rejection feedback. */
    @PostMapping("/ai-feedback")
    public void aiFeedback(@RequestBody AiFeedbackEmailRequest request) {
        notificationService.sendAiFeedbackEmail(request);
    }
}
