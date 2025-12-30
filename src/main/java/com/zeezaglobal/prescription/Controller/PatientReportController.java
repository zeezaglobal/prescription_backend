package com.zeezaglobal.prescription.Controller;

import com.zeezaglobal.prescription.DTO.PatientReportDTO;
import com.zeezaglobal.prescription.Entities.Doctor;
import com.zeezaglobal.prescription.Service.PatientReportPdfService;
import com.zeezaglobal.prescription.Service.PatientReportService;
import com.zeezaglobal.prescription.Repository.DoctorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/patient-reports")
@RequiredArgsConstructor
@Slf4j
public class PatientReportController {

    private final PatientReportService patientReportService;
    private final PatientReportPdfService patientReportPdfService;
    private final DoctorRepository doctorRepository;

    /**
     * Get full patient report with all prescriptions (JSON)
     * Doctor ID is extracted from JWT token
     * GET /api/patient-reports/{patientId}
     */
    @GetMapping("/{patientId}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<PatientReportDTO> getPatientReport(@PathVariable Long patientId) {
        try {
            Long doctorId = getAuthenticatedDoctorId();
            log.info("Fetching report for patient ID: {} by doctor ID: {}", patientId, doctorId);

            PatientReportDTO report = patientReportService.generatePatientReportByDoctor(patientId, doctorId);
            return ResponseEntity.ok(report);
        } catch (RuntimeException e) {
            log.error("Error fetching patient report: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    /**
     * Get patient report as PDF download
     * Doctor ID is extracted from JWT token
     * GET /api/patient-reports/{patientId}/pdf
     */
    @GetMapping("/{patientId}/pdf")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<byte[]> getPatientReportPdf(@PathVariable Long patientId) {
        try {
            Long doctorId = getAuthenticatedDoctorId();
            log.info("Generating PDF report for patient ID: {} by doctor ID: {}", patientId, doctorId);

            // Get the patient report data
            PatientReportDTO report = patientReportService.generatePatientReportByDoctor(patientId, doctorId);

            // Generate PDF
            byte[] pdfBytes = patientReportPdfService.generatePatientReportPdf(report);

            // Generate filename with patient name and timestamp
            String patientName = getPatientNameForFile(report);
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = String.format("Patient_Report_%s_%s.pdf", patientName, timestamp);

            // Set response headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(pdfBytes.length);

            log.info("PDF report generated successfully for patient ID: {}, size: {} bytes", patientId, pdfBytes.length);

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);

        } catch (IOException e) {
            log.error("Error generating PDF report: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (RuntimeException e) {
            log.error("Error fetching patient report for PDF: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    /**
     * Get patient report PDF for inline viewing (opens in browser)
     * GET /api/patient-reports/{patientId}/pdf/view
     */
    @GetMapping("/{patientId}/pdf/view")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<byte[]> viewPatientReportPdf(@PathVariable Long patientId) {
        try {
            Long doctorId = getAuthenticatedDoctorId();
            log.info("Generating PDF for inline view - patient ID: {} by doctor ID: {}", patientId, doctorId);

            // Get the patient report data
            PatientReportDTO report = patientReportService.generatePatientReportByDoctor(patientId, doctorId);

            // Generate PDF
            byte[] pdfBytes = patientReportPdfService.generatePatientReportPdf(report);

            // Generate filename
            String patientName = getPatientNameForFile(report);
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = String.format("Patient_Report_%s_%s.pdf", patientName, timestamp);

            // Set response headers for inline viewing
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"");
            headers.setContentLength(pdfBytes.length);

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);

        } catch (IOException e) {
            log.error("Error generating PDF report: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (RuntimeException e) {
            log.error("Error fetching patient report for PDF: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    /**
     * Extract doctor ID from the authenticated user's JWT token
     */
    private Long getAuthenticatedDoctorId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        // Find doctor by username/email
        Doctor doctor = doctorRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Doctor not found for authenticated user"));

        return doctor.getId();
    }

    /**
     * Generate a clean filename from patient name
     */
    private String getPatientNameForFile(PatientReportDTO report) {
        String name = "";

        if (report.getName() != null && !report.getName().isEmpty()) {
            name = report.getName();
        } else if (report.getFirstName() != null || report.getLastName() != null) {
            StringBuilder sb = new StringBuilder();
            if (report.getFirstName() != null) {
                sb.append(report.getFirstName());
            }
            if (report.getLastName() != null) {
                if (sb.length() > 0) sb.append("_");
                sb.append(report.getLastName());
            }
            name = sb.toString();
        } else {
            name = "Patient_" + report.getPatientId();
        }

        // Clean the name for use in filename (remove special characters)
        return name.replaceAll("[^a-zA-Z0-9]", "_").replaceAll("_+", "_");
    }
}