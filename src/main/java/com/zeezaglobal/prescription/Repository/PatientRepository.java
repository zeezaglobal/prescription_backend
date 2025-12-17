package com.zeezaglobal.prescription.Repository;

import com.zeezaglobal.prescription.Entities.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {
    Optional<Patient> findByUsername(String username);
    Optional<Patient> findByEmail(String email);
    Optional<Patient> findByPhone(String phone);
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);

    // Search by name (supports both first name and last name)
    Page<Patient> findByNameContainingIgnoreCase(String name, Pageable pageable);

    // Find patients by doctor ID
    Page<Patient> findByDoctorId(Long doctorId, Pageable pageable);
    List<Patient> findByDoctorId(Long doctorId);

    // Search patients with multiple criteria
    @Query("SELECT p FROM Patient p WHERE " +
            "LOWER(p.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(p.phone) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(p.email) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Patient> searchPatients(@Param("searchTerm") String searchTerm);

    // Search by first name, last name or contact number (backward compatibility)
    @Query("SELECT p FROM Patient p WHERE " +
            "LOWER(p.name) LIKE LOWER(CONCAT('%', :firstName, '%')) OR " +
            "LOWER(p.name) LIKE LOWER(CONCAT('%', :lastName, '%')) OR " +
            "LOWER(p.phone) LIKE LOWER(CONCAT('%', :contactNumber, '%'))")
    List<Patient> findByFirstNameContainingOrLastNameContainingOrContactNumberContaining(
            @Param("firstName") String firstName,
            @Param("lastName") String lastName,
            @Param("contactNumber") String contactNumber
    );

    // Search patients by doctor and search term
    @Query("SELECT p FROM Patient p WHERE p.doctor.id = :doctorId AND " +
            "(LOWER(p.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(p.phone) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(p.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<Patient> searchPatientsByDoctor(
            @Param("doctorId") Long doctorId,
            @Param("searchTerm") String searchTerm,
            Pageable pageable
    );
}