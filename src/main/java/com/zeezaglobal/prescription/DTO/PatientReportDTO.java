package com.zeezaglobal.prescription.DTO;



import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientReportDTO {

    // Patient Information
    private Long patientId;
    private String name;
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private Integer age;
    private String gender;
    private String phone;
    private String email;
    private String address;
    private String bloodGroup;
    private String medicalHistory;
    private String allergies;
    private Integer numberOfVisits;
    private LocalDateTime registeredDate;

    // Doctor Information
    private DoctorSummary assignedDoctor;

    // Prescription History
    private List<PrescriptionSummary> prescriptions;
    private Integer totalPrescriptions;

    // Statistics
    private PatientStatistics statistics;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DoctorSummary {
        private Long doctorId;
        private String name;
        private String specialization;
        private String phone;
        private String email;
        private String clinicName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PrescriptionSummary {
        private Long prescriptionId;
        private LocalDate prescriptionDate;
        private LocalDate validUntil;
        private String diagnosis;
        private String specialInstructions;
        private String status;
        private List<MedicationItem> medications;
        private String prescribedBy;
        private Long prescribedByDoctorId;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MedicationItem {
        private Long medicationId;
        private String medicineName;
        private String dosage;
        private String frequency;
        private String duration;
        private String instructions;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PatientStatistics {
        private Integer totalVisits;
        private Integer totalPrescriptions;
        private LocalDate firstVisitDate;
        private LocalDate lastVisitDate;
        private List<String> frequentDiagnoses;
        private List<String> frequentMedications;
        private Integer pendingPrescriptions;
        private Integer activePrescriptions;
        private Integer completedPrescriptions;
        private Integer cancelledPrescriptions;
        private Integer expiredPrescriptions;
    }
}