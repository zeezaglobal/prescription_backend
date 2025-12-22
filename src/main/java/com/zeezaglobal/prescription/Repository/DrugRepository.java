package com.zeezaglobal.prescription.Repository;




import com.zeezaglobal.prescription.Entities.Drug;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DrugRepository extends JpaRepository<Drug, Long> {

    // Find drug by name
    Optional<Drug> findByName(String name);

    // Find drug by generic name
    Optional<Drug> findByGenericName(String genericName);

    // Find drugs by category
    Page<Drug> findByCategory(String category, Pageable pageable);

    // Find drugs by status
    Page<Drug> findByStatus(Drug.DrugStatus status, Pageable pageable);

    // Search drugs by name or generic name (case-insensitive)
    @Query("SELECT d FROM Drug d WHERE LOWER(d.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR LOWER(d.genericName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<Drug> searchByNameOrGenericName(@Param("searchTerm") String searchTerm, Pageable pageable);

    // Find active drugs only
    List<Drug> findByStatus(Drug.DrugStatus status);

    // Check if drug name exists
    boolean existsByName(String name);

    // Get all categories (distinct)
    @Query("SELECT DISTINCT d.category FROM Drug d ORDER BY d.category")
    List<String> findAllCategories();
}

