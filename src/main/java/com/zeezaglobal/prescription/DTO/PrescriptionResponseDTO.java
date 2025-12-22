package com.zeezaglobal.prescription.DTO;




import com.zeezaglobal.prescription.Entities.Prescription;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrescriptionResponseDTO {

    private Long id;
    private PatientBasicDTO patient;
    private DoctorBasicDTO doctor;
    private List<MedicationDTO> medications;
    private LocalDate prescriptionDate;
    private String status;
    private String specialInstructions;
    private LocalDate validUntil;
    private String diagnosis;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PatientBasicDTO {
        private Long id;
        private String firstName;
        private String lastName;
        private String email;
        private String phone;
        private LocalDate dateOfBirth;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DoctorBasicDTO {
        private Long id;
        private String firstName;
        private String lastName;
        private String specialization;
        private String licenseNumber;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MedicationDTO {
        private Long id;
        private DrugDTO drug;
        private String dosage;
        private String frequency;
        private String duration;
        private String instructions;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DrugDTO {
        private Long id;
        private String name;
        private String genericName;
        private String category;
    }

    // Factory method to create from entity
    public static PrescriptionResponseDTO fromEntity(Prescription prescription) {
        PrescriptionResponseDTO dto = new PrescriptionResponseDTO();
        dto.setId(prescription.getId());
        dto.setPrescriptionDate(prescription.getPrescriptionDate());
        dto.setStatus(prescription.getStatus().name());
        dto.setSpecialInstructions(prescription.getSpecialInstructions());
        dto.setValidUntil(prescription.getValidUntil());
        dto.setDiagnosis(prescription.getDiagnosis());
        dto.setCreatedAt(prescription.getCreatedAt());
        dto.setUpdatedAt(prescription.getUpdatedAt());

        // Patient info
        PatientBasicDTO patientDTO = new PatientBasicDTO();
        patientDTO.setId(prescription.getPatient().getId());
        patientDTO.setFirstName(prescription.getPatient().getFirstName());
        patientDTO.setLastName(prescription.getPatient().getLastName());
        patientDTO.setEmail(prescription.getPatient().getEmail());
        patientDTO.setPhone(prescription.getPatient().getPhone());
        patientDTO.setDateOfBirth(prescription.getPatient().getDateOfBirth());
        dto.setPatient(patientDTO);

        // Doctor info
        DoctorBasicDTO doctorDTO = new DoctorBasicDTO();
        doctorDTO.setId(prescription.getDoctor().getId());
        doctorDTO.setFirstName(prescription.getDoctor().getFirstName());
        doctorDTO.setLastName(prescription.getDoctor().getLastName());
        doctorDTO.setSpecialization(prescription.getDoctor().getSpecialization());
        doctorDTO.setLicenseNumber(prescription.getDoctor().getLicenseNumber());
        dto.setDoctor(doctorDTO);

        // Medications
        List<MedicationDTO> medicationDTOs = prescription.getMedications().stream()
                .map(med -> {
                    MedicationDTO medDTO = new MedicationDTO();
                    medDTO.setId(med.getId());
                    medDTO.setDosage(med.getDosage());
                    medDTO.setFrequency(med.getFrequency());
                    medDTO.setDuration(med.getDuration());
                    medDTO.setInstructions(med.getInstructions());

                    DrugDTO drugDTO = new DrugDTO();
                    drugDTO.setId(med.getDrug().getId());
                    drugDTO.setName(med.getDrug().getName());
                    drugDTO.setGenericName(med.getDrug().getGenericName());
                    drugDTO.setCategory(med.getDrug().getCategory());
                    medDTO.setDrug(drugDTO);

                    return medDTO;
                })
                .collect(Collectors.toList());
        dto.setMedications(medicationDTOs);

        return dto;
    }
}
