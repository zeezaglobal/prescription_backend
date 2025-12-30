package com.zeezaglobal.prescription.Service;





import com.zeezaglobal.prescription.DTO.PatientReportDTO;
import com.zeezaglobal.prescription.DTO.PatientReportDTO.*;
import com.zeezaglobal.prescription.Entities.*;
import com.zeezaglobal.prescription.Repository.PatientRepository;
import com.zeezaglobal.prescription.Repository.PrescriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PatientReportService {

    private final PatientRepository patientRepository;
    private final PrescriptionRepository prescriptionRepository;

    /**
     * Generate a comprehensive report for a patient including all prescriptions
     */
    @Transactional(readOnly = true)
    public PatientReportDTO generatePatientReport(Long patientId) {
        log.info("Generating report for patient ID: {}", patientId);

        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new RuntimeException("Patient not found with ID: " + patientId));

        List<Prescription> prescriptions = prescriptionRepository.findByPatientIdOrderByPrescriptionDateDesc(patientId);

        return buildPatientReport(patient, prescriptions);
    }

    /**
     * Generate report for a patient by doctor (ensures doctor owns the patient)
     */
    @Transactional(readOnly = true)
    public PatientReportDTO generatePatientReportByDoctor(Long patientId, Long doctorId) {
        log.info("Generating report for patient ID: {} by doctor ID: {}", patientId, doctorId);

        Patient patient = patientRepository.findByIdAndDoctorId(patientId, doctorId)
                .orElseThrow(() -> new RuntimeException("Patient not found or not assigned to this doctor"));

        List<Prescription> prescriptions = prescriptionRepository.findByPatientIdOrderByPrescriptionDateDesc(patientId);

        return buildPatientReport(patient, prescriptions);
    }

    /**
     * Generate report within a date range
     */
    @Transactional(readOnly = true)
    public PatientReportDTO generatePatientReportWithDateRange(Long patientId, LocalDate startDate, LocalDate endDate) {
        log.info("Generating report for patient ID: {} from {} to {}", patientId, startDate, endDate);

        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new RuntimeException("Patient not found with ID: " + patientId));

        List<Prescription> prescriptions = prescriptionRepository
                .findByPatientIdAndPrescriptionDateBetweenOrderByPrescriptionDateDesc(patientId, startDate, endDate);

        return buildPatientReport(patient, prescriptions);
    }

    private PatientReportDTO buildPatientReport(Patient patient, List<Prescription> prescriptions) {
        return PatientReportDTO.builder()
                .patientId(patient.getId())
                .name(patient.getName())
                .firstName(patient.getFirstName())
                .lastName(patient.getLastName())
                .dateOfBirth(patient.getDateOfBirth())
                .age(patient.getAge())
                .gender(patient.getGender())
                .phone(patient.getPhone())
                .email(patient.getEmail())
                .address(patient.getAddress())
                .bloodGroup(patient.getBloodGroup())
                .medicalHistory(patient.getMedicalHistory())
                .allergies(patient.getAllergies())
                .numberOfVisits(patient.getNumberOfVisit())
                .registeredDate(patient.getCreatedAt())
                .assignedDoctor(buildDoctorSummary(patient.getDoctor()))
                .prescriptions(buildPrescriptionSummaries(prescriptions))
                .totalPrescriptions(prescriptions.size())
                .statistics(buildStatistics(patient, prescriptions))
                .build();
    }

    private DoctorSummary buildDoctorSummary(Doctor doctor) {
        if (doctor == null) {
            return null;
        }
        return DoctorSummary.builder()
                .doctorId(doctor.getId())
                .name(doctor.getName())
                .specialization(doctor.getSpecialization())
                .phone(doctor.getPhone())
                .email(doctor.getEmail())

                .build();
    }

    private List<PrescriptionSummary> buildPrescriptionSummaries(List<Prescription> prescriptions) {
        return prescriptions.stream()
                .map(this::buildPrescriptionSummary)
                .collect(Collectors.toList());
    }

    private PrescriptionSummary buildPrescriptionSummary(Prescription prescription) {
        return PrescriptionSummary.builder()
                .prescriptionId(prescription.getId())
                .prescriptionDate(prescription.getPrescriptionDate())
                .validUntil(prescription.getValidUntil())
                .diagnosis(prescription.getDiagnosis())
                .specialInstructions(prescription.getSpecialInstructions())
                .status(prescription.getStatus() != null ? prescription.getStatus().name() : null)
                .medications(buildMedicationItems(prescription.getMedications()))
                .prescribedBy(prescription.getDoctor() != null ? prescription.getDoctor().getName() : null)
                .prescribedByDoctorId(prescription.getDoctor() != null ? prescription.getDoctor().getId() : null)
                .createdAt(prescription.getCreatedAt())
                .updatedAt(prescription.getUpdatedAt())
                .build();
    }

    private List<MedicationItem> buildMedicationItems(List<PrescriptionMedication> medications) {
        if (medications == null || medications.isEmpty()) {
            return Collections.emptyList();
        }
        return medications.stream()
                .map(this::buildMedicationItem)
                .collect(Collectors.toList());
    }

    private MedicationItem buildMedicationItem(PrescriptionMedication med) {
        MedicationItem.MedicationItemBuilder builder = MedicationItem.builder()
                .medicationId(med.getId());

        // Get medicine name from Drug entity
        if (med.getDrug() != null) {
            builder.medicineName(med.getDrug().getName());
        }

        // Map fields from PrescriptionMedication
        builder.dosage(med.getDosage())
                .frequency(med.getFrequency())
                .duration(med.getDuration())
                .instructions(med.getInstructions());

        return builder.build();
    }

    private PatientStatistics buildStatistics(Patient patient, List<Prescription> prescriptions) {
        // Collect all diagnoses
        List<String> allDiagnoses = prescriptions.stream()
                .map(Prescription::getDiagnosis)
                .filter(Objects::nonNull)
                .filter(d -> !d.trim().isEmpty())
                .collect(Collectors.toList());

        // Collect all medication names from Drug entity
        List<String> allMedications = prescriptions.stream()
                .flatMap(p -> p.getMedications() != null ? p.getMedications().stream() : Collections.<PrescriptionMedication>emptyList().stream())
                .filter(med -> med.getDrug() != null)
                .map(med -> med.getDrug().getName())
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // Count prescriptions by status
        Map<Prescription.PrescriptionStatus, Long> statusCounts = prescriptions.stream()
                .filter(p -> p.getStatus() != null)
                .collect(Collectors.groupingBy(Prescription::getStatus, Collectors.counting()));

        LocalDate firstVisit = prescriptions.isEmpty() ? null :
                prescriptions.get(prescriptions.size() - 1).getPrescriptionDate();
        LocalDate lastVisit = prescriptions.isEmpty() ? null :
                prescriptions.get(0).getPrescriptionDate();

        return PatientStatistics.builder()
                .totalVisits(patient.getNumberOfVisit())
                .totalPrescriptions(prescriptions.size())
                .firstVisitDate(firstVisit)
                .lastVisitDate(lastVisit)
                .frequentDiagnoses(getFrequentItems(allDiagnoses, 5))
                .frequentMedications(getFrequentItems(allMedications, 5))
                .pendingPrescriptions(statusCounts.getOrDefault(Prescription.PrescriptionStatus.PENDING, 0L).intValue())
                .activePrescriptions(statusCounts.getOrDefault(Prescription.PrescriptionStatus.ACTIVE, 0L).intValue())
                .completedPrescriptions(statusCounts.getOrDefault(Prescription.PrescriptionStatus.COMPLETED, 0L).intValue())
                .cancelledPrescriptions(statusCounts.getOrDefault(Prescription.PrescriptionStatus.CANCELLED, 0L).intValue())
                .expiredPrescriptions(statusCounts.getOrDefault(Prescription.PrescriptionStatus.EXPIRED, 0L).intValue())
                .build();
    }

    private List<String> getFrequentItems(List<String> items, int limit) {
        if (items.isEmpty()) {
            return Collections.emptyList();
        }
        return items.stream()
                .collect(Collectors.groupingBy(s -> s, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * Get prescription history only (lightweight version)
     */
    @Transactional(readOnly = true)
    public List<PrescriptionSummary> getPrescriptionHistory(Long patientId) {
        List<Prescription> prescriptions = prescriptionRepository.findByPatientIdOrderByPrescriptionDateDesc(patientId);
        return buildPrescriptionSummaries(prescriptions);
    }

    /**
     * Get prescription history with pagination
     */
    @Transactional(readOnly = true)
    public List<PrescriptionSummary> getPrescriptionHistoryPaginated(Long patientId, int page, int size) {
        List<Prescription> prescriptions = prescriptionRepository.findByPatientIdOrderByPrescriptionDateDesc(patientId);

        int start = page * size;
        int end = Math.min(start + size, prescriptions.size());

        if (start >= prescriptions.size()) {
            return Collections.emptyList();
        }

        return buildPrescriptionSummaries(prescriptions.subList(start, end));
    }

    /**
     * Get total prescription count for a patient
     */
    @Transactional(readOnly = true)
    public long getPrescriptionCount(Long patientId) {
        return prescriptionRepository.countByPatientId(patientId);
    }
}