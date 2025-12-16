package com.zeezaglobal.prescription.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrescriptionRequestDTO {
    private Long patientId;
    private Long doctorId;
    private LocalDate prescriptionDate;
    private String specialInstructions;
    private String diagnosis;
    private List<MedicationDTO> medications;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MedicationDTO {
        private Long drugId;
        private String dosage;
        private String frequency;
        private String duration;
        private String instructions;
    }
}