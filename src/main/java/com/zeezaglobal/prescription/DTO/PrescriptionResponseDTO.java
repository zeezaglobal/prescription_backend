package com.zeezaglobal.prescription.DTO;


import com.zeezaglobal.prescription.Entities.Prescription;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrescriptionResponseDTO {
    private Long id;
    private PatientSummary patient;
    private DoctorSummary doctor;
    private List<MedicationDetail> medications;
    private LocalDate prescriptionDate;
    private Prescription.PrescriptionStatus status;
    private String specialInstructions;
    private LocalDate validUntil;
    private String diagnosis;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PatientSummary {
        private Long id;
        private String name;
        private Integer age;
        private String phone;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DoctorSummary {
        private Long id;
        private String name;
        private String licenseNumber;
        private String specialization;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MedicationDetail {
        private Long id;
        private DrugInfo drug;
        private String dosage;
        private String frequency;
        private String duration;
        private String instructions;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DrugInfo {
        private Long id;
        private String name;
        private String genericName;
        private String category;
    }
}
