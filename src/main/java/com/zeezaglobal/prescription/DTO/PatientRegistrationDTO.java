package com.zeezaglobal.prescription.DTO;

import lombok.Data;
import java.time.LocalDate;

@Data
public class PatientRegistrationDTO {
    private String username;
    private String password;
    private String name;
    private LocalDate dateOfBirth;
    private String gender;
    private String phone;
    private String email;
    private String address;
    private String bloodGroup;
    private String medicalHistory;
    private String allergies;
}