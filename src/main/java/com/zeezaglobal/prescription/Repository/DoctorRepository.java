package com.zeezaglobal.prescription.Repository;

import com.zeezaglobal.prescription.Entities.Doctor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {




    Optional<Doctor> findByUsername(String username);
    Optional<Doctor> findByEmail(String email);
    Optional<Doctor> findByLicenseNumber(String licenseNumber);
    boolean existsByEmail(String email);
    boolean existsByLicenseNumber(String licenseNumber);
    Page<Doctor> findByNameContainingIgnoreCase(String name, Pageable pageable);
    List<Doctor> findByStatus(Doctor.DoctorStatus status);

    List<Doctor> findBySpecialization(String specialization);

    @Query("SELECT d FROM Doctor d WHERE d.status = 'ACTIVE'")
    List<Doctor> findAllActiveDoctors();

    @Query("SELECT d FROM Doctor d WHERE " +
            "LOWER(d.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(d.specialization) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Doctor> searchDoctors(@Param("searchTerm") String searchTerm);
}
