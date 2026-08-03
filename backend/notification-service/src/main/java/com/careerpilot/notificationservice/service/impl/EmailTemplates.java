package com.careerpilot.notificationservice.service.impl;

public final class EmailTemplates {

    private EmailTemplates() {
    }

    public record EmailContent(String subject, String body) {
    }

    /**
     * Verbatim port of the subject/body AuthService.cs builds in
     * ForgotPasswordAsync - the one email that actually exists in the
     * original app today. Wording, styling, and the 10-minute claim are all
     * unchanged.
     */
    public static EmailContent otp(String firstName, String otp) {
        String subject = "CareerPilot - Password Reset OTP";
        String body = """
                <h2>Password Reset Request</h2>
                <p>Hello %s,</p>
                <p>Your OTP for password reset is:</p>
                <h1 style='color:#4F46E5;letter-spacing:4px;'>%s</h1>
                <p>This OTP is valid for <strong>10 minutes</strong>.</p>
                <p>If you did not request this, please ignore this email.</p>
                <br/><p>&mdash; CareerPilot Team</p>""".formatted(firstName, otp);
        return new EmailContent(subject, body);
    }

    /** New - the original never sends a registration email. Kept in the same visual style as the OTP template above. */
    public static EmailContent registration(String firstName, String role) {
        String subject = "Welcome to CareerPilot";
        String roleText = "Employer".equalsIgnoreCase(role) ? "start posting jobs and finding great candidates" : "start applying to jobs that match your skills";
        String body = """
                <h2>Welcome to CareerPilot, %s!</h2>
                <p>Your account has been created successfully.</p>
                <p>You're all set to %s.</p>
                <br/><p>&mdash; CareerPilot Team</p>""".formatted(firstName, roleText);
        return new EmailContent(subject, body);
    }

    /** New - the original never sends this either; matches your spec's "Application Status Email" responsibility. */
    public static EmailContent applicationStatus(String firstName, String jobTitle, String companyName, String status) {
        String subject = "Update on your application to " + jobTitle;
        String body = """
                <h2>Application Status Update</h2>
                <p>Hello %s,</p>
                <p>Your application for <strong>%s</strong> at <strong>%s</strong> has been updated to:</p>
                <h3 style='color:#4F46E5;'>%s</h3>
                <p>You can view the full details by logging into your CareerPilot account.</p>
                <br/><p>&mdash; CareerPilot Team</p>""".formatted(firstName, jobTitle, companyName, status);
        return new EmailContent(subject, body);
    }

    /**
     * New. If feedback is null/blank, sends the professional default message
     * your spec calls for ("If the Gemini API fails, send a professional
     * default feedback email") rather than a broken/empty email.
     */
    public static EmailContent aiFeedback(String firstName, String jobTitle, String feedback) {
        String subject = "Feedback on your application to " + jobTitle;
        String feedbackHtml = (feedback == null || feedback.isBlank())
                ? "<p>Thank you for applying. After careful review, we've decided to move forward with other candidates "
                        + "at this time. We encourage you to keep refining your resume and skills, and to apply to future "
                        + "openings that match your experience.</p>"
                : "<p>" + feedback.replace("\n", "<br/>") + "</p>";

        String body = """
                <h2>Application Feedback</h2>
                <p>Hello %s,</p>
                <p>Here's some feedback on your application for <strong>%s</strong>:</p>
                %s
                <br/><p>&mdash; CareerPilot Team</p>""".formatted(firstName, jobTitle, feedbackHtml);
        return new EmailContent(subject, body);
    }
}
