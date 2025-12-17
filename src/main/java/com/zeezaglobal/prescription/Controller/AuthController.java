package com.zeezaglobal.prescription.Controller;

import com.stripe.exception.StripeException;
import com.zeezaglobal.prescription.DTO.DoctorRegistrationDTO;
import com.zeezaglobal.prescription.DTO.LoginDTO;
import com.zeezaglobal.prescription.DTO.PatientRegistrationDTO;
import com.zeezaglobal.prescription.Entities.Doctor;
import com.zeezaglobal.prescription.Entities.Patient;
import com.zeezaglobal.prescription.Entities.User;
import com.zeezaglobal.prescription.Repository.DoctorRepository;
import com.zeezaglobal.prescription.Repository.PatientRepository;
import com.zeezaglobal.prescription.Repository.UserRepository;
import com.zeezaglobal.prescription.Service.StripeService;
import com.zeezaglobal.prescription.Utils.JwtUtil;
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

@RestController
@RequestMapping("/auth")
public class AuthController {

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

    @PostMapping("/doctor/register")
    public ResponseEntity<Map<String, String>> registerDoctor(@RequestBody DoctorRegistrationDTO request) {
        Map<String, String> response = new HashMap<>();

        try {
            // Check if username already exists
            if (userRepository.existsByUsername(request.getUsername())) {
                response.put("message", "Username already exists!");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            // Check if email already exists
            if (doctorRepository.existsByEmail(request.getEmail())) {
                response.put("message", "Email already exists!");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            // Check if license number already exists
            if (doctorRepository.existsByLicenseNumber(request.getLicenseNumber())) {
                response.put("message", "License number already exists!");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            // Create new doctor
            Doctor doctor = new Doctor();
            doctor.setUsername(request.getUsername());
            doctor.setPassword(passwordEncoder.encode(request.getPassword()));
            doctor.setName(request.getName());
            doctor.setEmail(request.getEmail());
            doctor.setLicenseNumber(request.getLicenseNumber());
            doctor.setSpecialization(request.getSpecialization());
            doctor.setPhone(request.getPhone());
            doctor.setAddress(request.getAddress());
            doctor.setQualifications(request.getQualifications());
            doctor.setHospitalName(request.getHospitalName());
            doctor.setValidated(0);
            doctor.setStatus(Doctor.DoctorStatus.ACTIVE);

            // Create Stripe customer
            try {
                String customerId = stripeService.createCustomer(request.getEmail());
                doctor.setStripeUsername(customerId);
            } catch (StripeException e) {
                response.put("message", "Registration not complete. Please contact support.");
                return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
            }

            // Save doctor
            doctorRepository.save(doctor);

            response.put("message", "Doctor registered successfully!");
            response.put("userId", doctor.getId().toString());
            return new ResponseEntity<>(response, HttpStatus.CREATED);

        } catch (Exception e) {
            response.put("message", "An error occurred during registration: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/patient/register")
    public ResponseEntity<Map<String, String>> registerPatient(@RequestBody PatientRegistrationDTO request) {
        Map<String, String> response = new HashMap<>();

        try {
            // Check if username already exists
            if (userRepository.existsByUsername(request.getUsername())) {
                response.put("message", "Username already exists!");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            // Check if email already exists (if provided)
            if (request.getEmail() != null && !request.getEmail().isEmpty()
                    && patientRepository.existsByEmail(request.getEmail())) {
                response.put("message", "Email already exists!");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            // Create new patient
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

            // Save patient
            patientRepository.save(patient);

            response.put("message", "Patient registered successfully!");
            response.put("userId", patient.getId().toString());
            return new ResponseEntity<>(response, HttpStatus.CREATED);

        } catch (Exception e) {
            response.put("message", "An error occurred during registration: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginDTO request) {
        try {
            if (request.getUsername() == null || request.getPassword() == null) {
                return ResponseEntity.badRequest()
                        .body(Collections.singletonMap("message", "Username and password are required"));
            }

            // Authenticate the user
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );

            // Load user details
            final UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());

            // Generate JWT token
            String token = jwtUtil.generateToken(userDetails.getUsername());

            // Fetch user entity from DB
            User user = userRepository.findByUsername(request.getUsername())
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            // Prepare user response
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("id", user.getId());
            userMap.put("username", user.getUsername());
            userMap.put("userType", user.getUserType().toString());

            // Add role-specific information
            if (user instanceof Doctor) {
                Doctor doctor = (Doctor) user;
                userMap.put("name", doctor.getName());
                userMap.put("email", doctor.getEmail());
                userMap.put("specialization", doctor.getSpecialization());
                userMap.put("licenseNumber", doctor.getLicenseNumber());
                userMap.put("isValidated", doctor.getValidated());
                userMap.put("status", doctor.getStatus().toString());
            } else if (user instanceof Patient) {
                Patient patient = (Patient) user;
                userMap.put("name", patient.getName());
                userMap.put("email", patient.getEmail());
                userMap.put("age", patient.getAge());
                userMap.put("gender", patient.getGender());
            }

            // Prepare final response
            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("user", userMap);

            return ResponseEntity.ok(response);

        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Collections.singletonMap("message", "Invalid username or password"));
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Collections.singletonMap("message", "User not found"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("message", "An error occurred: " + e.getMessage()));
        }
    }

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
                    userMap.put("isValidated", doctor.getValidated());
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
}