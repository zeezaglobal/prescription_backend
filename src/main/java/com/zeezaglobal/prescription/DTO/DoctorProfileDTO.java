package com.zeezaglobal.prescription.DTO;

import lombok.Data;

@Data
public class DoctorProfileDTO {
    private String name;
    private String licenseNumber;
    private String specialization;
    private String phone;
    private String address;
    private String qualifications;
    private String hospitalName;
}
