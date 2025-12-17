package com.zeezaglobal.prescription.DTO;

import lombok.Data;

@Data
public class DoctorRegistrationDTO {
    private String username;
    private String password;
    private String name;
    private String email;
    private String licenseNumber;
    private String specialization;
    private String phone;
    private String address;
    private String qualifications;
    private String hospitalName;
}
