package com.zeezaglobal.prescription.Controller;

import com.zeezaglobal.prescription.DTO.CreatePrescriptionDTO;
import com.zeezaglobal.prescription.DTO.PrescriptionResponseDTO;
import com.zeezaglobal.prescription.DTO.UpdatePrescriptionStatusDTO;
import com.zeezaglobal.prescription.Services.PrescriptionService;
import com.zeezaglobal.prescription.Utils.JwtUtil;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/prescriptions")
@RequiredArgsConstructor
public class PrescriptionController {

    private final PrescriptionService prescriptionService;
    private final JwtUtil jwtUtil;

    /**
     * Create a new prescription (Doctors only)
     * POST /api/prescriptions
     */
    @GetMapping("/test-token")
    public ResponseEntity<?> testToken(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7);

            // Extract all claims
            Long userId = jwtUtil.extractUserId(token);
            String role = jwtUtil.extractRole(token);
            String username = jwtUtil.extractUsername(token);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("userId", userId);
            response.put("role", role);
            response.put("username", username);
            response.put("token", token);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }
    @PostMapping
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<?> createPrescription(
            @RequestBody CreatePrescriptionDTO dto,
            @RequestHeader("Authorization") String authHeader) {
        try {
            Long doctorId = extractUserIdFromToken(authHeader);
            PrescriptionResponseDTO response = prescriptionService.createPrescription(dto, doctorId);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * Get a specific prescription by ID (Doctors and Patients can view)
     * GET /api/prescriptions/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'PATIENT')")
    public ResponseEntity<?> getPrescriptionById(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        try {
            Long userId = extractUserIdFromToken(authHeader);
            String role = extractRoleFromToken(authHeader);

            PrescriptionResponseDTO response;
            if ("DOCTOR".equals(role)) {
                response = prescriptionService.getPrescriptionById(id, userId);
            } else {
                // For patients, verify they own this prescription
                response = prescriptionService.getPrescriptionByIdForPatient(id, userId);
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * Get all prescriptions by the authenticated doctor (Doctors only)
     * GET /api/prescriptions?page=0&size=10&sort=prescriptionDate,desc
     */
    @GetMapping
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<?> getAllPrescriptions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "prescriptionDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection,
            @RequestHeader("Authorization") String authHeader) {
        try {
            Long doctorId = extractUserIdFromToken(authHeader);

            Sort.Direction direction = sortDirection.equalsIgnoreCase("asc")
                    ? Sort.Direction.ASC
                    : Sort.Direction.DESC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

            Page<PrescriptionResponseDTO> prescriptions = prescriptionService.getAllPrescriptionsByDoctor(doctorId, pageable);
            return ResponseEntity.ok(createPageResponse(prescriptions));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * Get all prescriptions for a specific patient (Doctors and the Patient themselves)
     * GET /api/prescriptions/patient/{patientId}?page=0&size=10
     */
    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'PATIENT')")
    public ResponseEntity<?> getPrescriptionsByPatient(
            @PathVariable Long patientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "prescriptionDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection,
            @RequestHeader("Authorization") String authHeader) {
        try {
            Long userId = extractUserIdFromToken(authHeader);
            String role = extractRoleFromToken(authHeader);

            // If patient, verify they're requesting their own prescriptions
            if ("PATIENT".equals(role) && !userId.equals(patientId)) {
                throw new RuntimeException("Patients can only view their own prescriptions");
            }

            Sort.Direction direction = sortDirection.equalsIgnoreCase("asc")
                    ? Sort.Direction.ASC
                    : Sort.Direction.DESC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

            Page<PrescriptionResponseDTO> prescriptions;
            if ("DOCTOR".equals(role)) {
                prescriptions = prescriptionService.getPrescriptionsByPatient(patientId, userId, pageable);
            } else {
                prescriptions = prescriptionService.getPrescriptionsByPatientId(patientId, pageable);
            }

            return ResponseEntity.ok(createPageResponse(prescriptions));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * Get active prescriptions for a specific patient (Doctors and the Patient themselves)
     * GET /api/prescriptions/patient/{patientId}/active
     */
    @GetMapping("/patient/{patientId}/active")
    @PreAuthorize("hasAnyRole('DOCTOR', 'PATIENT')")
    public ResponseEntity<?> getActivePrescriptionsForPatient(
            @PathVariable Long patientId,
            @RequestHeader("Authorization") String authHeader) {
        try {
            Long userId = extractUserIdFromToken(authHeader);
            String role = extractRoleFromToken(authHeader);

            // If patient, verify they're requesting their own prescriptions
            if ("PATIENT".equals(role) && !userId.equals(patientId)) {
                throw new RuntimeException("Patients can only view their own prescriptions");
            }

            List<PrescriptionResponseDTO> prescriptions;
            if ("DOCTOR".equals(role)) {
                prescriptions = prescriptionService.getActivePrescriptionsForPatient(patientId, userId);
            } else {
                prescriptions = prescriptionService.getActivePrescriptionsForPatientOnly(patientId);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", prescriptions);
            response.put("count", prescriptions.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * Get prescriptions by status (Doctors only)
     * GET /api/prescriptions/status/{status}?page=0&size=10
     */
    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<?> getPrescriptionsByStatus(
            @PathVariable String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "prescriptionDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection,
            @RequestHeader("Authorization") String authHeader) {
        try {
            Long doctorId = extractUserIdFromToken(authHeader);

            Sort.Direction direction = sortDirection.equalsIgnoreCase("asc")
                    ? Sort.Direction.ASC
                    : Sort.Direction.DESC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

            Page<PrescriptionResponseDTO> prescriptions = prescriptionService.getPrescriptionsByStatus(doctorId, status, pageable);
            return ResponseEntity.ok(createPageResponse(prescriptions));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * Update prescription status (Doctors only)
     * PATCH /api/prescriptions/{id}/status
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<?> updatePrescriptionStatus(
            @PathVariable Long id,
            @RequestBody UpdatePrescriptionStatusDTO dto,
            @RequestHeader("Authorization") String authHeader) {
        try {
            Long doctorId = extractUserIdFromToken(authHeader);
            PrescriptionResponseDTO response = prescriptionService.updatePrescriptionStatus(id, dto, doctorId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * Delete a prescription (Doctors only)
     * DELETE /api/prescriptions/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<?> deletePrescription(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        try {
            Long doctorId = extractUserIdFromToken(authHeader);
            prescriptionService.deletePrescription(id, doctorId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Prescription deleted successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * Get prescription statistics for the authenticated doctor (Doctors only)
     * GET /api/prescriptions/stats
     */
    @GetMapping("/stats")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<?> getPrescriptionStats(@RequestHeader("Authorization") String authHeader) {
        try {
            Long doctorId = extractUserIdFromToken(authHeader);
            long totalPrescriptions = prescriptionService.getTotalPrescriptionsByDoctor(doctorId);

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalPrescriptions", totalPrescriptions);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", stats);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * Get prescription count for a specific patient (Doctors and the Patient themselves)
     * GET /api/prescriptions/patient/{patientId}/count
     */
    @GetMapping("/patient/{patientId}/count")
    @PreAuthorize("hasAnyRole('DOCTOR', 'PATIENT')")
    public ResponseEntity<?> getPatientPrescriptionCount(
            @PathVariable Long patientId,
            @RequestHeader("Authorization") String authHeader) {
        try {
            Long userId = extractUserIdFromToken(authHeader);
            String role = extractRoleFromToken(authHeader);

            // If patient, verify they're requesting their own count
            if ("PATIENT".equals(role) && !userId.equals(patientId)) {
                throw new RuntimeException("Patients can only view their own prescription count");
            }

            long count = prescriptionService.getTotalPrescriptionsForPatient(patientId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("patientId", patientId);
            response.put("totalPrescriptions", count);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    // Helper method to extract user ID from JWT token
    private Long extractUserIdFromToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Invalid authorization header");
        }
        String token = authHeader.substring(7);
        Long userId = jwtUtil.extractUserId(token);

        if (userId == null) {
            throw new RuntimeException("User ID not found in token. Please login again.");
        }

        return userId;
    }

    // Helper method to extract role from JWT token
    private String extractRoleFromToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Invalid authorization header");
        }
        String token = authHeader.substring(7);
        String role = jwtUtil.extractRole(token);

        if (role == null) {
            throw new RuntimeException("Role not found in token. Please login again.");
        }

        return role;
    }

    // Helper method to create error response
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("error", message);
        return error;
    }

    // Helper method to create paginated response
    private Map<String, Object> createPageResponse(Page<PrescriptionResponseDTO> page) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", page.getContent());
        response.put("currentPage", page.getNumber());
        response.put("totalPages", page.getTotalPages());
        response.put("totalElements", page.getTotalElements());
        response.put("pageSize", page.getSize());
        response.put("hasNext", page.hasNext());
        response.put("hasPrevious", page.hasPrevious());
        return response;
    }
}