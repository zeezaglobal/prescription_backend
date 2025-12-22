package com.zeezaglobal.prescription.Services;


import com.zeezaglobal.prescription.DTO.CreatePrescriptionDTO;
import com.zeezaglobal.prescription.DTO.PrescriptionResponseDTO;
import com.zeezaglobal.prescription.DTO.UpdatePrescriptionStatusDTO;
import com.zeezaglobal.prescription.Entities.*;
import com.zeezaglobal.prescription.Repository.DoctorRepository;
import com.zeezaglobal.prescription.Repository.DrugRepository;
import com.zeezaglobal.prescription.Repository.PatientRepository;
import com.zeezaglobal.prescription.Repository.PrescriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final DrugRepository drugRepository;

    @Transactional
    public PrescriptionResponseDTO createPrescription(CreatePrescriptionDTO dto, Long doctorId) {
        // Validate doctor
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new RuntimeException("Doctor not found with id: " + doctorId));

        // Validate patient
        Patient patient = patientRepository.findById(dto.getPatientId())
                .orElseThrow(() -> new RuntimeException("Patient not found with id: " + dto.getPatientId()));

        // Create prescription
        Prescription prescription = new Prescription();
        prescription.setDoctor(doctor);
        prescription.setPatient(patient);
        prescription.setPrescriptionDate(dto.getPrescriptionDate() != null ? dto.getPrescriptionDate() : LocalDate.now());
        prescription.setSpecialInstructions(dto.getSpecialInstructions());
        prescription.setDiagnosis(dto.getDiagnosis());
        prescription.setStatus(Prescription.PrescriptionStatus.ACTIVE);

        // Set valid until date
        if (dto.getValidUntil() != null) {
            prescription.setValidUntil(dto.getValidUntil());
        } else {
            prescription.setValidUntil(prescription.getPrescriptionDate().plusDays(30));
        }

        // Create medications
        List<PrescriptionMedication> medications = new ArrayList<>();
        for (CreatePrescriptionDTO.PrescriptionMedicationDTO medDTO : dto.getMedications()) {
            Drug drug = drugRepository.findById(medDTO.getDrugId())
                    .orElseThrow(() -> new RuntimeException("Drug not found with id: " + medDTO.getDrugId()));

            PrescriptionMedication medication = new PrescriptionMedication();
            medication.setDrug(drug);
            medication.setDosage(medDTO.getDosage());
            medication.setFrequency(medDTO.getFrequency());
            medication.setDuration(medDTO.getDuration());
            medication.setInstructions(medDTO.getInstructions());
            medication.setPrescription(prescription);

            medications.add(medication);
        }

        prescription.setMedications(medications);

        // Save prescription
        Prescription savedPrescription = prescriptionRepository.save(prescription);

        return PrescriptionResponseDTO.fromEntity(savedPrescription);
    }
    @Transactional(readOnly = true)
    public PrescriptionResponseDTO getPrescriptionByIdForPatient(Long prescriptionId, Long patientId) {
        Prescription prescription = prescriptionRepository.findById(prescriptionId)
                .orElseThrow(() -> new RuntimeException("Prescription not found with id: " + prescriptionId));

        // Verify that the prescription belongs to this patient
        if (!prescription.getPatient().getId().equals(patientId)) {
            throw new RuntimeException("Unauthorized: Prescription does not belong to this patient");
        }

        return PrescriptionResponseDTO.fromEntity(prescription);
    }

    /**
     * Get all prescriptions for a patient (without doctor filter)
     */
    @Transactional(readOnly = true)
    public Page<PrescriptionResponseDTO> getPrescriptionsByPatientId(Long patientId, Pageable pageable) {
        // Verify patient exists
        patientRepository.findById(patientId)
                .orElseThrow(() -> new RuntimeException("Patient not found with id: " + patientId));

        Page<Prescription> prescriptions = prescriptionRepository.findByPatientId(patientId, pageable);
        return prescriptions.map(PrescriptionResponseDTO::fromEntity);
    }

    /**
     * Get active prescriptions for a patient (without doctor filter)
     */
    @Transactional(readOnly = true)
    public List<PrescriptionResponseDTO> getActivePrescriptionsForPatientOnly(Long patientId) {
        // Verify patient exists
        patientRepository.findById(patientId)
                .orElseThrow(() -> new RuntimeException("Patient not found with id: " + patientId));

        List<Prescription> activePrescriptions = prescriptionRepository
                .findActivePrescriptionsByPatientId(patientId, LocalDate.now());

        return activePrescriptions.stream()
                .map(PrescriptionResponseDTO::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public PrescriptionResponseDTO getPrescriptionById(Long id, Long doctorId) {
        Prescription prescription = prescriptionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Prescription not found with id: " + id));

        // Verify that the prescription belongs to this doctor
        if (!prescription.getDoctor().getId().equals(doctorId)) {
            throw new RuntimeException("Unauthorized: Prescription does not belong to this doctor");
        }

        return PrescriptionResponseDTO.fromEntity(prescription);
    }

    @Transactional(readOnly = true)
    public Page<PrescriptionResponseDTO> getAllPrescriptionsByDoctor(Long doctorId, Pageable pageable) {
        Page<Prescription> prescriptions = prescriptionRepository.findByDoctorId(doctorId, pageable);
        return prescriptions.map(PrescriptionResponseDTO::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<PrescriptionResponseDTO> getPrescriptionsByPatient(Long patientId, Long doctorId, Pageable pageable) {
        // Verify doctor exists
        doctorRepository.findById(doctorId)
                .orElseThrow(() -> new RuntimeException("Doctor not found with id: " + doctorId));

        // Verify patient exists
        patientRepository.findById(patientId)
                .orElseThrow(() -> new RuntimeException("Patient not found with id: " + patientId));

        // Get prescriptions for this patient by this doctor
        Page<Prescription> prescriptions = prescriptionRepository.findByPatientIdAndDoctorId(patientId, doctorId, pageable);
        return prescriptions.map(PrescriptionResponseDTO::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<PrescriptionResponseDTO> getPrescriptionsByStatus(
            Long doctorId,
            String status,
            Pageable pageable) {

        Prescription.PrescriptionStatus prescriptionStatus;
        try {
            prescriptionStatus = Prescription.PrescriptionStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid status: " + status);
        }

        Page<Prescription> prescriptions = prescriptionRepository.findByDoctorIdAndStatus(doctorId, prescriptionStatus, pageable);
        return prescriptions.map(PrescriptionResponseDTO::fromEntity);
    }

    @Transactional
    public PrescriptionResponseDTO updatePrescriptionStatus(
            Long prescriptionId,
            UpdatePrescriptionStatusDTO dto,
            Long doctorId) {

        Prescription prescription = prescriptionRepository.findById(prescriptionId)
                .orElseThrow(() -> new RuntimeException("Prescription not found with id: " + prescriptionId));

        // Verify that the prescription belongs to this doctor
        if (!prescription.getDoctor().getId().equals(doctorId)) {
            throw new RuntimeException("Unauthorized: Prescription does not belong to this doctor");
        }

        // Update status
        try {
            Prescription.PrescriptionStatus newStatus = Prescription.PrescriptionStatus.valueOf(dto.getStatus().toUpperCase());
            prescription.setStatus(newStatus);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid status: " + dto.getStatus());
        }

        Prescription updatedPrescription = prescriptionRepository.save(prescription);
        return PrescriptionResponseDTO.fromEntity(updatedPrescription);
    }

    @Transactional
    public void deletePrescription(Long prescriptionId, Long doctorId) {
        Prescription prescription = prescriptionRepository.findById(prescriptionId)
                .orElseThrow(() -> new RuntimeException("Prescription not found with id: " + prescriptionId));

        // Verify that the prescription belongs to this doctor
        if (!prescription.getDoctor().getId().equals(doctorId)) {
            throw new RuntimeException("Unauthorized: Prescription does not belong to this doctor");
        }

        prescriptionRepository.delete(prescription);
    }

    @Transactional(readOnly = true)
    public List<PrescriptionResponseDTO> getActivePrescriptionsForPatient(Long patientId, Long doctorId) {
        // Verify doctor and patient exist
        doctorRepository.findById(doctorId)
                .orElseThrow(() -> new RuntimeException("Doctor not found with id: " + doctorId));

        patientRepository.findById(patientId)
                .orElseThrow(() -> new RuntimeException("Patient not found with id: " + patientId));

        List<Prescription> activePrescriptions = prescriptionRepository
                .findActivePrescriptionsByPatientId(patientId, LocalDate.now());

        return activePrescriptions.stream()
                .filter(p -> p.getDoctor().getId().equals(doctorId))
                .map(PrescriptionResponseDTO::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public long getTotalPrescriptionsByDoctor(Long doctorId) {
        return prescriptionRepository.countByDoctorId(doctorId);
    }

    @Transactional(readOnly = true)
    public long getTotalPrescriptionsForPatient(Long patientId) {
        return prescriptionRepository.countByPatientId(patientId);
    }
}