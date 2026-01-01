package com.zeezaglobal.prescription.Service;




import com.zeezaglobal.prescription.Entities.Doctor;
import com.zeezaglobal.prescription.Entities.PasswordResetToken;
import com.zeezaglobal.prescription.Entities.Patient;
import com.zeezaglobal.prescription.Entities.User;
import com.zeezaglobal.prescription.Repository.DoctorRepository;
import com.zeezaglobal.prescription.Repository.PasswordResetTokenRepository;
import com.zeezaglobal.prescription.Repository.PatientRepository;
import com.zeezaglobal.prescription.Repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class PasswordResetService {

    private static final Logger logger = LoggerFactory.getLogger(PasswordResetService.class);

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private PostmarkEmailService emailService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${password.reset.max.requests.per.hour:3}")
    private int maxRequestsPerHour;

    /**
     * Initiate password reset - sends reset link via email
     */
    @Transactional
    public PasswordResetResult initiatePasswordReset(String email) {
        try {
            // Find user by email
            User user = findUserByEmail(email);

            if (user == null) {
                // Don't reveal if email exists (security best practice)
                logger.info("Password reset requested for non-existent email: {}", email);
                return PasswordResetResult.success(
                        "If an account exists with this email, you will receive a password reset link."
                );
            }

            // Check rate limiting
            long recentRequests = tokenRepository.countRecentTokenRequests(email, LocalDateTime.now().minusHours(1));

            if (recentRequests >= maxRequestsPerHour) {
                logger.warn("Password reset rate limit exceeded for: {}", email);
                return PasswordResetResult.error("Too many password reset requests. Please try again later.");
            }

            // Invalidate previous tokens
            tokenRepository.invalidatePreviousTokensByEmail(email);

            // Create new token
            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setEmail(email);
            resetToken.setUser(user);
            resetToken.setToken(UUID.randomUUID().toString());
            resetToken.setExpiresAt(LocalDateTime.now().plusHours(1));

            tokenRepository.save(resetToken);

            // Get user name for email
            String userName = getUserName(user);

            // Send reset email
            boolean emailSent = emailService.sendPasswordResetEmail(email, resetToken.getToken(), userName);

            if (emailSent) {
                logger.info("Password reset email sent to: {}", email);
                return PasswordResetResult.success(
                        "If an account exists with this email, you will receive a password reset link."
                );
            } else {
                logger.error("Failed to send password reset email to: {}", email);
                return PasswordResetResult.error("Failed to send password reset email. Please try again.");
            }

        } catch (Exception e) {
            logger.error("Error initiating password reset for {}: {}", email, e.getMessage(), e);
            return PasswordResetResult.error("An error occurred. Please try again.");
        }
    }

    /**
     * Validate password reset token
     */
    public PasswordResetResult validateToken(String token) {
        try {
            Optional<PasswordResetToken> tokenOptional = tokenRepository.findValidToken(token, LocalDateTime.now());

            if (tokenOptional.isEmpty()) {
                Optional<PasswordResetToken> anyToken = tokenRepository.findByToken(token);

                if (anyToken.isPresent()) {
                    PasswordResetToken existingToken = anyToken.get();
                    if (existingToken.isUsed()) {
                        return PasswordResetResult.error("This password reset link has already been used.");
                    }
                    if (existingToken.isExpired()) {
                        return PasswordResetResult.error("This password reset link has expired. Please request a new one.");
                    }
                }

                return PasswordResetResult.error("Invalid password reset link.");
            }

            return PasswordResetResult.success("Token is valid.");

        } catch (Exception e) {
            logger.error("Error validating reset token: {}", e.getMessage(), e);
            return PasswordResetResult.error("An error occurred. Please try again.");
        }
    }

    /**
     * Reset password using token
     */
    @Transactional
    public PasswordResetResult resetPassword(String token, String newPassword) {
        try {
            // Validate password requirements
            PasswordResetResult validationResult = validatePassword(newPassword);
            if (!validationResult.isSuccess()) {
                return validationResult;
            }

            // Find valid token
            Optional<PasswordResetToken> tokenOptional = tokenRepository.findValidToken(token, LocalDateTime.now());

            if (tokenOptional.isEmpty()) {
                return PasswordResetResult.error("Invalid or expired password reset link.");
            }

            PasswordResetToken resetToken = tokenOptional.get();
            User user = resetToken.getUser();

            if (user == null) {
                user = findUserByEmail(resetToken.getEmail());
                if (user == null) {
                    return PasswordResetResult.error("User account not found.");
                }
            }

            // Update password based on user type
            String encodedPassword = passwordEncoder.encode(newPassword);

            if (user instanceof Doctor) {
                Doctor doctor = (Doctor) user;
                doctor.setPassword(encodedPassword);
                doctorRepository.save(doctor);
            } else if (user instanceof Patient) {
                Patient patient = (Patient) user;
                patient.setPassword(encodedPassword);
                patientRepository.save(patient);
            } else {
                user.setPassword(encodedPassword);
                userRepository.save(user);
            }

            // Mark token as used
            resetToken.setUsed(true);
            tokenRepository.save(resetToken);

            // Send confirmation email
            String userName = getUserName(user);
            emailService.sendPasswordChangedEmail(resetToken.getEmail(), userName);

            logger.info("Password reset successful for: {}", resetToken.getEmail());
            return PasswordResetResult.success("Password has been reset successfully. You can now log in with your new password.");

        } catch (Exception e) {
            logger.error("Error resetting password: {}", e.getMessage(), e);
            return PasswordResetResult.error("An error occurred. Please try again.");
        }
    }

    /**
     * Validate password requirements
     */
    private PasswordResetResult validatePassword(String password) {
        if (password == null || password.isEmpty()) {
            return PasswordResetResult.error("Password is required.");
        }

        if (password.length() < 8) {
            return PasswordResetResult.error("Password must be at least 8 characters long.");
        }

        if (password.length() > 128) {
            return PasswordResetResult.error("Password must not exceed 128 characters.");
        }

        if (!password.matches(".*[A-Z].*")) {
            return PasswordResetResult.error("Password must contain at least one uppercase letter.");
        }

        if (!password.matches(".*[a-z].*")) {
            return PasswordResetResult.error("Password must contain at least one lowercase letter.");
        }

        if (!password.matches(".*\\d.*")) {
            return PasswordResetResult.error("Password must contain at least one number.");
        }

        if (!password.matches(".*[!@#$%^&*(),.?\":{}|<>].*")) {
            return PasswordResetResult.error("Password must contain at least one special character.");
        }

        return PasswordResetResult.success("Password is valid.");
    }

    /**
     * Find user by email (Doctor or Patient)
     */
    private User findUserByEmail(String email) {
        Optional<Doctor> doctor = doctorRepository.findByEmail(email);
        if (doctor.isPresent()) {
            return doctor.get();
        }

        Optional<Patient> patient = patientRepository.findByEmail(email);
        if (patient.isPresent()) {
            return patient.get();
        }

        return null;
    }

    /**
     * Get user's display name
     */
    private String getUserName(User user) {
        if (user instanceof Doctor) {
            return ((Doctor) user).getName();
        } else if (user instanceof Patient) {
            return ((Patient) user).getName();
        }
        return user.getUsername();
    }

    /**
     * Result class for password reset operations
     */
    public static class PasswordResetResult {
        private final boolean success;
        private final String message;

        private PasswordResetResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public static PasswordResetResult success(String message) {
            return new PasswordResetResult(true, message);
        }

        public static PasswordResetResult error(String message) {
            return new PasswordResetResult(false, message);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }
    }
}
