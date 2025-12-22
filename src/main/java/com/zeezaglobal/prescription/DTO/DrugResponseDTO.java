package com.zeezaglobal.prescription.DTO;

import com.zeezaglobal.prescription.Entities.Drug;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DrugResponseDTO {

    private Long id;
    private String name;
    private String genericName;
    private String category;
    private List<String> commonDosages;
    private String description;
    private String sideEffects;
    private String contraindications;
    private String manufacturer;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Factory method to create DrugResponseDTO from Drug entity
     * @param drug The Drug entity
     * @return DrugResponseDTO
     */
    public static DrugResponseDTO fromEntity(Drug drug) {
        DrugResponseDTO dto = new DrugResponseDTO();
        dto.setId(drug.getId());
        dto.setName(drug.getName());
        dto.setGenericName(drug.getGenericName());
        dto.setCategory(drug.getCategory());
        dto.setCommonDosages(drug.getCommonDosages());
        dto.setDescription(drug.getDescription());
        dto.setSideEffects(drug.getSideEffects());
        dto.setContraindications(drug.getContraindications());
        dto.setManufacturer(drug.getManufacturer());
        dto.setStatus(drug.getStatus() != null ? drug.getStatus().name() : null);
        dto.setCreatedAt(drug.getCreatedAt());
        dto.setUpdatedAt(drug.getUpdatedAt());
        return dto;
    }
}