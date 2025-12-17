package com.zeezaglobal.prescription.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PatientDTO {
    private Long id;
    private Integer numberOfVisit;
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private String gender;
    private String contactNumber;
    private String email;
    private String address;
    private String medicalHistory;
    private Long doctorId;
    private String bloodGroup;
    private String allergies;
    private Integer age;

    // Constructor for backward compatibility
    public PatientDTO(Long id, Integer numberOfVisit, String firstName, String lastName,
                      LocalDate dateOfBirth, String gender, String contactNumber,
                      String email, String address, String medicalHistory, Long doctorId) {
        this.id = id;
        this.numberOfVisit = numberOfVisit;
        this.firstName = firstName;
        this.lastName = lastName;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
        this.contactNumber = contactNumber;
        this.email = email;
        this.address = address;
        this.medicalHistory = medicalHistory;
        this.doctorId = doctorId;
    }

    // Full name helper
    public String getFullName() {
        return firstName + " " + lastName;
    }
}