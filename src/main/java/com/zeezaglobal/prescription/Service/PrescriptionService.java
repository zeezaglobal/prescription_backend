package com.zeezaglobal.prescription.Service;


import com.zeezaglobal.prescription.DTO.PrescriptionRequestDTO;
import com.zeezaglobal.prescription.DTO.PrescriptionResponseDTO;
import com.zeezaglobal.prescription.Entities.*;
import com.zeezaglobal.prescription.Repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;
    private final PrescriptionMedicationRepository prescriptionMedicationRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final DrugRepository drugRepository;

    @Transactional(readOnly = true)
    public List<PrescriptionResponseDTO> getAllPrescriptions() {
        return prescriptionRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<PrescriptionResponseDTO> getAllPrescriptionsPaged(Pageable pageable) {
        return prescriptionRepository.findAll(pageable)
                .map(this::convertToDTO);
    }

    @Transactional(readOnly = true)
    public PrescriptionResponseDTO getPrescriptionById(Long id) {
        Prescription prescription = prescriptionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Prescription not found with id: " + id));
        return convertToDTO(prescription);
    }

    @Transactional(readOnly = true)
    public List<PrescriptionResponseDTO> getPrescriptionsByPatientId(Long patientId) {
        return prescriptionRepository.findByPatientId(patientId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<PrescriptionResponseDTO> getPrescriptionsByPatientIdPaged(Long patientId, Pageable pageable) {
        return prescriptionRepository.findByPatientId(patientId, pageable)
                .map(this::convertToDTO);
    }

    @Transactional(readOnly = true)
    public List<PrescriptionResponseDTO> getPrescriptionsByDoctorId(Long doctorId) {
        return prescriptionRepository.findByDoctorId(doctorId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<PrescriptionResponseDTO> getPrescriptionsByDoctorIdPaged(Long doctorId, Pageable pageable) {
        return prescriptionRepository.findByDoctorId(doctorId, pageable)
                .map(this::convertToDTO);
    }

    @Transactional(readOnly = true)
    public List<PrescriptionResponseDTO> getPrescriptionsByStatus(Prescription.PrescriptionStatus status) {
        return prescriptionRepository.findByStatus(status).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<PrescriptionResponseDTO> searchPrescriptions(String searchTerm, Pageable pageable) {
        return prescriptionRepository.searchPrescriptions(searchTerm, pageable)
                .map(this::convertToDTO);
    }

    @Transactional(readOnly = true)
    public List<PrescriptionResponseDTO> getPrescriptionsByDateRange(LocalDate startDate, LocalDate endDate) {
        return prescriptionRepository.findByDateRange(startDate, endDate).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public PrescriptionResponseDTO createPrescription(PrescriptionRequestDTO requestDTO) {
        // Validate patient
        Patient patient = patientRepository.findById(requestDTO.getPatientId())
                .orElseThrow(() -> new IllegalArgumentException("Patient not found with id: " + requestDTO.getPatientId()));

        // Validate doctor
        Doctor doctor = doctorRepository.findById(requestDTO.getDoctorId())
                .orElseThrow(() -> new IllegalArgumentException("Doctor not found with id: " + requestDTO.getDoctorId()));

        // Create prescription
        Prescription prescription = new Prescription();
        prescription.setPatient(patient);
        prescription.setDoctor(doctor);
        prescription.setPrescriptionDate(requestDTO.getPrescriptionDate() != null ?
                requestDTO.getPrescriptionDate() : LocalDate.now());
        prescription.setSpecialInstructions(requestDTO.getSpecialInstructions());
        prescription.setDiagnosis(requestDTO.getDiagnosis());
        prescription.setStatus(Prescription.PrescriptionStatus.ACTIVE);

        // Save prescription first to get ID
        prescription = prescriptionRepository.save(prescription);

        // Add medications
        for (PrescriptionRequestDTO.MedicationDTO medDTO : requestDTO.getMedications()) {
            Drug drug = drugRepository.findById(medDTO.getDrugId())
                    .orElseThrow(() -> new IllegalArgumentException("Drug not found with id: " + medDTO.getDrugId()));

            PrescriptionMedication medication = new PrescriptionMedication();
            medication.setPrescription(prescription);
            medication.setDrug(drug);
            medication.setDosage(medDTO.getDosage());
            medication.setFrequency(medDTO.getFrequency());
            medication.setDuration(medDTO.getDuration());
            medication.setInstructions(medDTO.getInstructions());

            prescription.addMedication(medication);
        }

        // Save with medications
        prescription = prescriptionRepository.save(prescription);

        return convertToDTO(prescription);
    }

    @Transactional
    public PrescriptionResponseDTO updatePrescription(Long id, PrescriptionRequestDTO requestDTO) {
        Prescription prescription = prescriptionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Prescription not found with id: " + id));

        // Update basic fields
        prescription.setSpecialInstructions(requestDTO.getSpecialInstructions());
        prescription.setDiagnosis(requestDTO.getDiagnosis());

        if (requestDTO.getPrescriptionDate() != null) {
            prescription.setPrescriptionDate(requestDTO.getPrescriptionDate());
        }

        // Update medications if provided
        if (requestDTO.getMedications() != null && !requestDTO.getMedications().isEmpty()) {
            // Remove existing medications
            prescription.getMedications().clear();
            prescriptionRepository.save(prescription);

            // Add new medications
            for (PrescriptionRequestDTO.MedicationDTO medDTO : requestDTO.getMedications()) {
                Drug drug = drugRepository.findById(medDTO.getDrugId())
                        .orElseThrow(() -> new IllegalArgumentException("Drug not found with id: " + medDTO.getDrugId()));

                PrescriptionMedication medication = new PrescriptionMedication();
                medication.setPrescription(prescription);
                medication.setDrug(drug);
                medication.setDosage(medDTO.getDosage());
                medication.setFrequency(medDTO.getFrequency());
                medication.setDuration(medDTO.getDuration());
                medication.setInstructions(medDTO.getInstructions());

                prescription.addMedication(medication);
            }
        }

        prescription = prescriptionRepository.save(prescription);
        return convertToDTO(prescription);
    }

    @Transactional
    public PrescriptionResponseDTO updatePrescriptionStatus(Long id, Prescription.PrescriptionStatus status) {
        Prescription prescription = prescriptionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Prescription not found with id: " + id));

        prescription.setStatus(status);
        prescription = prescriptionRepository.save(prescription);

        return convertToDTO(prescription);
    }

    @Transactional
    public void deletePrescription(Long id) {
        Prescription prescription = prescriptionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Prescription not found with id: " + id));
        prescriptionRepository.delete(prescription);
    }

    @Transactional(readOnly = true)
    public Long getTotalPrescriptions() {
        return prescriptionRepository.count();
    }

    @Transactional(readOnly = true)
    public Long getActivePrescriptions() {
        return prescriptionRepository.countByStatus(Prescription.PrescriptionStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public Long getPendingPrescriptions() {
        return prescriptionRepository.countByStatus(Prescription.PrescriptionStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public Long getPrescriptionsThisMonth() {
        LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);
        return prescriptionRepository.countByDateAfter(startOfMonth);
    }

    @Transactional
    public void updateExpiredPrescriptions() {
        List<Prescription> expiredPrescriptions = prescriptionRepository
                .findExpiredPrescriptions(LocalDate.now());

        for (Prescription prescription : expiredPrescriptions) {
            prescription.setStatus(Prescription.PrescriptionStatus.EXPIRED);
        }

        prescriptionRepository.saveAll(expiredPrescriptions);
    }

    // Conversion method
    private PrescriptionResponseDTO convertToDTO(Prescription prescription) {
        PrescriptionResponseDTO dto = new PrescriptionResponseDTO();
        dto.setId(prescription.getId());
        dto.setPrescriptionDate(prescription.getPrescriptionDate());
        dto.setStatus(prescription.getStatus());
        dto.setSpecialInstructions(prescription.getSpecialInstructions());
        dto.setValidUntil(prescription.getValidUntil());
        dto.setDiagnosis(prescription.getDiagnosis());
        dto.setCreatedAt(prescription.getCreatedAt());
        dto.setUpdatedAt(prescription.getUpdatedAt());

        // Patient summary
        Patient patient = prescription.getPatient();
        PrescriptionResponseDTO.PatientSummary patientSummary = new PrescriptionResponseDTO.PatientSummary();
        patientSummary.setId(patient.getId());
        patientSummary.setName(patient.getName());
        patientSummary.setAge(patient.getAge());
        patientSummary.setPhone(patient.getPhone());
        dto.setPatient(patientSummary);

        // Doctor summary
        Doctor doctor = prescription.getDoctor();
        PrescriptionResponseDTO.DoctorSummary doctorSummary = new PrescriptionResponseDTO.DoctorSummary();
        doctorSummary.setId(doctor.getId());
        doctorSummary.setName(doctor.getFirstName());
        doctorSummary.setLicenseNumber(doctor.getLicenseNumber());
        doctorSummary.setSpecialization(doctor.getSpecialization());
        dto.setDoctor(doctorSummary);

        // Medications
        List<PrescriptionResponseDTO.MedicationDetail> medications = prescription.getMedications().stream()
                .map(med -> {
                    PrescriptionResponseDTO.MedicationDetail medDetail = new PrescriptionResponseDTO.MedicationDetail();
                    medDetail.setId(med.getId());
                    medDetail.setDosage(med.getDosage());
                    medDetail.setFrequency(med.getFrequency());
                    medDetail.setDuration(med.getDuration());
                    medDetail.setInstructions(med.getInstructions());

                    Drug drug = med.getDrug();
                    PrescriptionResponseDTO.DrugInfo drugInfo = new PrescriptionResponseDTO.DrugInfo();
                    drugInfo.setId(drug.getId());
                    drugInfo.setName(drug.getName());
                    drugInfo.setGenericName(drug.getGenericName());
                    drugInfo.setCategory(drug.getCategory());
                    medDetail.setDrug(drugInfo);

                    return medDetail;
                })
                .collect(Collectors.toList());
        dto.setMedications(medications);

        return dto;
    }
}