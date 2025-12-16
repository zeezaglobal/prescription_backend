package com.zeezaglobal.prescription.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DoctorDTO {
    private Long id;
    private String name;
    private String lastName; // Kept for backward compatibility
    private String specialization;
    private String licenseNumber;
    private String hospitalName; // Kept for backward compatibility
    private String contactNumber; // Maps to phone in entity
    private String stripeUsername; // Kept for backward compatibility
}