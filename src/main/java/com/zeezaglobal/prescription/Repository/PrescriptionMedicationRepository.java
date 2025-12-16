package com.zeezaglobal.prescription.Repository;


import com.zeezaglobal.prescription.Entities.PrescriptionMedication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PrescriptionMedicationRepository extends JpaRepository<PrescriptionMedication, Long> {

    List<PrescriptionMedication> findByPrescriptionId(Long prescriptionId);

    List<PrescriptionMedication> findByDrugId(Long drugId);

    @Query("SELECT pm FROM PrescriptionMedication pm WHERE pm.prescription.id = :prescriptionId")
    List<PrescriptionMedication> findMedicationsByPrescription(@Param("prescriptionId") Long prescriptionId);

    @Query("SELECT COUNT(pm) FROM PrescriptionMedication pm WHERE pm.drug.id = :drugId")
    Long countByDrugId(@Param("drugId") Long drugId);
}
