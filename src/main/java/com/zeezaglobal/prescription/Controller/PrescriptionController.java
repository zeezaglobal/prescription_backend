package com.zeezaglobal.prescription.Controller;

import com.zeezaglobal.prescription.DTO.PrescriptionRequestDTO;
import com.zeezaglobal.prescription.DTO.PrescriptionResponseDTO;
import com.zeezaglobal.prescription.Entities.Prescription;
import com.zeezaglobal.prescription.Service.PrescriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/prescriptions")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PrescriptionController {

    private final PrescriptionService prescriptionService;

    @GetMapping
    public ResponseEntity<List<PrescriptionResponseDTO>> getAllPrescriptions() {
        return ResponseEntity.ok(prescriptionService.getAllPrescriptions());
    }

    @GetMapping("/paged")
    public ResponseEntity<Page<PrescriptionResponseDTO>> getAllPrescriptionsPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc") ?
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return ResponseEntity.ok(prescriptionService.getAllPrescriptionsPaged(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PrescriptionResponseDTO> getPrescriptionById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(prescriptionService.getPrescriptionById(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<PrescriptionResponseDTO>> getPrescriptionsByPatientId(
            @PathVariable Long patientId) {
        return ResponseEntity.ok(prescriptionService.getPrescriptionsByPatientId(patientId));
    }

    @GetMapping("/patient/{patientId}/paged")
    public ResponseEntity<Page<PrescriptionResponseDTO>> getPrescriptionsByPatientIdPaged(
            @PathVariable Long patientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("prescriptionDate").descending());
        return ResponseEntity.ok(prescriptionService.getPrescriptionsByPatientIdPaged(patientId, pageable));
    }

    @GetMapping("/doctor/{doctorId}")
    public ResponseEntity<List<PrescriptionResponseDTO>> getPrescriptionsByDoctorId(
            @PathVariable Long doctorId) {
        return ResponseEntity.ok(prescriptionService.getPrescriptionsByDoctorId(doctorId));
    }

    @GetMapping("/doctor/{doctorId}/paged")
    public ResponseEntity<Page<PrescriptionResponseDTO>> getPrescriptionsByDoctorIdPaged(
            @PathVariable Long doctorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("prescriptionDate").descending());
        return ResponseEntity.ok(prescriptionService.getPrescriptionsByDoctorIdPaged(doctorId, pageable));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<PrescriptionResponseDTO>> getPrescriptionsByStatus(
            @PathVariable Prescription.PrescriptionStatus status) {
        return ResponseEntity.ok(prescriptionService.getPrescriptionsByStatus(status));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<PrescriptionResponseDTO>> searchPrescriptions(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("prescriptionDate").descending());
        return ResponseEntity.ok(prescriptionService.searchPrescriptions(query, pageable));
    }

    @GetMapping("/date-range")
    public ResponseEntity<List<PrescriptionResponseDTO>> getPrescriptionsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(prescriptionService.getPrescriptionsByDateRange(startDate, endDate));
    }

    @PostMapping
    public ResponseEntity<PrescriptionResponseDTO> createPrescription(
            @RequestBody PrescriptionRequestDTO requestDTO) {
        try {
            PrescriptionResponseDTO createdPrescription = prescriptionService.createPrescription(requestDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdPrescription);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<PrescriptionResponseDTO> updatePrescription(
            @PathVariable Long id,
            @RequestBody PrescriptionRequestDTO requestDTO) {
        try {
            PrescriptionResponseDTO updatedPrescription = prescriptionService.updatePrescription(id, requestDTO);
            return ResponseEntity.ok(updatedPrescription);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<PrescriptionResponseDTO> updatePrescriptionStatus(
            @PathVariable Long id,
            @RequestParam Prescription.PrescriptionStatus status) {
        try {
            PrescriptionResponseDTO updatedPrescription = prescriptionService.updatePrescriptionStatus(id, status);
            return ResponseEntity.ok(updatedPrescription);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePrescription(@PathVariable Long id) {
        try {
            prescriptionService.deletePrescription(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getPrescriptionStats() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("total", prescriptionService.getTotalPrescriptions());
        stats.put("active", prescriptionService.getActivePrescriptions());
        stats.put("pending", prescriptionService.getPendingPrescriptions());
        stats.put("thisMonth", prescriptionService.getPrescriptionsThisMonth());
        return ResponseEntity.ok(stats);
    }

    @PostMapping("/update-expired")
    public ResponseEntity<Void> updateExpiredPrescriptions() {
        prescriptionService.updateExpiredPrescriptions();
        return ResponseEntity.ok().build();
    }
}
