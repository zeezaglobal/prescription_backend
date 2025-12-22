package com.zeezaglobal.prescription.Service;




import com.zeezaglobal.prescription.DTO.CreateDrugDTO;
import com.zeezaglobal.prescription.DTO.DrugResponseDTO;
import com.zeezaglobal.prescription.DTO.UpdateDrugDTO;
import com.zeezaglobal.prescription.Entities.Drug;
import com.zeezaglobal.prescription.Repository.DrugRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DrugService {

    private final DrugRepository drugRepository;

    @Transactional
    public DrugResponseDTO createDrug(CreateDrugDTO dto) {
        // Check if drug with same name already exists
        if (drugRepository.existsByName(dto.getName())) {
            throw new RuntimeException("Drug with name '" + dto.getName() + "' already exists");
        }

        Drug drug = new Drug();
        drug.setName(dto.getName());
        drug.setGenericName(dto.getGenericName());
        drug.setCategory(dto.getCategory());
        drug.setCommonDosages(dto.getCommonDosages() != null ? dto.getCommonDosages() : List.of());
        drug.setDescription(dto.getDescription());
        drug.setSideEffects(dto.getSideEffects());
        drug.setContraindications(dto.getContraindications());
        drug.setManufacturer(dto.getManufacturer());
        drug.setStatus(Drug.DrugStatus.ACTIVE);

        Drug savedDrug = drugRepository.save(drug);
        return DrugResponseDTO.fromEntity(savedDrug);
    }

    @Transactional(readOnly = true)
    public DrugResponseDTO getDrugById(Long id) {
        Drug drug = drugRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Drug not found with id: " + id));
        return DrugResponseDTO.fromEntity(drug);
    }

    @Transactional(readOnly = true)
    public Page<DrugResponseDTO> getAllDrugs(Pageable pageable) {
        Page<Drug> drugs = drugRepository.findAll(pageable);
        return drugs.map(DrugResponseDTO::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<DrugResponseDTO> searchDrugs(String searchTerm, Pageable pageable) {
        Page<Drug> drugs = drugRepository.searchByNameOrGenericName(searchTerm, pageable);
        return drugs.map(DrugResponseDTO::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<DrugResponseDTO> getDrugsByCategory(String category, Pageable pageable) {
        Page<Drug> drugs = drugRepository.findByCategory(category, pageable);
        return drugs.map(DrugResponseDTO::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<DrugResponseDTO> getActiveDrugs(Pageable pageable) {
        Page<Drug> drugs = drugRepository.findByStatus(Drug.DrugStatus.ACTIVE, pageable);
        return drugs.map(DrugResponseDTO::fromEntity);
    }

    @Transactional(readOnly = true)
    public List<String> getAllCategories() {
        return drugRepository.findAllCategories();
    }

    @Transactional
    public DrugResponseDTO updateDrug(Long id, UpdateDrugDTO dto) {
        Drug drug = drugRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Drug not found with id: " + id));

        // Update fields only if provided
        if (dto.getName() != null && !dto.getName().equals(drug.getName())) {
            if (drugRepository.existsByName(dto.getName())) {
                throw new RuntimeException("Drug with name '" + dto.getName() + "' already exists");
            }
            drug.setName(dto.getName());
        }

        if (dto.getGenericName() != null) {
            drug.setGenericName(dto.getGenericName());
        }

        if (dto.getCategory() != null) {
            drug.setCategory(dto.getCategory());
        }

        if (dto.getCommonDosages() != null) {
            drug.setCommonDosages(dto.getCommonDosages());
        }

        if (dto.getDescription() != null) {
            drug.setDescription(dto.getDescription());
        }

        if (dto.getSideEffects() != null) {
            drug.setSideEffects(dto.getSideEffects());
        }

        if (dto.getContraindications() != null) {
            drug.setContraindications(dto.getContraindications());
        }

        if (dto.getManufacturer() != null) {
            drug.setManufacturer(dto.getManufacturer());
        }

        Drug updatedDrug = drugRepository.save(drug);
        return DrugResponseDTO.fromEntity(updatedDrug);
    }

    @Transactional
    public DrugResponseDTO updateDrugStatus(Long id, String statusString) {
        Drug drug = drugRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Drug not found with id: " + id));

        try {
            Drug.DrugStatus status = Drug.DrugStatus.valueOf(statusString.toUpperCase());
            drug.setStatus(status);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid status: " + statusString + ". Valid values: ACTIVE, INACTIVE, DISCONTINUED");
        }

        Drug updatedDrug = drugRepository.save(drug);
        return DrugResponseDTO.fromEntity(updatedDrug);
    }

    @Transactional
    public void deleteDrug(Long id) {
        Drug drug = drugRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Drug not found with id: " + id));

        // Instead of hard delete, mark as discontinued
        drug.setStatus(Drug.DrugStatus.DISCONTINUED);
        drugRepository.save(drug);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getDrugStatistics() {
        Map<String, Object> stats = new HashMap<>();

        long totalDrugs = drugRepository.count();
        long activeDrugs = drugRepository.findByStatus(Drug.DrugStatus.ACTIVE).size();
        long inactiveDrugs = drugRepository.findByStatus(Drug.DrugStatus.INACTIVE).size();
        long discontinuedDrugs = drugRepository.findByStatus(Drug.DrugStatus.DISCONTINUED).size();

        List<String> categories = drugRepository.findAllCategories();

        stats.put("totalDrugs", totalDrugs);
        stats.put("activeDrugs", activeDrugs);
        stats.put("inactiveDrugs", inactiveDrugs);
        stats.put("discontinuedDrugs", discontinuedDrugs);
        stats.put("totalCategories", categories.size());
        stats.put("categories", categories);

        return stats;
    }
}
