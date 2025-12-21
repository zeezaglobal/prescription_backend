package com.zeezaglobal.prescription.Controller;

import com.zeezaglobal.prescription.DTO.DoctorProfileDTO;
import com.zeezaglobal.prescription.Entities.Doctor;
import com.zeezaglobal.prescription.Entities.User;
import com.zeezaglobal.prescription.Repository.DoctorRepository;
import com.zeezaglobal.prescription.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/doctors")
public class DoctorController {

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/profile/complete")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<Map<String, Object>> completeProfile(@RequestBody DoctorProfileDTO profileDTO) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Get current authenticated user
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();

            // Find doctor
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (!(user instanceof Doctor)) {
                response.put("message", "User is not a doctor");
                return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
            }

            Doctor doctor = (Doctor) user;

            // Validate required fields
            if (profileDTO.getName() == null || profileDTO.getName().isEmpty()) {
                response.put("message", "Name is required");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            if (profileDTO.getLicenseNumber() == null || profileDTO.getLicenseNumber().isEmpty()) {
                response.put("message", "License number is required");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            if (profileDTO.getSpecialization() == null || profileDTO.getSpecialization().isEmpty()) {
                response.put("message", "Specialization is required");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            if (profileDTO.getPhone() == null || profileDTO.getPhone().isEmpty()) {
                response.put("message", "Phone number is required");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            // Check if license number already exists (excluding current doctor)
            Optional<Doctor> existingDoctor = doctorRepository.findByLicenseNumber(profileDTO.getLicenseNumber());
            if (existingDoctor.isPresent() && !existingDoctor.get().getId().equals(doctor.getId())) {
                response.put("message", "License number already exists");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            // Update doctor profile
            doctor.setName(profileDTO.getName());
            doctor.setLicenseNumber(profileDTO.getLicenseNumber());
            doctor.setSpecialization(profileDTO.getSpecialization());
            doctor.setPhone(profileDTO.getPhone());
            doctor.setAddress(profileDTO.getAddress());
            doctor.setQualifications(profileDTO.getQualifications());
            doctor.setHospitalName(profileDTO.getHospitalName());

            // Save updated doctor
            Doctor updatedDoctor = doctorRepository.save(doctor);

            // Prepare response
            Map<String, Object> doctorData = new HashMap<>();
            doctorData.put("id", updatedDoctor.getId());
            doctorData.put("name", updatedDoctor.getName());
            doctorData.put("email", updatedDoctor.getEmail());
            doctorData.put("licenseNumber", updatedDoctor.getLicenseNumber());
            doctorData.put("specialization", updatedDoctor.getSpecialization());
            doctorData.put("phone", updatedDoctor.getPhone());
            doctorData.put("address", updatedDoctor.getAddress());
            doctorData.put("qualifications", updatedDoctor.getQualifications());
            doctorData.put("hospitalName", updatedDoctor.getHospitalName());
            doctorData.put("isValidated", updatedDoctor.getValidated());
            doctorData.put("status", updatedDoctor.getStatus().toString());
            doctorData.put("profileComplete", true);

            response.put("message", "Profile completed successfully!");
            response.put("doctor", doctorData);

            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (Exception e) {
            response.put("message", "An error occurred: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/profile")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<Map<String, Object>> getProfile() {
        Map<String, Object> response = new HashMap<>();

        try {
            // Get current authenticated user
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();

            // Find doctor
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (!(user instanceof Doctor)) {
                response.put("message", "User is not a doctor");
                return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
            }

            Doctor doctor = (Doctor) user;

            // Check if profile is complete
            boolean profileComplete = doctor.getName() != null && !doctor.getName().isEmpty()
                    && !doctor.getLicenseNumber().startsWith("PENDING-")
                    && doctor.getSpecialization() != null && !doctor.getSpecialization().isEmpty()
                    && doctor.getPhone() != null && !doctor.getPhone().isEmpty();

            // Prepare response
            Map<String, Object> doctorData = new HashMap<>();
            doctorData.put("id", doctor.getId());
            doctorData.put("name", doctor.getName());
            doctorData.put("email", doctor.getEmail());
            doctorData.put("username", doctor.getUsername());
            doctorData.put("licenseNumber", doctor.getLicenseNumber());
            doctorData.put("specialization", doctor.getSpecialization());
            doctorData.put("phone", doctor.getPhone());
            doctorData.put("address", doctor.getAddress());
            doctorData.put("qualifications", doctor.getQualifications());
            doctorData.put("hospitalName", doctor.getHospitalName());
            doctorData.put("isValidated", doctor.getValidated());
            doctorData.put("status", doctor.getStatus().toString());
            doctorData.put("profileComplete", profileComplete);

            response.put("doctor", doctorData);

            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (Exception e) {
            response.put("message", "An error occurred: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/profile")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<Map<String, Object>> updateProfile(@RequestBody DoctorProfileDTO profileDTO) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Get current authenticated user
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();

            // Find doctor
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (!(user instanceof Doctor)) {
                response.put("message", "User is not a doctor");
                return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
            }

            Doctor doctor = (Doctor) user;

            // Update only provided fields (partial update)
            if (profileDTO.getName() != null && !profileDTO.getName().isEmpty()) {
                doctor.setName(profileDTO.getName());
            }

            if (profileDTO.getLicenseNumber() != null && !profileDTO.getLicenseNumber().isEmpty()) {
                // Check if license number already exists (excluding current doctor)
                Optional<Doctor> existingDoctor = doctorRepository.findByLicenseNumber(profileDTO.getLicenseNumber());
                if (existingDoctor.isPresent() && !existingDoctor.get().getId().equals(doctor.getId())) {
                    response.put("message", "License number already exists");
                    return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
                }
                doctor.setLicenseNumber(profileDTO.getLicenseNumber());
            }

            if (profileDTO.getSpecialization() != null && !profileDTO.getSpecialization().isEmpty()) {
                doctor.setSpecialization(profileDTO.getSpecialization());
            }

            if (profileDTO.getPhone() != null && !profileDTO.getPhone().isEmpty()) {
                doctor.setPhone(profileDTO.getPhone());
            }

            if (profileDTO.getAddress() != null) {
                doctor.setAddress(profileDTO.getAddress());
            }

            if (profileDTO.getQualifications() != null) {
                doctor.setQualifications(profileDTO.getQualifications());
            }

            if (profileDTO.getHospitalName() != null) {
                doctor.setHospitalName(profileDTO.getHospitalName());
            }

            // Save updated doctor
            Doctor updatedDoctor = doctorRepository.save(doctor);

            // Check if profile is complete
            boolean profileComplete = updatedDoctor.getName() != null && !updatedDoctor.getName().isEmpty()
                    && !updatedDoctor.getLicenseNumber().startsWith("PENDING-")
                    && updatedDoctor.getSpecialization() != null && !updatedDoctor.getSpecialization().isEmpty()
                    && updatedDoctor.getPhone() != null && !updatedDoctor.getPhone().isEmpty();

            // Prepare response
            Map<String, Object> doctorData = new HashMap<>();
            doctorData.put("id", updatedDoctor.getId());
            doctorData.put("name", updatedDoctor.getName());
            doctorData.put("email", updatedDoctor.getEmail());
            doctorData.put("licenseNumber", updatedDoctor.getLicenseNumber());
            doctorData.put("specialization", updatedDoctor.getSpecialization());
            doctorData.put("phone", updatedDoctor.getPhone());
            doctorData.put("address", updatedDoctor.getAddress());
            doctorData.put("qualifications", updatedDoctor.getQualifications());
            doctorData.put("hospitalName", updatedDoctor.getHospitalName());
            doctorData.put("isValidated", updatedDoctor.getValidated());
            doctorData.put("status", updatedDoctor.getStatus().toString());
            doctorData.put("profileComplete", profileComplete);

            response.put("message", "Profile updated successfully!");
            response.put("doctor", doctorData);

            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (Exception e) {
            response.put("message", "An error occurred: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}