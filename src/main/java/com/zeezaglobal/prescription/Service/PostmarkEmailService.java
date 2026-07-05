package com.zeezaglobal.prescription.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class PostmarkEmailService {

    private static final Logger logger = LoggerFactory.getLogger(PostmarkEmailService.class);
    private static final String POSTMARK_API_URL = "https://api.postmarkapp.com/email";

    @Value("${postmark.api.token:b6ce4c63-1434-43e6-9d9a-188b2510d65b}")
    private String apiToken;

    @Value("${postmark.from.email:info@indigorx.me}")
    private String fromEmail;

    @Value("${postmark.from.name:IndigoRx}")
    private String fromName;

    @Value("${app.frontend.url:https://indigorx.me}")
    private String frontendUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public PostmarkEmailService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    // ==================== EXISTING EMAIL METHODS ====================

    public boolean sendOtpEmail(String toEmail, String otp, String recipientName) {
        String subject = "Verify Your IndigoRx Account";
        String htmlBody = buildOtpEmailHtml(otp, recipientName);
        String textBody = buildOtpEmailText(otp, recipientName);
        return sendEmail(toEmail, subject, htmlBody, textBody);
    }

    public boolean sendPasswordResetEmail(String toEmail, String resetToken, String recipientName) {
        String subject = "Reset Your IndigoRx Password";
        String resetLink = frontendUrl + "/reset-password?token=" + resetToken;
        String htmlBody = buildPasswordResetEmailHtml(resetLink, recipientName);
        String textBody = buildPasswordResetEmailText(resetLink, recipientName);
        return sendEmail(toEmail, subject, htmlBody, textBody);
    }

    public boolean sendWelcomeEmail(String toEmail, String doctorName) {
        String subject = "Welcome to IndigoRx!";
        String htmlBody = buildWelcomeEmailHtml(doctorName);
        String textBody = buildWelcomeEmailText(doctorName);
        return sendEmail(toEmail, subject, htmlBody, textBody);
    }

    public boolean sendPasswordChangedEmail(String toEmail, String recipientName) {
        String subject = "Your IndigoRx Password Has Been Changed";
        String htmlBody = buildPasswordChangedEmailHtml(recipientName);
        String textBody = buildPasswordChangedEmailText(recipientName);
        return sendEmail(toEmail, subject, htmlBody, textBody);
    }

    // ==================== CORE EMAIL SENDING ====================

    private boolean sendEmail(String toEmail, String subject, String htmlBody, String textBody) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Postmark-Server-Token", apiToken);
            headers.set("Accept", "application/json");

            Map<String, Object> emailData = new HashMap<>();
            emailData.put("From", fromName + " <" + fromEmail + ">");
            emailData.put("To", toEmail);
            emailData.put("Subject", subject);
            emailData.put("HtmlBody", htmlBody);
            emailData.put("TextBody", textBody);
            emailData.put("MessageStream", "outbound");

            String jsonBody = objectMapper.writeValueAsString(emailData);
            HttpEntity<String> request = new HttpEntity<>(jsonBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    POSTMARK_API_URL, HttpMethod.POST, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                logger.info("Email sent successfully to: {}", toEmail);
                return true;
            } else {
                logger.error("Failed to send email. Status: {}", response.getStatusCode());
                return false;
            }
        } catch (Exception e) {
            logger.error("Error sending email to {}: {}", toEmail, e.getMessage(), e);
            return false;
        }
    }

    // ==================== HELPER ====================

    private String safeName(String name, String defaultName) {
        return (name != null && !name.isEmpty()) ? name : defaultName;
    }

    // ==================== EXISTING TEMPLATES ====================

    private String buildOtpEmailHtml(String otp, String recipientName) {
        String name = safeName(recipientName, "Doctor");
        return String.format("""
            <!DOCTYPE html><html><body style="margin:0;padding:0;font-family:sans-serif;background:#f4f7fa;">
            <table style="width:100%%;"><tr><td align="center" style="padding:40px 0;">
            <table style="width:600px;background:#fff;border-radius:12px;box-shadow:0 4px 6px rgba(0,0,0,0.1);">
            <tr><td style="padding:40px;text-align:center;background:linear-gradient(135deg,#059669,#047857);border-radius:12px 12px 0 0;">
            <h1 style="margin:0;color:#fff;font-size:28px;">IndigoRx</h1></td></tr>
            <tr><td style="padding:40px;">
            <h2 style="color:#1f2937;">Verify Your Email</h2>
            <p style="color:#4b5563;">Hello %s,</p>
            <p style="color:#4b5563;">Use this verification code:</p>
            <div style="text-align:center;margin:30px 0;">
            <div style="display:inline-block;background:#f0fdf4;border:2px solid #059669;border-radius:12px;padding:20px 40px;">
            <span style="font-size:36px;font-weight:700;color:#059669;letter-spacing:8px;">%s</span>
            </div></div>
            <p style="color:#6b7280;font-size:14px;">This code expires in 10 minutes.</p>
            </td></tr>
            <tr><td style="padding:30px;background:#f9fafb;border-radius:0 0 12px 12px;text-align:center;">
            <p style="color:#9ca3af;font-size:12px;">© 2024 IndigoRx</p></td></tr>
            </table></td></tr></table></body></html>
            """, name, otp);
    }

    private String buildOtpEmailText(String otp, String recipientName) {
        String name = safeName(recipientName, "Doctor");
        return String.format("IndigoRx - Verify Your Email\n\nHello %s,\n\nYour verification code: %s\n\nExpires in 10 minutes.", name, otp);
    }

    private String buildPasswordResetEmailHtml(String resetLink, String recipientName) {
        String name = safeName(recipientName, "User");
        return String.format("""
            <!DOCTYPE html><html><body style="margin:0;padding:0;font-family:sans-serif;background:#f4f7fa;">
            <table style="width:100%%;"><tr><td align="center" style="padding:40px 0;">
            <table style="width:600px;background:#fff;border-radius:12px;">
            <tr><td style="padding:40px;text-align:center;background:linear-gradient(135deg,#059669,#047857);border-radius:12px 12px 0 0;">
            <h1 style="margin:0;color:#fff;">IndigoRx</h1></td></tr>
            <tr><td style="padding:40px;">
            <h2 style="color:#1f2937;">Reset Your Password</h2>
            <p style="color:#4b5563;">Hello %s,</p>
            <p style="color:#4b5563;">Click below to reset your password:</p>
            <div style="text-align:center;margin:30px 0;">
            <a href="%s" style="display:inline-block;background:#059669;color:#fff;padding:16px 40px;border-radius:8px;text-decoration:none;font-weight:600;">Reset Password</a>
            </div>
            <p style="color:#6b7280;font-size:14px;">Link expires in 1 hour.</p>
            </td></tr></table></td></tr></table></body></html>
            """, name, resetLink);
    }

    private String buildPasswordResetEmailText(String resetLink, String recipientName) {
        String name = safeName(recipientName, "User");
        return String.format("IndigoRx - Reset Password\n\nHello %s,\n\nReset link: %s\n\nExpires in 1 hour.", name, resetLink);
    }

    private String buildWelcomeEmailHtml(String doctorName) {
        String name = safeName(doctorName, "Doctor");
        return String.format("""
            <!DOCTYPE html><html><body style="margin:0;padding:0;font-family:sans-serif;background:#f4f7fa;">
            <table style="width:100%%;"><tr><td align="center" style="padding:40px 0;">
            <table style="width:600px;background:#fff;border-radius:12px;">
            <tr><td style="padding:40px;text-align:center;background:linear-gradient(135deg,#059669,#047857);border-radius:12px 12px 0 0;">
            <h1 style="margin:0;color:#fff;">🎉 Welcome to IndigoRx!</h1></td></tr>
            <tr><td style="padding:40px;">
            <h2 style="color:#1f2937;">Hello Dr. %s!</h2>
            <p style="color:#4b5563;">Your email has been verified. Welcome to IndigoRx!</p>
            <div style="text-align:center;margin:30px 0;">
            <a href="%s/dashboard" style="display:inline-block;background:#059669;color:#fff;padding:16px 40px;border-radius:8px;text-decoration:none;">Go to Dashboard</a>
            </div></td></tr></table></td></tr></table></body></html>
            """, name, frontendUrl);
    }

    private String buildWelcomeEmailText(String doctorName) {
        String name = safeName(doctorName, "Doctor");
        return String.format("Welcome to IndigoRx!\n\nHello Dr. %s,\n\nYour email has been verified.\n\nDashboard: %s/dashboard", name, frontendUrl);
    }

    private String buildPasswordChangedEmailHtml(String recipientName) {
        String name = safeName(recipientName, "User");
        return String.format("""
            <!DOCTYPE html><html><body style="margin:0;padding:0;font-family:sans-serif;background:#f4f7fa;">
            <table style="width:100%%;"><tr><td align="center" style="padding:40px 0;">
            <table style="width:600px;background:#fff;border-radius:12px;">
            <tr><td style="padding:40px;text-align:center;background:linear-gradient(135deg,#059669,#047857);border-radius:12px 12px 0 0;">
            <h1 style="margin:0;color:#fff;">IndigoRx</h1></td></tr>
            <tr><td style="padding:40px;text-align:center;">
            <div style="font-size:40px;margin-bottom:20px;">✓</div>
            <h2 style="color:#1f2937;">Password Changed</h2>
            <p style="color:#4b5563;">Hello %s, your password has been changed successfully.</p>
            <p style="color:#ef4444;font-size:14px;">Didn't make this change? Contact support@indigorx.me immediately.</p>
            </td></tr></table></td></tr></table></body></html>
            """, name);
    }

    private String buildPasswordChangedEmailText(String recipientName) {
        String name = safeName(recipientName, "User");
        return String.format("IndigoRx - Password Changed\n\nHello %s,\n\nYour password has been changed.\n\nDidn't do this? Contact support@indigorx.me", name);
    }
}