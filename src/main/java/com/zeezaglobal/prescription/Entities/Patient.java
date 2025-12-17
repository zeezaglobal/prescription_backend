package com.zeezaglobal.prescription.Entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDate;
import java.time.Period;

@Entity
@Table(name = "patients")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "password", "authorities", "accountNonExpired", "accountNonLocked", "credentialsNonExpired", "enabled"})
public class Patient extends User {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private LocalDate dateOfBirth;

    @Column(nullable = false)
    private String gender;

    @Column(nullable = false)
    private String phone;

    @Column
    private String email;

    @Column(length = 500)
    private String address;

    @Column(name = "blood_group")
    private String bloodGroup;

    @Column(length = 1000)
    private String medicalHistory;

    @Column(length = 500)
    private String allergies;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id")
    @JsonIgnoreProperties({"patients", "password"})
    private Doctor doctor;

    @Column(name = "number_of_visit")
    private Integer numberOfVisit = 0;

    @PrePersist
    @Override
    protected void onCreate() {
        super.onCreate();
        setUserType(UserType.PATIENT);
        if (numberOfVisit == null) {
            numberOfVisit = 0;
        }
    }

    // Calculate age from date of birth
    public Integer getAge() {
        if (dateOfBirth == null) {
            return null;
        }
        return Period.between(dateOfBirth, LocalDate.now()).getYears();
    }

    // Helper methods for backward compatibility
    public String getFirstName() {
        if (name != null && name.contains(" ")) {
            return name.substring(0, name.indexOf(" "));
        }
        return name;
    }

    public String getLastName() {
        if (name != null && name.contains(" ")) {
            return name.substring(name.indexOf(" ") + 1);
        }
        return "";
    }

    public String getContactNumber() {
        return phone;
    }

    public void setContactNumber(String contactNumber) {
        this.phone = contactNumber;
    }

    public void setFirstName(String firstName) {
        if (this.name == null) {
            this.name = firstName;
        } else {
            String lastName = getLastName();
            this.name = firstName + (lastName.isEmpty() ? "" : " " + lastName);
        }
    }

    public void setLastName(String lastName) {
        String firstName = getFirstName();
        this.name = firstName + (lastName.isEmpty() ? "" : " " + lastName);
    }
}