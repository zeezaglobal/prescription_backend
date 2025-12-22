package com.zeezaglobal.prescription.Controller;




import com.zeezaglobal.prescription.DTO.CreateDrugDTO;
import com.zeezaglobal.prescription.DTO.DrugResponseDTO;
import com.zeezaglobal.prescription.DTO.UpdateDrugDTO;
import com.zeezaglobal.prescription.Service.DrugService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/drugs")
@RequiredArgsConstructor

public class DrugController {

    private final DrugService drugService;

    /**
     * Create a new drug (Doctors only)
     * POST /api/drugs
     */
    @PostMapping
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<?> createDrug( @RequestBody CreateDrugDTO dto) {
        try {
            DrugResponseDTO response = drugService.createDrug(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * Get all drugs (paginated)
     * GET /api/drugs?page=0&size=10&sort=name,asc
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('DOCTOR', 'PATIENT')")
    public ResponseEntity<?> getAllDrugs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection) {
        try {
            Sort.Direction direction = sortDirection.equalsIgnoreCase("asc")
                    ? Sort.Direction.ASC
                    : Sort.Direction.DESC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

            Page<DrugResponseDTO> drugs = drugService.getAllDrugs(pageable);
            return ResponseEntity.ok(createPageResponse(drugs));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * Get drug by ID
     * GET /api/drugs/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'PATIENT')")
    public ResponseEntity<?> getDrugById(@PathVariable Long id) {
        try {
            DrugResponseDTO response = drugService.getDrugById(id);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * Search drugs by name or generic name
     * GET /api/drugs/search?query=lisinopril
     */
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('DOCTOR', 'PATIENT')")
    public ResponseEntity<?> searchDrugs(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<DrugResponseDTO> drugs = drugService.searchDrugs(query, pageable);
            return ResponseEntity.ok(createPageResponse(drugs));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * Get drugs by category
     * GET /api/drugs/category/{category}
     */
    @GetMapping("/category/{category}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'PATIENT')")
    public ResponseEntity<?> getDrugsByCategory(
            @PathVariable String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"));
            Page<DrugResponseDTO> drugs = drugService.getDrugsByCategory(category, pageable);
            return ResponseEntity.ok(createPageResponse(drugs));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * Get all drug categories
     * GET /api/drugs/categories/list
     */
    @GetMapping("/categories/list")
    @PreAuthorize("hasAnyRole('DOCTOR', 'PATIENT')")
    public ResponseEntity<?> getAllCategories() {
        try {
            List<String> categories = drugService.getAllCategories();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", categories);
            response.put("count", categories.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * Get active drugs only
     * GET /api/drugs/active
     */
    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('DOCTOR', 'PATIENT')")
    public ResponseEntity<?> getActiveDrugs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"));
            Page<DrugResponseDTO> drugs = drugService.getActiveDrugs(pageable);
            return ResponseEntity.ok(createPageResponse(drugs));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * Update drug information (Doctors only)
     * PUT /api/drugs/{id}
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<?> updateDrug(
            @PathVariable Long id,
             @RequestBody UpdateDrugDTO dto) {
        try {
            DrugResponseDTO response = drugService.updateDrug(id, dto);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * Update drug status (Doctors only)
     * PATCH /api/drugs/{id}/status
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<?> updateDrugStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        try {
            DrugResponseDTO response = drugService.updateDrugStatus(id, status);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * Delete drug (Doctors only)
     * DELETE /api/drugs/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<?> deleteDrug(@PathVariable Long id) {
        try {
            drugService.deleteDrug(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Drug deleted successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * Get drug statistics
     * GET /api/drugs/stats
     */
    @GetMapping("/stats")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<?> getDrugStats() {
        try {
            Map<String, Object> stats = drugService.getDrugStatistics();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", stats);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    // Helper method to create error response
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("error", message);
        return error;
    }

    // Helper method to create paginated response
    private Map<String, Object> createPageResponse(Page<DrugResponseDTO> page) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", page.getContent());
        response.put("currentPage", page.getNumber());
        response.put("totalPages", page.getTotalPages());
        response.put("totalElements", page.getTotalElements());
        response.put("pageSize", page.getSize());
        response.put("hasNext", page.hasNext());
        response.put("hasPrevious", page.hasPrevious());
        return response;
    }
}