package com.zeezaglobal.prescription.Service;




import com.zeezaglobal.prescription.Entities.OtpVerification;
import com.zeezaglobal.prescription.Repository.OtpVerificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class OtpService {

    private static final Logger logger = LoggerFactory.getLogger(OtpService.class);
    private static final SecureRandom secureRandom = new SecureRandom();

    @Autowired
    private OtpVerificationRepository otpRepository;

    @Autowired
    private PostmarkEmailService emailService;

    @Value("${otp.expiry.minutes:10}")
    private int otpExpiryMinutes;

    @Value("${otp.max.requests.per.hour:5}")
    private int maxOtpRequestsPerHour;

    @Value("${otp.length:6}")
    private int otpLength;

    /**
     * Generate and send OTP for email verification
     */
    @Transactional
    public OtpResult generateAndSendOtp(String email, OtpVerification.OtpType type) {
        return generateAndSendOtp(email, type, null);
    }

    /**
     * Generate and send OTP with recipient name
     */
    @Transactional
    public OtpResult generateAndSendOtp(String email, OtpVerification.OtpType type, String recipientName) {
        try {
            // Check if email is blocked due to too many failed attempts
            if (otpRepository.isEmailBlocked(email, LocalDateTime.now())) {
                logger.warn("OTP request blocked for email: {} - too many failed attempts", email);
                return OtpResult.error("Too many failed attempts. Please try again in 30 minutes.");
            }

            // Check rate limiting - max requests per hour
            long recentRequests = otpRepository.countRecentOtpRequests(email, LocalDateTime.now().minusHours(1));

            if (recentRequests >= maxOtpRequestsPerHour) {
                logger.warn("OTP rate limit exceeded for email: {}", email);
                return OtpResult.error("Too many OTP requests. Please try again later.");
            }

            // Invalidate all previous OTPs for this email and type
            otpRepository.invalidatePreviousOtps(email, type);

            // Generate new secure OTP
            String otp = generateOtp();

            // Create and save OTP entity
            OtpVerification otpVerification = new OtpVerification();
            otpVerification.setEmail(email);
            otpVerification.setOtp(otp);
            otpVerification.setType(type);
            otpVerification.setCreatedAt(LocalDateTime.now());
            otpVerification.setExpiresAt(LocalDateTime.now().plusMinutes(otpExpiryMinutes));
            otpVerification.setUsed(false);
            otpVerification.setAttempts(0);
            otpVerification.setBlocked(false);

            otpRepository.save(otpVerification);

            // Send OTP email
            boolean emailSent = emailService.sendOtpEmail(email, otp, recipientName);

            if (emailSent) {
                logger.info("OTP sent successfully to: {}", maskEmail(email));
                return OtpResult.success("Verification code sent to " + maskEmail(email));
            } else {
                logger.error("Failed to send OTP email to: {}", email);
                return OtpResult.error("Failed to send verification code. Please try again.");
            }

        } catch (Exception e) {
            logger.error("Error generating OTP for {}: {}", email, e.getMessage(), e);
            return OtpResult.error("An error occurred. Please try again.");
        }
    }

    /**
     * Verify OTP code
     */
    @Transactional
    public OtpResult verifyOtp(String email, String otp, OtpVerification.OtpType type) {
        try {
            // Check if email is blocked
            if (otpRepository.isEmailBlocked(email, LocalDateTime.now())) {
                return OtpResult.error("Account temporarily locked due to too many failed attempts. Please try again in 30 minutes.");
            }

            // Find valid OTP matching email, otp code, and type
            Optional<OtpVerification> otpOptional = otpRepository.findByEmailAndOtpAndType(
                    email, otp, type, LocalDateTime.now()
            );

            if (otpOptional.isEmpty()) {
                // OTP not found or invalid - increment attempts on latest OTP
                Optional<OtpVerification> latestOtp = otpRepository.findLatestValidOtp(email, type, LocalDateTime.now());

                if (latestOtp.isPresent()) {
                    OtpVerification verification = latestOtp.get();
                    verification.incrementAttempts();
                    otpRepository.save(verification);

                    if (verification.isBlocked()) {
                        logger.warn("Email {} blocked after too many failed OTP attempts", email);
                        return OtpResult.error("Too many failed attempts. Account temporarily locked for 30 minutes.");
                    }

                    int remainingAttempts = 5 - verification.getAttempts();
                    return OtpResult.error("Invalid verification code. " + remainingAttempts + " attempt(s) remaining.");
                }

                return OtpResult.error("Invalid or expired verification code. Please request a new one.");
            }

            // OTP found and valid - mark as used
            OtpVerification verification = otpOptional.get();
            verification.setUsed(true);
            otpRepository.save(verification);

            logger.info("OTP verified successfully for: {}", maskEmail(email));
            return OtpResult.success("Email verified successfully.");

        } catch (Exception e) {
            logger.error("Error verifying OTP for {}: {}", email, e.getMessage(), e);
            return OtpResult.error("An error occurred during verification. Please try again.");
        }
    }

    /**
     * Resend OTP - generates new OTP and sends to email
     */
    @Transactional
    public OtpResult resendOtp(String email, OtpVerification.OtpType type, String recipientName) {
        return generateAndSendOtp(email, type, recipientName);
    }

    /**
     * Check if there's a valid pending OTP for email
     */
    public boolean hasPendingOtp(String email, OtpVerification.OtpType type) {
        return otpRepository.findLatestValidOtp(email, type, LocalDateTime.now()).isPresent();
    }

    /**
     * Get remaining time for current OTP in seconds
     */
    public long getRemainingTimeSeconds(String email, OtpVerification.OtpType type) {
        Optional<OtpVerification> otp = otpRepository.findLatestValidOtp(email, type, LocalDateTime.now());
        if (otp.isPresent()) {
            LocalDateTime expiresAt = otp.get().getExpiresAt();
            LocalDateTime now = LocalDateTime.now();
            if (expiresAt.isAfter(now)) {
                return java.time.Duration.between(now, expiresAt).getSeconds();
            }
        }
        return 0;
    }

    /**
     * Generate secure random numeric OTP
     */
    private String generateOtp() {
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < otpLength; i++) {
            otp.append(secureRandom.nextInt(10));
        }
        return otp.toString();
    }

    /**
     * Mask email for display (e.g., a***z@example.com)
     */
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }

        String[] parts = email.split("@");
        String localPart = parts[0];
        String domain = parts[1];

        if (localPart.length() <= 2) {
            return localPart.charAt(0) + "***@" + domain;
        }

        return localPart.charAt(0) + "***" + localPart.charAt(localPart.length() - 1) + "@" + domain;
    }

    /**
     * Result class for OTP operations
     */
    public static class OtpResult {
        private final boolean success;
        private final String message;

        private OtpResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public static OtpResult success(String message) {
            return new OtpResult(true, message);
        }

        public static OtpResult error(String message) {
            return new OtpResult(false, message);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }
    }
}