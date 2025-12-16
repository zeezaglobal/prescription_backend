package com.zeezaglobal.prescription.Entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "drugs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Drug {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private String genericName;

    @Column(nullable = false)
    private String category;

    @ElementCollection
    @CollectionTable(name = "drug_dosages", joinColumns = @JoinColumn(name = "drug_id"))
    @Column(name = "dosage")
    private List<String> commonDosages = new ArrayList<>();

    @Column(length = 1000)
    private String description;

    @Column(length = 500)
    private String sideEffects;

    @Column(length = 500)
    private String contraindications;

    @Column
    private String manufacturer;

    @Enumerated(EnumType.STRING)
    private DrugStatus status = DrugStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum DrugStatus {
        ACTIVE,
        INACTIVE,
        DISCONTINUED
    }
}