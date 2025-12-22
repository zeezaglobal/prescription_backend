package com.zeezaglobal.prescription.DTO;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateDrugDTO {


    private String name;


    private String genericName;


    private String category;

    private List<String> commonDosages;

    private String description;

    private String sideEffects;

    private String contraindications;

    private String manufacturer;
}
