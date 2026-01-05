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

    @Value("${postmark.from.email:noreply@indigorx.me}")
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

    // ==================== SUBSCRIPTION EMAIL METHODS ====================

    public boolean sendSubscriptionConfirmationEmail(String toEmail, String recipientName) {
        String subject = "Welcome to IndigoRx Pro! 🎉";
        String htmlBody = buildSubscriptionConfirmationHtml(recipientName);
        String textBody = buildSubscriptionConfirmationText(recipientName);
        return sendEmail(toEmail, subject, htmlBody, textBody);
    }

    public boolean sendRenewalSuccessEmail(String toEmail, String recipientName, String amount) {
        String subject = "Your IndigoRx Subscription Has Been Renewed";
        String htmlBody = buildRenewalSuccessHtml(recipientName, amount);
        String textBody = buildRenewalSuccessText(recipientName, amount);
        return sendEmail(toEmail, subject, htmlBody, textBody);
    }

    public boolean sendPaymentFailedEmail(String toEmail, String recipientName, int attemptCount, boolean willRetry) {
        String subject = willRetry ? "Action Required: Payment Failed" : "Urgent: Final Payment Attempt Failed";
        String htmlBody = buildPaymentFailedHtml(recipientName, attemptCount, willRetry);
        String textBody = buildPaymentFailedText(recipientName, attemptCount, willRetry);
        return sendEmail(toEmail, subject, htmlBody, textBody);
    }

    public boolean sendPaymentPastDueEmail(String toEmail, String recipientName) {
        String subject = "Action Required: Your IndigoRx Payment is Past Due";
        String htmlBody = buildPaymentPastDueHtml(recipientName);
        String textBody = buildPaymentPastDueText(recipientName);
        return sendEmail(toEmail, subject, htmlBody, textBody);
    }

    public boolean sendCancellationScheduledEmail(String toEmail, String recipientName, String cancelDate) {
        String subject = "Your IndigoRx Subscription Cancellation Confirmed";
        String htmlBody = buildCancellationScheduledHtml(recipientName, cancelDate);
        String textBody = buildCancellationScheduledText(recipientName, cancelDate);
        return sendEmail(toEmail, subject, htmlBody, textBody);
    }

    public boolean sendSubscriptionEndedEmail(String toEmail, String recipientName) {
        String subject = "Your IndigoRx Subscription Has Ended";
        String htmlBody = buildSubscriptionEndedHtml(recipientName);
        String textBody = buildSubscriptionEndedText(recipientName);
        return sendEmail(toEmail, subject, htmlBody, textBody);
    }

    public boolean sendSubscriptionSuspendedEmail(String toEmail, String recipientName) {
        String subject = "Your IndigoRx Subscription Has Been Suspended";
        String htmlBody = buildSubscriptionSuspendedHtml(recipientName);
        String textBody = buildSubscriptionSuspendedText(recipientName);
        return sendEmail(toEmail, subject, htmlBody, textBody);
    }

    public boolean sendTrialEndingEmail(String toEmail, String recipientName, String endDate) {
        String subject = "Your IndigoRx Trial Ends Soon - Subscribe Now";
        String htmlBody = buildTrialEndingHtml(recipientName, endDate);
        String textBody = buildTrialEndingText(recipientName, endDate);
        return sendEmail(toEmail, subject, htmlBody, textBody);
    }

    public boolean sendSubscriptionPausedEmail(String toEmail, String recipientName) {
        String subject = "Your IndigoRx Subscription Has Been Paused";
        String htmlBody = buildSubscriptionPausedHtml(recipientName);
        String textBody = buildSubscriptionPausedText(recipientName);
        return sendEmail(toEmail, subject, htmlBody, textBody);
    }

    public boolean sendSubscriptionResumedEmail(String toEmail, String recipientName) {
        String subject = "Your IndigoRx Subscription Has Been Resumed";
        String htmlBody = buildSubscriptionResumedHtml(recipientName);
        String textBody = buildSubscriptionResumedText(recipientName);
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

    // ==================== SUBSCRIPTION TEMPLATES ====================

    private String buildSubscriptionConfirmationHtml(String recipientName) {
        String name = safeName(recipientName, "Doctor");
        return String.format("""
            <!DOCTYPE html><html><body style="margin:0;padding:0;font-family:sans-serif;background:#f4f7fa;">
            <table style="width:100%%;"><tr><td align="center" style="padding:40px 0;">
            <table style="width:600px;background:#fff;border-radius:12px;">
            <tr><td style="padding:40px;text-align:center;background:linear-gradient(135deg,#059669,#047857);border-radius:12px 12px 0 0;">
            <h1 style="margin:0;color:#fff;">🎉 Welcome to IndigoRx Pro!</h1></td></tr>
            <tr><td style="padding:40px;">
            <h2 style="color:#1f2937;">Thank You for Subscribing!</h2>
            <p style="color:#4b5563;">Hello Dr. %s,</p>
            <p style="color:#4b5563;">Your IndigoRx Pro Annual subscription is now active with unlimited prescriptions, digital signatures, PDF exports, and priority support.</p>
            <div style="text-align:center;margin:30px 0;">
            <a href="%s/dashboard" style="display:inline-block;background:#059669;color:#fff;padding:16px 40px;border-radius:8px;text-decoration:none;">Go to Dashboard</a>
            </div></td></tr></table></td></tr></table></body></html>
            """, name, frontendUrl);
    }

    private String buildSubscriptionConfirmationText(String recipientName) {
        String name = safeName(recipientName, "Doctor");
        return String.format("Welcome to IndigoRx Pro!\n\nHello Dr. %s,\n\nYour subscription is active.\n\nDashboard: %s/dashboard", name, frontendUrl);
    }

    private String buildRenewalSuccessHtml(String recipientName, String amount) {
        String name = safeName(recipientName, "Doctor");
        return String.format("""
            <!DOCTYPE html><html><body style="margin:0;padding:0;font-family:sans-serif;background:#f4f7fa;">
            <table style="width:100%%;"><tr><td align="center" style="padding:40px 0;">
            <table style="width:600px;background:#fff;border-radius:12px;">
            <tr><td style="padding:40px;text-align:center;background:linear-gradient(135deg,#059669,#047857);border-radius:12px 12px 0 0;">
            <h1 style="margin:0;color:#fff;">Subscription Renewed</h1></td></tr>
            <tr><td style="padding:40px;text-align:center;">
            <div style="font-size:40px;margin-bottom:20px;">✓</div>
            <h2 style="color:#1f2937;">Renewed Successfully</h2>
            <p style="color:#4b5563;">Hello Dr. %s,</p>
            <p style="color:#4b5563;">Your subscription has been renewed.</p>
            <div style="background:#f0fdf4;border-radius:8px;padding:20px;margin:20px 0;">
            <p style="color:#6b7280;margin:0;">Amount Charged</p>
            <p style="color:#059669;font-size:28px;font-weight:700;margin:5px 0;">%s</p>
            </div></td></tr></table></td></tr></table></body></html>
            """, name, amount);
    }

    private String buildRenewalSuccessText(String recipientName, String amount) {
        String name = safeName(recipientName, "Doctor");
        return String.format("IndigoRx - Subscription Renewed\n\nHello Dr. %s,\n\nAmount charged: %s for 1 year.", name, amount);
    }

    private String buildPaymentFailedHtml(String recipientName, int attemptCount, boolean willRetry) {
        String name = safeName(recipientName, "Doctor");
        String color = willRetry ? "#f59e0b" : "#ef4444";
        String message = willRetry ? "We'll retry automatically. Please update your payment method." : "This was the final attempt. Update payment to avoid suspension.";
        return String.format("""
            <!DOCTYPE html><html><body style="margin:0;padding:0;font-family:sans-serif;background:#f4f7fa;">
            <table style="width:100%%;"><tr><td align="center" style="padding:40px 0;">
            <table style="width:600px;background:#fff;border-radius:12px;">
            <tr><td style="padding:40px;text-align:center;background:%s;border-radius:12px 12px 0 0;">
            <h1 style="margin:0;color:#fff;">⚠️ Payment Failed</h1></td></tr>
            <tr><td style="padding:40px;">
            <h2 style="color:#1f2937;">Action Required</h2>
            <p style="color:#4b5563;">Hello Dr. %s,</p>
            <p style="color:#4b5563;">Payment attempt %d failed. %s</p>
            <div style="text-align:center;margin:30px 0;">
            <a href="%s/settings?tab=subscription" style="display:inline-block;background:#059669;color:#fff;padding:16px 40px;border-radius:8px;text-decoration:none;">Update Payment</a>
            </div></td></tr></table></td></tr></table></body></html>
            """, color, name, attemptCount, message, frontendUrl);
    }

    private String buildPaymentFailedText(String recipientName, int attemptCount, boolean willRetry) {
        String name = safeName(recipientName, "Doctor");
        return String.format("IndigoRx - Payment Failed\n\nHello Dr. %s,\n\nAttempt %d failed. Update payment: %s/settings?tab=subscription", name, attemptCount, frontendUrl);
    }

    private String buildPaymentPastDueHtml(String recipientName) {
        String name = safeName(recipientName, "Doctor");
        return String.format("""
            <!DOCTYPE html><html><body style="margin:0;padding:0;font-family:sans-serif;background:#f4f7fa;">
            <table style="width:100%%;"><tr><td align="center" style="padding:40px 0;">
            <table style="width:600px;background:#fff;border-radius:12px;">
            <tr><td style="padding:40px;text-align:center;background:#f59e0b;border-radius:12px 12px 0 0;">
            <h1 style="margin:0;color:#fff;">⚠️ Payment Past Due</h1></td></tr>
            <tr><td style="padding:40px;">
            <h2 style="color:#1f2937;">Update Payment Method</h2>
            <p style="color:#4b5563;">Hello Dr. %s,</p>
            <p style="color:#4b5563;">Your payment is past due. Update to avoid service interruption.</p>
            <div style="text-align:center;margin:30px 0;">
            <a href="%s/settings?tab=subscription" style="display:inline-block;background:#059669;color:#fff;padding:16px 40px;border-radius:8px;text-decoration:none;">Update Payment</a>
            </div></td></tr></table></td></tr></table></body></html>
            """, name, frontendUrl);
    }

    private String buildPaymentPastDueText(String recipientName) {
        String name = safeName(recipientName, "Doctor");
        return String.format("IndigoRx - Payment Past Due\n\nHello Dr. %s,\n\nUpdate payment: %s/settings?tab=subscription", name, frontendUrl);
    }

    private String buildCancellationScheduledHtml(String recipientName, String cancelDate) {
        String name = safeName(recipientName, "Doctor");
        return String.format("""
            <!DOCTYPE html><html><body style="margin:0;padding:0;font-family:sans-serif;background:#f4f7fa;">
            <table style="width:100%%;"><tr><td align="center" style="padding:40px 0;">
            <table style="width:600px;background:#fff;border-radius:12px;">
            <tr><td style="padding:40px;text-align:center;background:#6b7280;border-radius:12px 12px 0 0;">
            <h1 style="margin:0;color:#fff;">Cancellation Confirmed</h1></td></tr>
            <tr><td style="padding:40px;">
            <h2 style="color:#1f2937;">We're Sorry to See You Go</h2>
            <p style="color:#4b5563;">Hello Dr. %s,</p>
            <p style="color:#4b5563;">Your cancellation is scheduled. You'll have access until:</p>
            <div style="background:#f3f4f6;border-radius:8px;padding:20px;margin:20px 0;text-align:center;">
            <p style="color:#1f2937;font-size:24px;font-weight:700;margin:0;">%s</p>
            </div>
            <div style="text-align:center;margin:30px 0;">
            <a href="%s/subscription" style="display:inline-block;background:#059669;color:#fff;padding:16px 40px;border-radius:8px;text-decoration:none;">Reactivate</a>
            </div></td></tr></table></td></tr></table></body></html>
            """, name, cancelDate, frontendUrl);
    }

    private String buildCancellationScheduledText(String recipientName, String cancelDate) {
        String name = safeName(recipientName, "Doctor");
        return String.format("IndigoRx - Cancellation Confirmed\n\nHello Dr. %s,\n\nAccess until: %s\n\nReactivate: %s/subscription", name, cancelDate, frontendUrl);
    }

    private String buildSubscriptionEndedHtml(String recipientName) {
        String name = safeName(recipientName, "Doctor");
        return String.format("""
            <!DOCTYPE html><html><body style="margin:0;padding:0;font-family:sans-serif;background:#f4f7fa;">
            <table style="width:100%%;"><tr><td align="center" style="padding:40px 0;">
            <table style="width:600px;background:#fff;border-radius:12px;">
            <tr><td style="padding:40px;text-align:center;background:#6b7280;border-radius:12px 12px 0 0;">
            <h1 style="margin:0;color:#fff;">Subscription Ended</h1></td></tr>
            <tr><td style="padding:40px;">
            <h2 style="color:#1f2937;">Your Access Has Ended</h2>
            <p style="color:#4b5563;">Hello Dr. %s,</p>
            <p style="color:#4b5563;">Your subscription has ended. Your data is safe. Resubscribe anytime.</p>
            <div style="text-align:center;margin:30px 0;">
            <a href="%s/subscription" style="display:inline-block;background:#059669;color:#fff;padding:16px 40px;border-radius:8px;text-decoration:none;">Resubscribe</a>
            </div></td></tr></table></td></tr></table></body></html>
            """, name, frontendUrl);
    }

    private String buildSubscriptionEndedText(String recipientName) {
        String name = safeName(recipientName, "Doctor");
        return String.format("IndigoRx - Subscription Ended\n\nHello Dr. %s,\n\nResubscribe: %s/subscription", name, frontendUrl);
    }

    private String buildSubscriptionSuspendedHtml(String recipientName) {
        String name = safeName(recipientName, "Doctor");
        return String.format("""
            <!DOCTYPE html><html><body style="margin:0;padding:0;font-family:sans-serif;background:#f4f7fa;">
            <table style="width:100%%;"><tr><td align="center" style="padding:40px 0;">
            <table style="width:600px;background:#fff;border-radius:12px;">
            <tr><td style="padding:40px;text-align:center;background:#ef4444;border-radius:12px 12px 0 0;">
            <h1 style="margin:0;color:#fff;">⚠️ Subscription Suspended</h1></td></tr>
            <tr><td style="padding:40px;">
            <h2 style="color:#1f2937;">Immediate Action Required</h2>
            <p style="color:#4b5563;">Hello Dr. %s,</p>
            <p style="color:#4b5563;">Your subscription is suspended due to failed payments. Your data is safe.</p>
            <div style="text-align:center;margin:30px 0;">
            <a href="%s/settings?tab=subscription" style="display:inline-block;background:#059669;color:#fff;padding:16px 40px;border-radius:8px;text-decoration:none;">Update Payment</a>
            </div></td></tr></table></td></tr></table></body></html>
            """, name, frontendUrl);
    }

    private String buildSubscriptionSuspendedText(String recipientName) {
        String name = safeName(recipientName, "Doctor");
        return String.format("IndigoRx - Subscription Suspended\n\nHello Dr. %s,\n\nUpdate payment: %s/settings?tab=subscription", name, frontendUrl);
    }

    private String buildTrialEndingHtml(String recipientName, String endDate) {
        String name = safeName(recipientName, "Doctor");
        return String.format("""
            <!DOCTYPE html><html><body style="margin:0;padding:0;font-family:sans-serif;background:#f4f7fa;">
            <table style="width:100%%;"><tr><td align="center" style="padding:40px 0;">
            <table style="width:600px;background:#fff;border-radius:12px;">
            <tr><td style="padding:40px;text-align:center;background:#3b82f6;border-radius:12px 12px 0 0;">
            <h1 style="margin:0;color:#fff;">⏰ Trial Ending Soon</h1></td></tr>
            <tr><td style="padding:40px;">
            <h2 style="color:#1f2937;">Don't Lose Access!</h2>
            <p style="color:#4b5563;">Hello Dr. %s,</p>
            <p style="color:#4b5563;">Your trial ends on <strong>%s</strong>. Subscribe to continue.</p>
            <div style="text-align:center;margin:30px 0;">
            <a href="%s/subscription" style="display:inline-block;background:#059669;color:#fff;padding:16px 40px;border-radius:8px;text-decoration:none;">Subscribe Now</a>
            </div></td></tr></table></td></tr></table></body></html>
            """, name, endDate, frontendUrl);
    }

    private String buildTrialEndingText(String recipientName, String endDate) {
        String name = safeName(recipientName, "Doctor");
        return String.format("IndigoRx - Trial Ending\n\nHello Dr. %s,\n\nTrial ends: %s\n\nSubscribe: %s/subscription", name, endDate, frontendUrl);
    }

    private String buildSubscriptionPausedHtml(String recipientName) {
        String name = safeName(recipientName, "Doctor");
        return String.format("""
            <!DOCTYPE html><html><body style="margin:0;padding:0;font-family:sans-serif;background:#f4f7fa;">
            <table style="width:100%%;"><tr><td align="center" style="padding:40px 0;">
            <table style="width:600px;background:#fff;border-radius:12px;">
            <tr><td style="padding:40px;text-align:center;background:#6b7280;border-radius:12px 12px 0 0;">
            <h1 style="margin:0;color:#fff;">Subscription Paused</h1></td></tr>
            <tr><td style="padding:40px;">
            <h2 style="color:#1f2937;">Your Subscription is Paused</h2>
            <p style="color:#4b5563;">Hello Dr. %s,</p>
            <p style="color:#4b5563;">Your subscription is paused. Resume anytime from settings.</p>
            <div style="text-align:center;margin:30px 0;">
            <a href="%s/settings?tab=subscription" style="display:inline-block;background:#059669;color:#fff;padding:16px 40px;border-radius:8px;text-decoration:none;">Resume</a>
            </div></td></tr></table></td></tr></table></body></html>
            """, name, frontendUrl);
    }

    private String buildSubscriptionPausedText(String recipientName) {
        String name = safeName(recipientName, "Doctor");
        return String.format("IndigoRx - Subscription Paused\n\nHello Dr. %s,\n\nResume: %s/settings?tab=subscription", name, frontendUrl);
    }

    private String buildSubscriptionResumedHtml(String recipientName) {
        String name = safeName(recipientName, "Doctor");
        return String.format("""
            <!DOCTYPE html><html><body style="margin:0;padding:0;font-family:sans-serif;background:#f4f7fa;">
            <table style="width:100%%;"><tr><td align="center" style="padding:40px 0;">
            <table style="width:600px;background:#fff;border-radius:12px;">
            <tr><td style="padding:40px;text-align:center;background:#059669;border-radius:12px 12px 0 0;">
            <h1 style="margin:0;color:#fff;">✓ Subscription Resumed</h1></td></tr>
            <tr><td style="padding:40px;">
            <h2 style="color:#1f2937;">Welcome Back!</h2>
            <p style="color:#4b5563;">Hello Dr. %s,</p>
            <p style="color:#4b5563;">Your subscription is active again. Enjoy all IndigoRx Pro features!</p>
            <div style="text-align:center;margin:30px 0;">
            <a href="%s/dashboard" style="display:inline-block;background:#059669;color:#fff;padding:16px 40px;border-radius:8px;text-decoration:none;">Go to Dashboard</a>
            </div></td></tr></table></td></tr></table></body></html>
            """, name, frontendUrl);
    }

    private String buildSubscriptionResumedText(String recipientName) {
        String name = safeName(recipientName, "Doctor");
        return String.format("IndigoRx - Subscription Resumed\n\nHello Dr. %s,\n\nYour subscription is active.\n\nDashboard: %s/dashboard", name, frontendUrl);
    }
}