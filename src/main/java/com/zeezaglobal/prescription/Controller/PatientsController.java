package com.zeezaglobal.prescription.Controller;

import com.zeezaglobal.prescription.DTO.PatientDTO;
import com.zeezaglobal.prescription.Entities.Patient;
import com.zeezaglobal.prescription.Service.PatientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/patients")
public class PatientsController {

    @Autowired
    private PatientService patientService;



    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'PATIENT')")
    public ResponseEntity<?> getPatientById(@PathVariable Long id) {
        Optional<Patient> patient = patientService.getPatientById(id);

        if (patient.isPresent()) {
            return ResponseEntity.ok(patient.get());
        } else {
            Map<String, String> response = new HashMap<>();
            response.put("message", "Patient not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    @GetMapping("/doctor")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<Map<String, Object>> getPatientsByDoctor(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection,
            Authentication authentication) {

        Sort.Direction direction = sortDirection.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        // Extract doctor username from JWT token
        String username = authentication.getName();
        Page<PatientDTO> patientsPage = patientService.getPatientsByAuthenticatedDoctor(username, pageable);

        Map<String, Object> response = new HashMap<>();
        response.put("patients", patientsPage.getContent());
        response.put("currentPage", patientsPage.getNumber());
        response.put("totalItems", patientsPage.getTotalElements());
        response.put("totalPages", patientsPage.getTotalPages());

        return ResponseEntity.ok(response);
    }



    @PostMapping
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<Map<String, Object>> createPatient(
            @RequestBody Patient patient,
            Authentication authentication) {
        try {
            // Extract doctor ID from the authenticated user
            String username = authentication.getName();
            Patient savedPatient = patientService.createPatientForAuthenticatedDoctor(patient, username);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Patient created successfully");
            response.put("patient", savedPatient);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Error creating patient: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }



    @PutMapping("/{id}")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<Map<String, Object>> updatePatient(
            @PathVariable Long id,
            @RequestBody Patient patient) {
        try {
            Optional<Patient> existingPatient = patientService.getPatientById(id);

            if (existingPatient.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("message", "Patient not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            patient.setId(id);
            Patient updatedPatient = patientService.savePatient(patient);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Patient updated successfully");
            response.put("patient", updatedPatient);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Error updating patient: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }


    @GetMapping("/search")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<?> searchPatients(
            @RequestParam(required = false) String searchTerm,
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) String contactNumber) {

        try {
            List<Patient> patients;

            // If single search term provided
            if (searchTerm != null && !searchTerm.isEmpty()) {
                patients = patientService.searchPatients(searchTerm);
            }
            // If individual fields provided (backward compatibility)
            else if ((firstName != null && !firstName.isEmpty()) ||
                    (lastName != null && !lastName.isEmpty()) ||
                    (contactNumber != null && !contactNumber.isEmpty())) {
                patients = patientService.searchPatients(firstName, lastName, contactNumber);
            }
            // No search criteria provided
            else {
                Map<String, String> response = new HashMap<>();
                response.put("message", "Please provide search criteria");
                return ResponseEntity.badRequest().body(response);
            }

            return ResponseEntity.ok(patients);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("message", "Error searching patients: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/search/doctor")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<Map<String, Object>> searchPatientsByDoctor(
            @RequestParam String searchTerm,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {

        try {
            // Extract doctor username from JWT token
            String username = authentication.getName();

            Pageable pageable = PageRequest.of(page, size);
            Page<PatientDTO> patientsPage = patientService.searchPatientsByAuthenticatedDoctor(username, searchTerm, pageable);

            Map<String, Object> response = new HashMap<>();
            response.put("patients", patientsPage.getContent());
            response.put("currentPage", patientsPage.getNumber());
            response.put("totalItems", patientsPage.getTotalElements());
            response.put("totalPages", patientsPage.getTotalPages());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Error searching patients: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }



    @PatchMapping("/{id}/increment-visit")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<Map<String, String>> incrementPatientVisit(@PathVariable Long id) {
        try {
            patientService.incrementPatientVisit(id);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Patient visit count incremented successfully");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> response = new HashMap<>();
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("message", "Error incrementing visit: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}