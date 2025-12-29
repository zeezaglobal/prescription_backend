package com.zeezaglobal.prescription.DTO;



import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorDashboardDTO {

    // Doctor info
    private Long doctorId;
    private String doctorName;
    private String specialization;
    private boolean profileComplete;
    private Integer validated;

    // Statistics
    private DashboardStats stats;

    // Recent activity
    private List<RecentPatient> recentPatients;
    private List<RecentPrescription> recentPrescriptions;

    // Quick insights
    private List<PatientsByMonth> patientTrend;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DashboardStats {
        private long totalPatients;
        private long activePatients;
        private long totalPrescriptions;
        private long prescriptionsThisMonth;
        private long prescriptionsToday;
        private long pendingPrescriptions;
        private long newPatientsThisMonth;
        private long newPatientsThisWeek;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentPatient {
        private Long id;
        private String name;
        private String email;
        private String phone;
        private LocalDateTime createdAt;
        private int prescriptionCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentPrescription {
        private Long id;
        private String patientName;
        private Long patientId;
        private LocalDateTime createdAt;
        private String status;
        private String diagnosis;
        private int medicationCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PatientsByMonth {
        private String month;
        private int year;
        private long count;
    }
}