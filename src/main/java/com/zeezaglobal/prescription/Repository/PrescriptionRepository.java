package com.zeezaglobal.prescription.Repository;



import com.zeezaglobal.prescription.Entities.Prescription;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {

    // Find all prescriptions for a specific patient
    Page<Prescription> findByPatientId(Long patientId, Pageable pageable);

    // Find all prescriptions by a specific doctor
    Page<Prescription> findByDoctorId(Long doctorId, Pageable pageable);

    // Find prescriptions for a patient by a specific doctor
    Page<Prescription> findByPatientIdAndDoctorId(Long patientId, Long doctorId, Pageable pageable);

    // Find prescriptions by status
    Page<Prescription> findByStatus(Prescription.PrescriptionStatus status, Pageable pageable);

    // Find prescriptions by doctor and status
    Page<Prescription> findByDoctorIdAndStatus(Long doctorId, Prescription.PrescriptionStatus status, Pageable pageable);

    // Find active prescriptions for a patient
    @Query("SELECT p FROM Prescription p WHERE p.patient.id = :patientId AND p.status = 'ACTIVE' AND p.validUntil >= :currentDate")
    List<Prescription> findActivePrescriptionsByPatientId(@Param("patientId") Long patientId, @Param("currentDate") LocalDate currentDate);

    // Find prescriptions within a date range
    @Query("SELECT p FROM Prescription p WHERE p.doctor.id = :doctorId AND p.prescriptionDate BETWEEN :startDate AND :endDate")
    Page<Prescription> findByDoctorIdAndDateRange(
            @Param("doctorId") Long doctorId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable
    );

    // Count prescriptions by doctor
    long countByDoctorId(Long doctorId);

    // Count prescriptions by patient
    long countByPatientId(Long patientId);
}