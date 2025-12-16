package com.zeezaglobal.prescription.Service;

import com.zeezaglobal.prescription.Entities.Drug;
import com.zeezaglobal.prescription.Repository.DrugRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DrugService {

    private final DrugRepository drugRepository;

    @Transactional(readOnly = true)
    public List<Drug> getAllDrugs() {
        return drugRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Drug> getAllActiveDrugs() {
        return drugRepository.findAllActiveDrugs(Drug.DrugStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public Optional<Drug> getDrugById(Long id) {
        return drugRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Drug> getDrugByName(String name) {
        return drugRepository.findByName(name);
    }

    @Transactional(readOnly = true)
    public List<Drug> getDrugsByCategory(String category) {
        return drugRepository.findByCategory(category);
    }

    @Transactional(readOnly = true)
    public List<Drug> searchDrugs(String searchTerm) {
        return drugRepository.searchDrugs(searchTerm);
    }

    @Transactional(readOnly = true)
    public List<Drug> searchActiveDrugs(String searchTerm) {
        return drugRepository.searchActiveDrugs(searchTerm);
    }

    @Transactional(readOnly = true)
    public List<String> getAllCategories() {
        return drugRepository.findAllCategories();
    }

    @Transactional
    public Drug createDrug(Drug drug) {
        // Check if drug with same name already exists
        if (drugRepository.findByName(drug.getName()).isPresent()) {
            throw new IllegalArgumentException("Drug with name " + drug.getName() + " already exists");
        }
        return drugRepository.save(drug);
    }

    @Transactional
    public Drug updateDrug(Long id, Drug drugDetails) {
        Drug drug = drugRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Drug not found with id: " + id));

        // Check if name is being changed and new name already exists
        if (!drug.getName().equals(drugDetails.getName()) &&
                drugRepository.findByName(drugDetails.getName()).isPresent()) {
            throw new IllegalArgumentException("Drug with name " + drugDetails.getName() + " already exists");
        }

        drug.setName(drugDetails.getName());
        drug.setGenericName(drugDetails.getGenericName());
        drug.setCategory(drugDetails.getCategory());
        drug.setCommonDosages(drugDetails.getCommonDosages());
        drug.setDescription(drugDetails.getDescription());
        drug.setSideEffects(drugDetails.getSideEffects());
        drug.setContraindications(drugDetails.getContraindications());
        drug.setManufacturer(drugDetails.getManufacturer());
        drug.setStatus(drugDetails.getStatus());

        return drugRepository.save(drug);
    }

    @Transactional
    public void deleteDrug(Long id) {
        Drug drug = drugRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Drug not found with id: " + id));
        drugRepository.delete(drug);
    }

    @Transactional
    public Drug updateDrugStatus(Long id, Drug.DrugStatus status) {
        Drug drug = drugRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Drug not found with id: " + id));
        drug.setStatus(status);
        return drugRepository.save(drug);
    }
}
