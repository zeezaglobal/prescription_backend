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

    /**
     * Send OTP verification email
     */
    public boolean sendOtpEmail(String toEmail, String otp, String recipientName) {
        String subject = "Verify Your IndigoRx Account";
        String htmlBody = buildOtpEmailHtml(otp, recipientName);
        String textBody = buildOtpEmailText(otp, recipientName);
        return sendEmail(toEmail, subject, htmlBody, textBody);
    }

    /**
     * Send password reset email with link
     */
    public boolean sendPasswordResetEmail(String toEmail, String resetToken, String recipientName) {
        String subject = "Reset Your IndigoRx Password";
        String resetLink = frontendUrl + "/reset-password?token=" + resetToken;
        String htmlBody = buildPasswordResetEmailHtml(resetLink, recipientName);
        String textBody = buildPasswordResetEmailText(resetLink, recipientName);
        return sendEmail(toEmail, subject, htmlBody, textBody);
    }

    /**
     * Send welcome email after successful verification
     */
    public boolean sendWelcomeEmail(String toEmail, String doctorName) {
        String subject = "Welcome to IndigoRx!";
        String htmlBody = buildWelcomeEmailHtml(doctorName);
        String textBody = buildWelcomeEmailText(doctorName);
        return sendEmail(toEmail, subject, htmlBody, textBody);
    }

    /**
     * Send password changed confirmation email
     */
    public boolean sendPasswordChangedEmail(String toEmail, String recipientName) {
        String subject = "Your IndigoRx Password Has Been Changed";
        String htmlBody = buildPasswordChangedEmailHtml(recipientName);
        String textBody = buildPasswordChangedEmailText(recipientName);
        return sendEmail(toEmail, subject, htmlBody, textBody);
    }

    /**
     * Core email sending method using Postmark API
     */
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
                    POSTMARK_API_URL,
                    HttpMethod.POST,
                    request,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                logger.info("Email sent successfully to: {}", toEmail);
                return true;
            } else {
                logger.error("Failed to send email. Status: {}, Body: {}", response.getStatusCode(), response.getBody());
                return false;
            }

        } catch (Exception e) {
            logger.error("Error sending email to {}: {}", toEmail, e.getMessage(), e);
            return false;
        }
    }

    // ==================== EMAIL TEMPLATES ====================

    private String buildOtpEmailHtml(String otp, String recipientName) {
        String name = (recipientName != null && !recipientName.isEmpty()) ? recipientName : "Doctor";

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
            </head>
            <body style="margin: 0; padding: 0; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f4f7fa;">
                <table role="presentation" style="width: 100%%; border-collapse: collapse;">
                    <tr>
                        <td align="center" style="padding: 40px 0;">
                            <table role="presentation" style="width: 600px; border-collapse: collapse; background-color: #ffffff; border-radius: 12px; box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);">
                                <tr>
                                    <td style="padding: 40px 40px 20px 40px; text-align: center; background: linear-gradient(135deg, #059669 0%%, #047857 100%%); border-radius: 12px 12px 0 0;">
                                        <h1 style="margin: 0; color: #ffffff; font-size: 28px; font-weight: 700;">IndigoRx</h1>
                                        <p style="margin: 10px 0 0 0; color: #d1fae5; font-size: 14px;">Prescription Management System</p>
                                    </td>
                                </tr>
                                <tr>
                                    <td style="padding: 40px;">
                                        <h2 style="margin: 0 0 20px 0; color: #1f2937; font-size: 24px; font-weight: 600;">Verify Your Email</h2>
                                        <p style="margin: 0 0 20px 0; color: #4b5563; font-size: 16px; line-height: 1.6;">Hello %s,</p>
                                        <p style="margin: 0 0 30px 0; color: #4b5563; font-size: 16px; line-height: 1.6;">
                                            Thank you for registering with IndigoRx. Please use the following verification code to complete your registration:
                                        </p>
                                        <div style="text-align: center; margin: 30px 0;">
                                            <div style="display: inline-block; background: linear-gradient(135deg, #f0fdf4 0%%, #dcfce7 100%%); border: 2px solid #059669; border-radius: 12px; padding: 20px 40px;">
                                                <span style="font-size: 36px; font-weight: 700; color: #059669; letter-spacing: 8px; font-family: 'Courier New', monospace;">%s</span>
                                            </div>
                                        </div>
                                        <p style="margin: 30px 0 20px 0; color: #6b7280; font-size: 14px; line-height: 1.6;">
                                            This code will expire in <strong>10 minutes</strong>. If you didn't request this verification, please ignore this email.
                                        </p>
                                        <div style="background-color: #fef3c7; border-left: 4px solid #f59e0b; padding: 15px 20px; border-radius: 0 8px 8px 0; margin-top: 30px;">
                                            <p style="margin: 0; color: #92400e; font-size: 14px;">
                                                <strong>Security Notice:</strong> Never share this code with anyone. IndigoRx staff will never ask for your verification code.
                                            </p>
                                        </div>
                                    </td>
                                </tr>
                                <tr>
                                    <td style="padding: 30px 40px; background-color: #f9fafb; border-radius: 0 0 12px 12px; text-align: center;">
                                        <p style="margin: 0 0 10px 0; color: #6b7280; font-size: 14px;">
                                            Need help? Contact us at <a href="mailto:support@indigorx.me" style="color: #059669; text-decoration: none;">support@indigorx.me</a>
                                        </p>
                                        <p style="margin: 0; color: #9ca3af; font-size: 12px;">© 2024 IndigoRx. All rights reserved.</p>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                </table>
            </body>
            </html>
            """.formatted(name, otp);
    }

    private String buildOtpEmailText(String otp, String recipientName) {
        String name = (recipientName != null && !recipientName.isEmpty()) ? recipientName : "Doctor";
        return """
            IndigoRx - Verify Your Email
            
            Hello %s,
            
            Thank you for registering with IndigoRx. Please use the following verification code:
            
            Your Verification Code: %s
            
            This code will expire in 10 minutes.
            
            If you didn't request this verification, please ignore this email.
            
            © 2024 IndigoRx. All rights reserved.
            """.formatted(name, otp);
    }

    private String buildPasswordResetEmailHtml(String resetLink, String recipientName) {
        String name = (recipientName != null && !recipientName.isEmpty()) ? recipientName : "User";

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
            </head>
            <body style="margin: 0; padding: 0; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f4f7fa;">
                <table role="presentation" style="width: 100%%; border-collapse: collapse;">
                    <tr>
                        <td align="center" style="padding: 40px 0;">
                            <table role="presentation" style="width: 600px; border-collapse: collapse; background-color: #ffffff; border-radius: 12px; box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);">
                                <tr>
                                    <td style="padding: 40px 40px 20px 40px; text-align: center; background: linear-gradient(135deg, #059669 0%%, #047857 100%%); border-radius: 12px 12px 0 0;">
                                        <h1 style="margin: 0; color: #ffffff; font-size: 28px; font-weight: 700;">IndigoRx</h1>
                                        <p style="margin: 10px 0 0 0; color: #d1fae5; font-size: 14px;">Prescription Management System</p>
                                    </td>
                                </tr>
                                <tr>
                                    <td style="padding: 40px;">
                                        <h2 style="margin: 0 0 20px 0; color: #1f2937; font-size: 24px; font-weight: 600;">Reset Your Password</h2>
                                        <p style="margin: 0 0 20px 0; color: #4b5563; font-size: 16px; line-height: 1.6;">Hello %s,</p>
                                        <p style="margin: 0 0 30px 0; color: #4b5563; font-size: 16px; line-height: 1.6;">
                                            We received a request to reset your password. Click the button below to create a new password:
                                        </p>
                                        <div style="text-align: center; margin: 30px 0;">
                                            <a href="%s" style="display: inline-block; background: linear-gradient(135deg, #059669 0%%, #047857 100%%); color: #ffffff; text-decoration: none; padding: 16px 40px; border-radius: 8px; font-size: 16px; font-weight: 600; box-shadow: 0 4px 6px rgba(5, 150, 105, 0.3);">
                                                Reset Password
                                            </a>
                                        </div>
                                        <p style="margin: 30px 0 10px 0; color: #6b7280; font-size: 14px; line-height: 1.6;">Or copy and paste this link:</p>
                                        <p style="margin: 0 0 20px 0; color: #059669; font-size: 14px; word-break: break-all;">%s</p>
                                        <p style="margin: 20px 0; color: #6b7280; font-size: 14px; line-height: 1.6;">
                                            This link will expire in <strong>1 hour</strong>. If you didn't request a password reset, please ignore this email.
                                        </p>
                                        <div style="background-color: #fef3c7; border-left: 4px solid #f59e0b; padding: 15px 20px; border-radius: 0 8px 8px 0; margin-top: 30px;">
                                            <p style="margin: 0; color: #92400e; font-size: 14px;">
                                                <strong>Security Notice:</strong> If you didn't request this, your account may be at risk. Contact support immediately.
                                            </p>
                                        </div>
                                    </td>
                                </tr>
                                <tr>
                                    <td style="padding: 30px 40px; background-color: #f9fafb; border-radius: 0 0 12px 12px; text-align: center;">
                                        <p style="margin: 0 0 10px 0; color: #6b7280; font-size: 14px;">
                                            Need help? Contact us at <a href="mailto:support@indigorx.me" style="color: #059669; text-decoration: none;">support@indigorx.me</a>
                                        </p>
                                        <p style="margin: 0; color: #9ca3af; font-size: 12px;">© 2024 IndigoRx. All rights reserved.</p>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                </table>
            </body>
            </html>
            """.formatted(name, resetLink, resetLink);
    }

    private String buildPasswordResetEmailText(String resetLink, String recipientName) {
        String name = (recipientName != null && !recipientName.isEmpty()) ? recipientName : "User";
        return """
            IndigoRx - Reset Your Password
            
            Hello %s,
            
            We received a request to reset your password. Click the link below:
            
            %s
            
            This link will expire in 1 hour.
            
            If you didn't request this, please ignore this email or contact support.
            
            © 2024 IndigoRx. All rights reserved.
            """.formatted(name, resetLink);
    }

    private String buildWelcomeEmailHtml(String doctorName) {
        String name = (doctorName != null && !doctorName.isEmpty()) ? doctorName : "Doctor";

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
            </head>
            <body style="margin: 0; padding: 0; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f4f7fa;">
                <table role="presentation" style="width: 100%%; border-collapse: collapse;">
                    <tr>
                        <td align="center" style="padding: 40px 0;">
                            <table role="presentation" style="width: 600px; border-collapse: collapse; background-color: #ffffff; border-radius: 12px; box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);">
                                <tr>
                                    <td style="padding: 40px 40px 20px 40px; text-align: center; background: linear-gradient(135deg, #059669 0%%, #047857 100%%); border-radius: 12px 12px 0 0;">
                                        <h1 style="margin: 0; color: #ffffff; font-size: 28px; font-weight: 700;">🎉 Welcome to IndigoRx!</h1>
                                    </td>
                                </tr>
                                <tr>
                                    <td style="padding: 40px;">
                                        <h2 style="margin: 0 0 20px 0; color: #1f2937; font-size: 24px; font-weight: 600;">Hello Dr. %s!</h2>
                                        <p style="margin: 0 0 20px 0; color: #4b5563; font-size: 16px; line-height: 1.6;">
                                            Your email has been verified successfully. Welcome to IndigoRx - your trusted prescription management platform.
                                        </p>
                                        <div style="background-color: #f0fdf4; border-radius: 8px; padding: 20px; margin: 20px 0;">
                                            <h3 style="margin: 0 0 15px 0; color: #059669; font-size: 18px;">Getting Started:</h3>
                                            <ul style="margin: 0; padding-left: 20px; color: #4b5563;">
                                                <li style="margin-bottom: 10px;">Complete your profile with professional details</li>
                                                <li style="margin-bottom: 10px;">Add your patients to the system</li>
                                                <li style="margin-bottom: 10px;">Start creating digital prescriptions</li>
                                                <li>Upload your digital signature</li>
                                            </ul>
                                        </div>
                                        <div style="text-align: center; margin: 30px 0;">
                                            <a href="%s/dashboard" style="display: inline-block; background: linear-gradient(135deg, #059669 0%%, #047857 100%%); color: #ffffff; text-decoration: none; padding: 16px 40px; border-radius: 8px; font-size: 16px; font-weight: 600;">
                                                Go to Dashboard
                                            </a>
                                        </div>
                                    </td>
                                </tr>
                                <tr>
                                    <td style="padding: 30px 40px; background-color: #f9fafb; border-radius: 0 0 12px 12px; text-align: center;">
                                        <p style="margin: 0 0 10px 0; color: #6b7280; font-size: 14px;">
                                            Need help? Contact us at <a href="mailto:support@indigorx.me" style="color: #059669; text-decoration: none;">support@indigorx.me</a>
                                        </p>
                                        <p style="margin: 0; color: #9ca3af; font-size: 12px;">© 2024 IndigoRx. All rights reserved.</p>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                </table>
            </body>
            </html>
            """.formatted(name, frontendUrl);
    }

    private String buildWelcomeEmailText(String doctorName) {
        String name = (doctorName != null && !doctorName.isEmpty()) ? doctorName : "Doctor";
        return """
            Welcome to IndigoRx!
            
            Hello Dr. %s,
            
            Your email has been verified successfully. Welcome to IndigoRx!
            
            Getting Started:
            - Complete your profile with professional details
            - Add your patients to the system
            - Start creating digital prescriptions
            - Upload your digital signature
            
            Visit your dashboard: %s/dashboard
            
            © 2024 IndigoRx. All rights reserved.
            """.formatted(name, frontendUrl);
    }

    private String buildPasswordChangedEmailHtml(String recipientName) {
        String name = (recipientName != null && !recipientName.isEmpty()) ? recipientName : "User";

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
            </head>
            <body style="margin: 0; padding: 0; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f4f7fa;">
                <table role="presentation" style="width: 100%%; border-collapse: collapse;">
                    <tr>
                        <td align="center" style="padding: 40px 0;">
                            <table role="presentation" style="width: 600px; border-collapse: collapse; background-color: #ffffff; border-radius: 12px; box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);">
                                <tr>
                                    <td style="padding: 40px 40px 20px 40px; text-align: center; background: linear-gradient(135deg, #059669 0%%, #047857 100%%); border-radius: 12px 12px 0 0;">
                                        <h1 style="margin: 0; color: #ffffff; font-size: 28px; font-weight: 700;">IndigoRx</h1>
                                        <p style="margin: 10px 0 0 0; color: #d1fae5; font-size: 14px;">Security Notification</p>
                                    </td>
                                </tr>
                                <tr>
                                    <td style="padding: 40px;">
                                        <div style="text-align: center; margin-bottom: 30px;">
                                            <div style="display: inline-block; background-color: #d1fae5; border-radius: 50%%; padding: 20px;">
                                                <span style="font-size: 40px;">✓</span>
                                            </div>
                                        </div>
                                        <h2 style="margin: 0 0 20px 0; color: #1f2937; font-size: 24px; font-weight: 600; text-align: center;">Password Changed Successfully</h2>
                                        <p style="margin: 0 0 20px 0; color: #4b5563; font-size: 16px; line-height: 1.6;">Hello %s,</p>
                                        <p style="margin: 0 0 30px 0; color: #4b5563; font-size: 16px; line-height: 1.6;">
                                            Your IndigoRx account password has been changed successfully. If you made this change, you can safely ignore this email.
                                        </p>
                                        <div style="background-color: #fef2f2; border-left: 4px solid #ef4444; padding: 15px 20px; border-radius: 0 8px 8px 0; margin-top: 30px;">
                                            <p style="margin: 0; color: #991b1b; font-size: 14px;">
                                                <strong>Didn't make this change?</strong> Contact support immediately at <a href="mailto:support@indigorx.me" style="color: #dc2626;">support@indigorx.me</a>
                                            </p>
                                        </div>
                                    </td>
                                </tr>
                                <tr>
                                    <td style="padding: 30px 40px; background-color: #f9fafb; border-radius: 0 0 12px 12px; text-align: center;">
                                        <p style="margin: 0; color: #9ca3af; font-size: 12px;">© 2024 IndigoRx. All rights reserved.</p>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                </table>
            </body>
            </html>
            """.formatted(name);
    }

    private String buildPasswordChangedEmailText(String recipientName) {
        String name = (recipientName != null && !recipientName.isEmpty()) ? recipientName : "User";
        return """
            IndigoRx - Password Changed Successfully
            
            Hello %s,
            
            Your IndigoRx account password has been changed successfully.
            
            If you made this change, you can safely ignore this email.
            
            Didn't make this change? Contact support immediately at support@indigorx.me
            
            © 2024 IndigoRx. All rights reserved.
            """.formatted(name);
    }
}
