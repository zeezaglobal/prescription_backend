package com.zeezaglobal.prescription.Controller;

import com.zeezaglobal.prescription.DTO.DoctorDTO;
import com.zeezaglobal.prescription.DTO.UpdateDoctorDTO;
import com.zeezaglobal.prescription.Entities.Doctor;
import com.zeezaglobal.prescription.Repository.DoctorRepository;
import com.zeezaglobal.prescription.Repository.UserRepository;
import com.zeezaglobal.prescription.Service.DoctorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;


@RestController
@RequestMapping("/doctor")
public class DoctorController {
    @Autowired
    private DoctorService doctorService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @GetMapping("/getdata/{doctorId}")
    public ResponseEntity<DoctorDTO> getDoctorDetails(@PathVariable Long doctorId) {
        return doctorRepository.findById(doctorId)
                .map(doctor -> ResponseEntity.ok(mapToDTO(doctor))) // Map entity to DTO
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(null)); // Return 404 if not found
    }

    private DoctorDTO mapToDTO(Doctor doctor) {
        return new DoctorDTO(
                doctor.getId(),
                doctor.getName(),
                null, // lastName - new entity doesn't have separate firstName/lastName, only 'name'
                doctor.getSpecialization(),
                doctor.getLicenseNumber(),
                null, // hospitalName - not in new entity, can be added if needed
                doctor.getPhone(), // contactNumber mapped to phone
                null  // stripeUsername - not in new entity, can be added if needed
        );
    }

    @PostMapping("/update")
    public ResponseEntity<Doctor> updateDoctor(@RequestBody UpdateDoctorDTO updatedDoctor) {
        Logger logger = LoggerFactory.getLogger(this.getClass());
        logger.info("Updating doctor with ID: {}", updatedDoctor.getId());

        if (updatedDoctor.getId() == null) {
            logger.error("Doctor ID is null in update request.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }

        Optional<Doctor> existingDoctor = doctorRepository.findById(updatedDoctor.getId());

        if (existingDoctor.isPresent()) {
            Doctor doctor = existingDoctor.get();

            // Log old and new values
            logger.info("Old values - Name: {}, Specialization: {}, License: {}",
                    doctor.getName(), doctor.getSpecialization(), doctor.getLicenseNumber());
            logger.info("New values - FirstName: {}, LastName: {}, Specialization: {}",
                    updatedDoctor.getFirstName(), updatedDoctor.getLastName(), updatedDoctor.getSpecialization());

            // Update fields - combine firstName and lastName into name field
            String fullName = updatedDoctor.getFirstName();
            if (updatedDoctor.getLastName() != null && !updatedDoctor.getLastName().isEmpty()) {
                fullName = updatedDoctor.getFirstName() + " " + updatedDoctor.getLastName();
            }
            doctor.setName(fullName);

            doctor.setLicenseNumber(updatedDoctor.getLicenseNumber());
            doctor.setPhone(updatedDoctor.getContactNumber()); // contactNumber mapped to phone
            doctor.setSpecialization(updatedDoctor.getSpecialization());

            // Note: New entity uses status enum instead of validated integer
            // Setting status to INACTIVE when not validated
            doctor.setStatus(Doctor.DoctorStatus.INACTIVE);

            Doctor savedDoctor = doctorRepository.save(doctor);
            logger.info("Doctor updated successfully with ID: {}", savedDoctor.getId());

            return ResponseEntity.ok(savedDoctor);
        } else {
            logger.error("Doctor not found with ID: {}", updatedDoctor.getId());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }
}