package com.zeezaglobal.prescription.Controller;

import com.stripe.exception.StripeException;
import com.zeezaglobal.prescription.DTO.*;
import com.zeezaglobal.prescription.DTO.OtpDto.ForgotPasswordDTO;
import com.zeezaglobal.prescription.DTO.OtpDto.OtpVerificationDTO;
import com.zeezaglobal.prescription.DTO.OtpDto.ResendOtpDTO;
import com.zeezaglobal.prescription.DTO.OtpDto.ResetPasswordDTO;
import com.zeezaglobal.prescription.Entities.Doctor;

import com.zeezaglobal.prescription.Entities.OtpVerification;
import com.zeezaglobal.prescription.Entities.Patient;
import com.zeezaglobal.prescription.Entities.User;
import com.zeezaglobal.prescription.Repository.DoctorRepository;
import com.zeezaglobal.prescription.Repository.PatientRepository;
import com.zeezaglobal.prescription.Repository.UserRepository;

import com.zeezaglobal.prescription.Service.OtpService;
import com.zeezaglobal.prescription.Service.PasswordResetService;
import com.zeezaglobal.prescription.Service.PostmarkEmailService;
import com.zeezaglobal.prescription.Service.StripeService;
import com.zeezaglobal.prescription.Utils.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    // Temporary storage for pending registrations (before email verification)
    private final ConcurrentHashMap<String, PendingDoctorRegistration> pendingRegistrations = new ConcurrentHashMap<>();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private StripeService stripeService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private OtpService otpService;

    @Autowired
    private PasswordResetService passwordResetService;

    @Autowired
    private PostmarkEmailService emailService;

    // ==================== DOCTOR REGISTRATION WITH EMAIL VERIFICATION ====================

    /**
     * Step 1: Initiate doctor registration - sends OTP to email
     */
    @PostMapping("/doctor/register")
    public ResponseEntity<Map<String, Object>> registerDoctor(@RequestBody DoctorRegistrationDTO request) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Validate input
            if (request.getEmail() == null || request.getEmail().isEmpty()) {
                response.put("success", false);
                response.put("message", "Email is required");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            if (!isValidEmail(request.getEmail())) {
                response.put("success", false);
                response.put("message", "Please enter a valid email address");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            if (request.getPassword() == null || request.getPassword().isEmpty()) {
                response.put("success", false);
                response.put("message", "Password is required");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            // Validate password strength
            String passwordError = validatePasswordStrength(request.getPassword());
            if (passwordError != null) {
                response.put("success", false);
                response.put("message", passwordError);
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            // Check if email already exists
            if (userRepository.existsByUsername(request.getEmail())) {
                response.put("success", false);
                response.put("message", "An account with this email already exists");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            if (doctorRepository.existsByEmail(request.getEmail())) {
                response.put("success", false);
                response.put("message", "An account with this email already exists");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            // Store pending registration
            PendingDoctorRegistration pending = new PendingDoctorRegistration();
            pending.setEmail(request.getEmail());
            pending.setPassword(request.getPassword());
            pending.setCreatedAt(System.currentTimeMillis());
            pendingRegistrations.put(request.getEmail(), pending);

            // Generate and send OTP
            OtpService.OtpResult otpResult = otpService.generateAndSendOtp(
                    request.getEmail(),
                    OtpVerification.OtpType.EMAIL_VERIFICATION,
                    null
            );

            if (otpResult.isSuccess()) {
                response.put("success", true);
                response.put("message", "Verification code sent to your email");
                response.put("email", request.getEmail());
                response.put("requiresVerification", true);
                return new ResponseEntity<>(response, HttpStatus.OK);
            } else {
                response.put("success", false);
                response.put("message", otpResult.getMessage());
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

        } catch (Exception e) {
            logger.error("Error during doctor registration: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "An error occurred during registration. Please try again.");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Step 2: Verify OTP and complete registration
     */
    @PostMapping("/doctor/verify-email")
    public ResponseEntity<Map<String, Object>> verifyDoctorEmail(@RequestBody OtpVerificationDTO request) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Validate input
            if (request.getEmail() == null || request.getEmail().isEmpty()) {
                response.put("success", false);
                response.put("message", "Email is required");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            if (request.getOtp() == null || request.getOtp().isEmpty()) {
                response.put("success", false);
                response.put("message", "Verification code is required");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            // Verify OTP
            OtpService.OtpResult verifyResult = otpService.verifyOtp(
                    request.getEmail(),
                    request.getOtp(),
                    OtpVerification.OtpType.EMAIL_VERIFICATION
            );

            if (!verifyResult.isSuccess()) {
                response.put("success", false);
                response.put("message", verifyResult.getMessage());
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            // Get pending registration
            PendingDoctorRegistration pending = pendingRegistrations.get(request.getEmail());

            if (pending == null) {
                response.put("success", false);
                response.put("message", "Registration session expired. Please start registration again.");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            // Check if registration is still valid (24 hour window)
            if (System.currentTimeMillis() - pending.getCreatedAt() > 24 * 60 * 60 * 1000) {
                pendingRegistrations.remove(request.getEmail());
                response.put("success", false);
                response.put("message", "Registration session expired. Please start registration again.");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            // Create the doctor account
            Doctor doctor = new Doctor();
            doctor.setUsername(pending.getEmail());
            doctor.setEmail(pending.getEmail());
            doctor.setPassword(passwordEncoder.encode(pending.getPassword()));

            // Set placeholder values for required fields
            doctor.setName("");
            doctor.setLicenseNumber("PENDING-" + System.currentTimeMillis());
            doctor.setSpecialization("");
            doctor.setPhone("");

            doctor.setValidated(0);
            doctor.setEmailVerified(true);
            doctor.setStatus(Doctor.DoctorStatus.ACTIVE);

            // Create Stripe customer
            try {
                String customerId = stripeService.createCustomer(pending.getEmail());
                doctor.setStripeUsername(customerId);
            } catch (StripeException e) {
                logger.error("Failed to create Stripe customer: {}", e.getMessage());
            }

            // Save doctor
            Doctor savedDoctor = doctorRepository.save(doctor);

            // Remove from pending registrations
            pendingRegistrations.remove(request.getEmail());

            // Generate JWT token
            String token = jwtUtil.generateToken(
                    savedDoctor.getUsername(),
                    savedDoctor.getId(),
                    "DOCTOR"
            );

            // Send welcome email
            emailService.sendWelcomeEmail(savedDoctor.getEmail(), savedDoctor.getName());

            // Prepare response
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("id", savedDoctor.getId());
            userMap.put("email", savedDoctor.getEmail());
            userMap.put("username", savedDoctor.getUsername());
            userMap.put("userType", "DOCTOR");
            userMap.put("profileComplete", false);
            userMap.put("emailVerified", true);

            response.put("success", true);
            response.put("message", "Email verified successfully! Welcome to IndigoRx.");
            response.put("token", token);
            response.put("user", userMap);

            return new ResponseEntity<>(response, HttpStatus.CREATED);

        } catch (Exception e) {
            logger.error("Error during email verification: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "An error occurred during verification. Please try again.");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Resend OTP for email verification
     */
    @PostMapping("/doctor/resend-otp")
    public ResponseEntity<Map<String, Object>> resendOtp(@RequestBody ResendOtpDTO request) {
        Map<String, Object> response = new HashMap<>();

        try {
            if (request.getEmail() == null || request.getEmail().isEmpty()) {
                response.put("success", false);
                response.put("message", "Email is required");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            // Check if there's a pending registration
            PendingDoctorRegistration pending = pendingRegistrations.get(request.getEmail());

            if (pending == null) {
                response.put("success", false);
                response.put("message", "No pending registration found. Please start registration again.");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            // Resend OTP
            OtpService.OtpResult otpResult = otpService.resendOtp(
                    request.getEmail(),
                    OtpVerification.OtpType.EMAIL_VERIFICATION,
                    null
            );

            response.put("success", otpResult.isSuccess());
            response.put("message", otpResult.getMessage());

            return new ResponseEntity<>(response, otpResult.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST);

        } catch (Exception e) {
            logger.error("Error resending OTP: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "An error occurred. Please try again.");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ==================== FORGOT PASSWORD ====================

    /**
     * Initiate forgot password - sends reset link via email
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, Object>> forgotPassword(@RequestBody ForgotPasswordDTO request) {
        Map<String, Object> response = new HashMap<>();

        try {
            if (request.getEmail() == null || request.getEmail().isEmpty()) {
                response.put("success", false);
                response.put("message", "Email is required");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            PasswordResetService.PasswordResetResult result = passwordResetService.initiatePasswordReset(request.getEmail());

            response.put("success", result.isSuccess());
            response.put("message", result.getMessage());

            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (Exception e) {
            logger.error("Error during forgot password: {}", e.getMessage(), e);
            response.put("success", true);
            response.put("message", "If an account exists with this email, you will receive a password reset link.");
            return new ResponseEntity<>(response, HttpStatus.OK);
        }
    }

    /**
     * Validate password reset token
     */
    @GetMapping("/reset-password/validate")
    public ResponseEntity<Map<String, Object>> validateResetToken(@RequestParam String token) {
        Map<String, Object> response = new HashMap<>();

        try {
            PasswordResetService.PasswordResetResult result = passwordResetService.validateToken(token);

            response.put("success", result.isSuccess());
            response.put("message", result.getMessage());
            response.put("valid", result.isSuccess());

            return new ResponseEntity<>(response, result.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST);

        } catch (Exception e) {
            logger.error("Error validating reset token: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "An error occurred. Please try again.");
            response.put("valid", false);
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Reset password using token
     */
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, Object>> resetPassword(@RequestBody ResetPasswordDTO request) {
        Map<String, Object> response = new HashMap<>();

        try {
            if (request.getToken() == null || request.getToken().isEmpty()) {
                response.put("success", false);
                response.put("message", "Reset token is required");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            if (request.getNewPassword() == null || request.getNewPassword().isEmpty()) {
                response.put("success", false);
                response.put("message", "New password is required");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            if (request.getConfirmPassword() == null || request.getConfirmPassword().isEmpty()) {
                response.put("success", false);
                response.put("message", "Please confirm your password");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            if (!request.getNewPassword().equals(request.getConfirmPassword())) {
                response.put("success", false);
                response.put("message", "Passwords do not match");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            PasswordResetService.PasswordResetResult result = passwordResetService.resetPassword(
                    request.getToken(),
                    request.getNewPassword()
            );

            response.put("success", result.isSuccess());
            response.put("message", result.getMessage());

            return new ResponseEntity<>(response, result.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST);

        } catch (Exception e) {
            logger.error("Error resetting password: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "An error occurred. Please try again.");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ==================== PATIENT REGISTRATION ====================

    @PostMapping("/patient/register")
    public ResponseEntity<Map<String, String>> registerPatient(@RequestBody PatientRegistrationDTO request) {
        Map<String, String> response = new HashMap<>();

        try {
            if (userRepository.existsByUsername(request.getUsername())) {
                response.put("message", "Username already exists!");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            if (request.getEmail() != null && !request.getEmail().isEmpty()
                    && patientRepository.existsByEmail(request.getEmail())) {
                response.put("message", "Email already exists!");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            Patient patient = new Patient();
            patient.setUsername(request.getUsername());
            patient.setPassword(passwordEncoder.encode(request.getPassword()));
            patient.setName(request.getName());
            patient.setDateOfBirth(request.getDateOfBirth());
            patient.setGender(request.getGender());
            patient.setPhone(request.getPhone());
            patient.setEmail(request.getEmail());
            patient.setAddress(request.getAddress());
            patient.setBloodGroup(request.getBloodGroup());
            patient.setMedicalHistory(request.getMedicalHistory());
            patient.setAllergies(request.getAllergies());

            Patient savedPatient = patientRepository.save(patient);

            response.put("message", "Patient registered successfully!");
            response.put("userId", savedPatient.getId().toString());
            return new ResponseEntity<>(response, HttpStatus.CREATED);

        } catch (Exception e) {
            response.put("message", "An error occurred during registration: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ==================== LOGIN ====================

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginDTO request) {
        try {
            if (request.getUsername() == null || request.getPassword() == null) {
                return ResponseEntity.badRequest()
                        .body(Collections.singletonMap("message", "Username and password are required"));
            }

            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );

            final UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());

            User user = userRepository.findByUsername(request.getUsername())
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            // Check if doctor's email is verified
            if (user instanceof Doctor) {
                Doctor doctor = (Doctor) user;
                if (!doctor.isEmailVerified()) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", false);
                    response.put("message", "Please verify your email before logging in");
                    response.put("requiresVerification", true);
                    response.put("email", doctor.getEmail());
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
                }
            }

            String token = jwtUtil.generateToken(
                    user.getUsername(),
                    user.getId(),
                    user.getUserType().toString()
            );

            Map<String, Object> userMap = new HashMap<>();
            userMap.put("id", user.getId());
            userMap.put("username", user.getUsername());
            userMap.put("userType", user.getUserType().toString());

            if (user instanceof Doctor) {
                Doctor doctor = (Doctor) user;
                userMap.put("name", doctor.getName());
                userMap.put("email", doctor.getEmail());
                userMap.put("specialization", doctor.getSpecialization());
                userMap.put("licenseNumber", doctor.getLicenseNumber());
                userMap.put("isValidated", doctor.getValidated());
                userMap.put("status", doctor.getStatus().toString());
                userMap.put("emailVerified", doctor.isEmailVerified());

                boolean profileComplete = doctor.getName() != null && !doctor.getName().isEmpty()
                        && !doctor.getLicenseNumber().startsWith("PENDING-")
                        && doctor.getSpecialization() != null && !doctor.getSpecialization().isEmpty()
                        && doctor.getPhone() != null && !doctor.getPhone().isEmpty();

                userMap.put("profileComplete", profileComplete);

            } else if (user instanceof Patient) {
                Patient patient = (Patient) user;
                userMap.put("name", patient.getName());
                userMap.put("email", patient.getEmail());
                userMap.put("age", patient.getAge());
                userMap.put("gender", patient.getGender());
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("token", token);
            response.put("user", userMap);

            return ResponseEntity.ok(response);

        } catch (BadCredentialsException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Invalid username or password");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        } catch (UsernameNotFoundException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "User not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "An error occurred: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ==================== TOKEN VALIDATION ====================

    @GetMapping("/validate-token")
    public ResponseEntity<Map<String, Object>> validateToken(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Collections.singletonMap("message", "Invalid authorization header"));
            }

            String token = authHeader.substring(7);
            String username = jwtUtil.extractUsername(token);

            if (username != null && jwtUtil.validateToken(token, username)) {
                User user = userRepository.findByUsername(username)
                        .orElseThrow(() -> new UsernameNotFoundException("User not found"));

                Map<String, Object> userMap = new HashMap<>();
                userMap.put("id", user.getId());
                userMap.put("username", user.getUsername());
                userMap.put("userType", user.getUserType().toString());

                if (user instanceof Doctor) {
                    Doctor doctor = (Doctor) user;
                    userMap.put("name", doctor.getName());
                    userMap.put("email", doctor.getEmail());
                    userMap.put("isValidated", doctor.getValidated());
                    userMap.put("emailVerified", doctor.isEmailVerified());

                    boolean profileComplete = doctor.getName() != null && !doctor.getName().isEmpty()
                            && !doctor.getLicenseNumber().startsWith("PENDING-")
                            && doctor.getSpecialization() != null && !doctor.getSpecialization().isEmpty()
                            && doctor.getPhone() != null && !doctor.getPhone().isEmpty();

                    userMap.put("profileComplete", profileComplete);

                } else if (user instanceof Patient) {
                    Patient patient = (Patient) user;
                    userMap.put("name", patient.getName());
                }

                Map<String, Object> response = new HashMap<>();
                response.put("valid", true);
                response.put("user", userMap);

                return ResponseEntity.ok(response);
            }

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Collections.singletonMap("message", "Invalid token"));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("message", "Token validation failed"));
        }
    }

    // ==================== UTILITY METHODS ====================

    private boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        return email.matches(emailRegex);
    }

    private String validatePasswordStrength(String password) {
        if (password.length() < 8) {
            return "Password must be at least 8 characters long";
        }
        if (!password.matches(".*[A-Z].*")) {
            return "Password must contain at least one uppercase letter";
        }
        if (!password.matches(".*[a-z].*")) {
            return "Password must contain at least one lowercase letter";
        }
        if (!password.matches(".*\\d.*")) {
            return "Password must contain at least one number";
        }
        if (!password.matches(".*[!@#$%^&*(),.?\":{}|<>].*")) {
            return "Password must contain at least one special character";
        }
        return null;
    }

    // ==================== INNER CLASS ====================

    private static class PendingDoctorRegistration {
        private String email;
        private String password;
        private long createdAt;

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public long getCreatedAt() { return createdAt; }
        public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    }
}