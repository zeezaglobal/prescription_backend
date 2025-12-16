package com.zeezaglobal.prescription.Controller;

import com.zeezaglobal.prescription.DTO.DrugDTO;
import com.zeezaglobal.prescription.Entities.Drug;
import com.zeezaglobal.prescription.Service.DrugService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/drugs")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DrugController {

    private final DrugService drugService;

    @GetMapping
    public ResponseEntity<List<Drug>> getAllDrugs() {
        return ResponseEntity.ok(drugService.getAllDrugs());
    }

    @GetMapping("/active")
    public ResponseEntity<List<Drug>> getAllActiveDrugs() {
        return ResponseEntity.ok(drugService.getAllActiveDrugs());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Drug> getDrugById(@PathVariable Long id) {
        return drugService.getDrugById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/name/{name}")
    public ResponseEntity<Drug> getDrugByName(@PathVariable String name) {
        return drugService.getDrugByName(name)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<Drug>> getDrugsByCategory(@PathVariable String category) {
        return ResponseEntity.ok(drugService.getDrugsByCategory(category));
    }

    @GetMapping("/categories")
    public ResponseEntity<List<String>> getAllCategories() {
        return ResponseEntity.ok(drugService.getAllCategories());
    }

    @GetMapping("/search")
    public ResponseEntity<List<Drug>> searchDrugs(@RequestParam String query) {
        return ResponseEntity.ok(drugService.searchDrugs(query));
    }

    @GetMapping("/search/active")
    public ResponseEntity<List<Drug>> searchActiveDrugs(@RequestParam String query) {
        return ResponseEntity.ok(drugService.searchActiveDrugs(query));
    }

    @PostMapping
    public ResponseEntity<Drug> createDrug(@RequestBody Drug drug) {
        try {
            Drug createdDrug = drugService.createDrug(drug);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdDrug);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Drug> updateDrug(@PathVariable Long id, @RequestBody Drug drug) {
        try {
            Drug updatedDrug = drugService.updateDrug(id, drug);
            return ResponseEntity.ok(updatedDrug);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Drug> updateDrugStatus(@PathVariable Long id, @RequestParam Drug.DrugStatus status) {
        try {
            Drug updatedDrug = drugService.updateDrugStatus(id, status);
            return ResponseEntity.ok(updatedDrug);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDrug(@PathVariable Long id) {
        try {
            drugService.deleteDrug(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}