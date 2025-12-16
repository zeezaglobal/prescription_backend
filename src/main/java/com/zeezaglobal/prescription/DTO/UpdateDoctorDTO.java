package com.zeezaglobal.prescription.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDoctorDTO {
    private Long id;
    private String firstName;
    private String lastName;
    private String specialization;
    private String licenseNumber;
    private String hospitalName; // Kept for backward compatibility, not mapped to new entity
    private String contactNumber; // Maps to phone in entity
}