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

    List<Prescription> findByPatientId(Long patientId);

    Page<Prescription> findByPatientId(Long patientId, Pageable pageable);

    List<Prescription> findByDoctorId(Long doctorId);

    Page<Prescription> findByDoctorId(Long doctorId, Pageable pageable);

    List<Prescription> findByStatus(Prescription.PrescriptionStatus status);

    Page<Prescription> findByStatus(Prescription.PrescriptionStatus status, Pageable pageable);

    @Query("SELECT p FROM Prescription p WHERE p.patient.id = :patientId AND p.status = :status")
    List<Prescription> findByPatientIdAndStatus(@Param("patientId") Long patientId,
                                                @Param("status") Prescription.PrescriptionStatus status);

    @Query("SELECT p FROM Prescription p WHERE p.prescriptionDate BETWEEN :startDate AND :endDate")
    List<Prescription> findByDateRange(@Param("startDate") LocalDate startDate,
                                       @Param("endDate") LocalDate endDate);

    @Query("SELECT p FROM Prescription p WHERE " +
            "LOWER(p.patient.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "CAST(p.id AS string) LIKE CONCAT('%', :searchTerm, '%')")
    Page<Prescription> searchPrescriptions(@Param("searchTerm") String searchTerm, Pageable pageable);

    @Query("SELECT p FROM Prescription p JOIN p.medications pm WHERE pm.drug.id = :drugId")
    List<Prescription> findByDrugId(@Param("drugId") Long drugId);

    @Query("SELECT COUNT(p) FROM Prescription p WHERE p.status = :status")
    Long countByStatus(@Param("status") Prescription.PrescriptionStatus status);

    @Query("SELECT COUNT(p) FROM Prescription p WHERE p.prescriptionDate >= :startDate")
    Long countByDateAfter(@Param("startDate") LocalDate startDate);

    @Query("SELECT p FROM Prescription p WHERE p.validUntil < :date AND p.status = 'ACTIVE'")
    List<Prescription> findExpiredPrescriptions(@Param("date") LocalDate date);
}
