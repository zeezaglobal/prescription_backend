package com.zeezaglobal.prescription.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.time.LocalDate;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreatePrescriptionDTO {


    private Long patientId;


    private List<PrescriptionMedicationDTO> medications;

    private LocalDate prescriptionDate;

    private String specialInstructions;

    private LocalDate validUntil;

    private String diagnosis;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PrescriptionMedicationDTO {


        private Long drugId;


        private String dosage;


        private String frequency;


        private String duration;

        private String instructions;
    }
}
