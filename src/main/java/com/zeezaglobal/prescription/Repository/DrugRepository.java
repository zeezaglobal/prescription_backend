package com.zeezaglobal.prescription.Repository;


import com.zeezaglobal.prescription.Entities.Drug;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DrugRepository extends JpaRepository<Drug, Long> {

    Optional<Drug> findByName(String name);

    List<Drug> findByStatus(Drug.DrugStatus status);

    List<Drug> findByCategory(String category);

    @Query("SELECT d FROM Drug d WHERE d.status = :status")
    List<Drug> findAllActiveDrugs(@Param("status") Drug.DrugStatus status);

    @Query("SELECT d FROM Drug d WHERE " +
            "LOWER(d.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(d.genericName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(d.category) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Drug> searchDrugs(@Param("searchTerm") String searchTerm);

    @Query("SELECT d FROM Drug d WHERE d.status = 'ACTIVE' AND (" +
            "LOWER(d.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(d.genericName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(d.category) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    List<Drug> searchActiveDrugs(@Param("searchTerm") String searchTerm);

    @Query("SELECT DISTINCT d.category FROM Drug d ORDER BY d.category")
    List<String> findAllCategories();
}

