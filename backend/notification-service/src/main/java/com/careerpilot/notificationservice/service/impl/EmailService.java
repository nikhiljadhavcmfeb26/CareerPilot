package com.careerpilot.notificationservice.service.impl;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.util.Properties;

/**
 * Mirrors CareerPilot.Infrastructure.Services.EmailService field for field:
 * same config keys (renamed to Spring's kebab-case convention), same
 * blank-host-or-username check before attempting anything, same console
 * fallback message shape, same per-send SmtpClient-equivalent construction,
 * same EnableSsl=true -> STARTTLS, same HTML body.
 */
@Component
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Value("${notification.email.smtp-host:}")
    private String smtpHost;

    @Value("${notification.email.smtp-port:587}")
    private int smtpPort;

    @Value("${notification.email.username:}")
    private String username;

    @Value("${notification.email.password:}")
    private String password;

    @Value("${notification.email.from:noreply@careerpilot.com}")
    private String from;

    /**
     * @return true if the email was actually sent, false if it was skipped
     * because SMTP isn't configured. Throws if SMTP is configured but sending
     * still fails, so the caller can log/record the failure.
     */
    public boolean send(String toEmail, String subject, String htmlBody) {
        if (isBlank(smtpHost) || isBlank(username)) {
            log.warn("SMTP not configured. Email to {} | Subject: {}", toEmail, subject);
            System.out.println("=".repeat(60));
            System.out.println("[EMAIL - NOT SENT (SMTP not configured)]");
            System.out.println("To: " + toEmail);
            System.out.println("Subject: " + subject);
            System.out.println("Body: " + htmlBody);
            System.out.println("=".repeat(60));
            return false;
        }

        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(smtpHost);
        mailSender.setPort(smtpPort);
        mailSender.setUsername(username);
        mailSender.setPassword(password);

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false);
            helper.setFrom(from);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            mailSender.send(message);
            log.info("Email sent to {} with subject: {}", toEmail, subject);
            return true;
        } catch (MessagingException ex) {
            log.error("Failed to send email to {}", toEmail, ex);
            throw new EmailSendException("Failed to send email to " + toEmail, ex);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public static class EmailSendException extends RuntimeException {
        public EmailSendException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
