package com.zeezaglobal.prescription.Service;

import com.zeezaglobal.prescription.DTO.DoctorDashboardDTO;
import com.zeezaglobal.prescription.Entities.Doctor;
import com.zeezaglobal.prescription.Entities.Patient;
import com.zeezaglobal.prescription.Entities.Prescription;
import com.zeezaglobal.prescription.Repository.DoctorRepository;
import com.zeezaglobal.prescription.Repository.PatientRepository;
import com.zeezaglobal.prescription.Repository.PrescriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DoctorDashboardService {

    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final PrescriptionRepository prescriptionRepository;

    @Transactional(readOnly = true)
    public DoctorDashboardDTO getDashboard(Long doctorId) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new RuntimeException("Doctor not found with id: " + doctorId));

        return DoctorDashboardDTO.builder()
                .doctorId(doctor.getId())
                .doctorName(doctor.getName())
                .specialization(doctor.getSpecialization())
                .profileComplete(doctor.isProfileComplete())
                .validated(doctor.getValidated())
                .stats(buildStats(doctorId))
                .recentPatients(getRecentPatients(doctorId, 5))
                .recentPrescriptions(getRecentPrescriptions(doctorId, 5))
                .patientTrend(getPatientTrend(doctorId, 6))
                .build();
    }

    private DoctorDashboardDTO.DashboardStats buildStats(Long doctorId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfMonth = now.withDayOfMonth(1).with(LocalTime.MIN);
        LocalDateTime startOfWeek = now.minusDays(now.getDayOfWeek().getValue() - 1).with(LocalTime.MIN);
        LocalDateTime startOfToday = now.with(LocalTime.MIN);

        // Patient statistics
        long totalPatients = patientRepository.countByDoctorId(doctorId);
        long newPatientsThisMonth = patientRepository.countByDoctorIdAndCreatedAtAfter(doctorId, startOfMonth);
        long newPatientsThisWeek = patientRepository.countByDoctorIdAndCreatedAtAfter(doctorId, startOfWeek);

        // Prescription statistics
        long totalPrescriptions = prescriptionRepository.countByDoctorId(doctorId);
        long prescriptionsThisMonth = prescriptionRepository.countByDoctorIdAndCreatedAtAfter(doctorId, startOfMonth);
        long prescriptionsToday = prescriptionRepository.countByDoctorIdAndCreatedAtAfter(doctorId, startOfToday);

        return DoctorDashboardDTO.DashboardStats.builder()
                .totalPatients(totalPatients)
                .activePatients(totalPatients) // All patients considered active since no status field
                .totalPrescriptions(totalPrescriptions)
                .prescriptionsThisMonth(prescriptionsThisMonth)
                .prescriptionsToday(prescriptionsToday)
                .pendingPrescriptions(0) // Update if Prescription has status field
                .newPatientsThisMonth(newPatientsThisMonth)
                .newPatientsThisWeek(newPatientsThisWeek)
                .build();
    }

    private List<DoctorDashboardDTO.RecentPatient> getRecentPatients(Long doctorId, int limit) {
        List<Patient> patients = patientRepository.findByDoctorIdOrderByCreatedAtDesc(doctorId);

        return patients.stream()
                .limit(limit)
                .map(patient -> DoctorDashboardDTO.RecentPatient.builder()
                        .id(patient.getId())
                        .name(patient.getName())
                        .email(patient.getEmail())
                        .phone(patient.getPhone())
                        .createdAt(patient.getCreatedAt())
                        .prescriptionCount((int) prescriptionRepository.countByPatientId(patient.getId()))
                        .build())
                .collect(Collectors.toList());
    }

    private List<DoctorDashboardDTO.RecentPrescription> getRecentPrescriptions(Long doctorId, int limit) {
        List<Prescription> prescriptions = prescriptionRepository.findByDoctorIdOrderByCreatedAtDesc(doctorId);

        return prescriptions.stream()
                .limit(limit)
                .map(prescription -> DoctorDashboardDTO.RecentPrescription.builder()
                        .id(prescription.getId())
                        .patientName(prescription.getPatient() != null ? prescription.getPatient().getName() : "Unknown")
                        .patientId(prescription.getPatient() != null ? prescription.getPatient().getId() : null)
                        .createdAt(prescription.getCreatedAt())
                        .status(prescription.getStatus() != null ? prescription.getStatus().name() : "UNKNOWN")
                        .diagnosis(prescription.getDiagnosis())
                        .medicationCount(prescription.getMedications() != null ? prescription.getMedications().size() : 0)
                        .build())
                .collect(Collectors.toList());
    }

    private List<DoctorDashboardDTO.PatientsByMonth> getPatientTrend(Long doctorId, int months) {
        List<DoctorDashboardDTO.PatientsByMonth> trend = new ArrayList<>();
        YearMonth currentMonth = YearMonth.now();

        for (int i = months - 1; i >= 0; i--) {
            YearMonth month = currentMonth.minusMonths(i);
            LocalDateTime startOfMonth = month.atDay(1).atStartOfDay();
            LocalDateTime endOfMonth = month.atEndOfMonth().atTime(LocalTime.MAX);

            long count = patientRepository.countByDoctorIdAndCreatedAtBetween(doctorId, startOfMonth, endOfMonth);

            trend.add(DoctorDashboardDTO.PatientsByMonth.builder()
                    .month(month.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH))
                    .year(month.getYear())
                    .count(count)
                    .build());
        }

        return trend;
    }
}