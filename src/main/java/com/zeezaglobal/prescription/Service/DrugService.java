package com.zeezaglobal.prescription.Service;

import com.zeezaglobal.prescription.DTO.DrugDTO;
import com.zeezaglobal.prescription.Entities.Doctor;
import com.zeezaglobal.prescription.Entities.Drug;
import com.zeezaglobal.prescription.Repository.DoctorRepository;
import com.zeezaglobal.prescription.Repository.DrugRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class DrugService {

    @Autowired
    private DrugRepository drugRepository;

    public Drug saveDrug(Drug drug) {
        return drugRepository.save(drug);
    }

    public List<DrugDTO> getAllDrugs() {
        List<Drug> drugs = drugRepository.findAll();
        return drugs.stream()
                .map(this::convertToDTO)
                .toList();
    }

    public Optional<Drug> getDrugById(Long id) {
        return drugRepository.findById(id);
    }

    public void deleteDrug(Long id) {
        drugRepository.deleteById(id);
    }
    public List<DrugDTO> searchDrugs(String keyword) {
        List<Drug> drugs = drugRepository.findByNameContainingIgnoreCase(keyword);
        return drugs.stream()
                .map(this::convertToDTO)
                .toList();
    }
    private DrugDTO convertToDTO(Drug drug) {
        return new DrugDTO(
                drug.getId(),
                drug.getSerialNumber(),
                drug.getType(),
                drug.getName(),
                drug.getDescription(),
                drug.getType_name(),
                drug.getForm()
        );
    }
}
