package com.zeezaglobal.prescription.Entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "doctors")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "password", "authorities", "accountNonExpired", "accountNonLocked", "credentialsNonExpired", "enabled"})
public class Doctor extends User {

    @Column(nullable = true)  // Changed to nullable for initial registration
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = true, unique = true)  // Changed to nullable, will be set during profile completion
    private String licenseNumber;

    @Column(nullable = true)  // Changed to nullable for initial registration
    private String specialization;

    @Column(nullable = true)  // Changed to nullable for initial registration
    private String phone;

    @Column(length = 500)
    private String address;

    @Column(length = 500)
    private String qualifications;

    @Column(nullable = false)
    private boolean emailVerified = false;

    @Column(name = "hospital_name")
    private String hospitalName;

    @Column(name = "stripe_username")
    private String stripeUsername;

    @Column(name = "validated")
    private Integer validated = 0; // 0 = not validated, 1 = validated

    @Enumerated(EnumType.STRING)
    private DoctorStatus status = DoctorStatus.ACTIVE;

    @OneToMany(mappedBy = "doctor", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties({"doctor", "password"})
    private List<Patient> patients = new ArrayList<>();

    @PrePersist
    @Override
    protected void onCreate() {
        super.onCreate();
        setUserType(UserType.DOCTOR);
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

    // Helper methods for patient relationship
    public void addPatient(Patient patient) {
        patients.add(patient);
        patient.setDoctor(this);
    }

    public void removePatient(Patient patient) {
        patients.remove(patient);
        patient.setDoctor(null);
    }

    // Check if profile is complete
    public boolean isProfileComplete() {
        return name != null && !name.isEmpty()
                && licenseNumber != null && !licenseNumber.startsWith("PENDING-")
                && specialization != null && !specialization.isEmpty()
                && phone != null && !phone.isEmpty();
    }

    public enum DoctorStatus {
        ACTIVE,
        INACTIVE,
        SUSPENDED
    }
}