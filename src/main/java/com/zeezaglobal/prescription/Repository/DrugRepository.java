package com.zeezaglobal.prescription.Repository;

import com.zeezaglobal.prescription.Entities.Doctor;
import com.zeezaglobal.prescription.Entities.Drug;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
@Repository
public interface DrugRepository extends JpaRepository<Drug, Long> {
    List<Drug> findByNameContainingIgnoreCase(String name);
}
