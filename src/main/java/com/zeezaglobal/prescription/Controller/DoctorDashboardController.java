package com.zeezaglobal.prescription.Controller;



import com.zeezaglobal.prescription.DTO.DoctorDashboardDTO;
import com.zeezaglobal.prescription.Entities.Doctor;
import com.zeezaglobal.prescription.Service.DoctorDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/doctor/dashboard")
@RequiredArgsConstructor
public class DoctorDashboardController {

    private final DoctorDashboardService dashboardService;

    /**
     * Get complete dashboard data for the authenticated doctor
     */
    @GetMapping
    public ResponseEntity<DoctorDashboardDTO> getDashboard() {
        Long doctorId = getAuthenticatedDoctorId();
        DoctorDashboardDTO dashboard = dashboardService.getDashboard(doctorId);
        return ResponseEntity.ok(dashboard);
    }

    /**
     * Get dashboard stats only (lighter endpoint for quick refresh)
     */
    @GetMapping("/stats")
    public ResponseEntity<DoctorDashboardDTO.DashboardStats> getStats() {
        Long doctorId = getAuthenticatedDoctorId();
        DoctorDashboardDTO dashboard = dashboardService.getDashboard(doctorId);
        return ResponseEntity.ok(dashboard.getStats());
    }



    private Long getAuthenticatedDoctorId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Doctor) {
            Doctor doctor = (Doctor) authentication.getPrincipal();
            return doctor.getId();
        }
        throw new RuntimeException("Unable to get authenticated doctor");
    }
}
