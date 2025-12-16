package com.zeezaglobal.prescription.Controller;

import com.zeezaglobal.prescription.DTO.PatientDTO;
import com.zeezaglobal.prescription.Entities.Patient;
import com.zeezaglobal.prescription.Service.PatientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;



@RestController
@RequestMapping("/api/patients")
public class PatientsController {

    @Autowired
    private PatientService patientService;

    @GetMapping
    public ResponseEntity<List<Patient>> getAllPatients() {
        return ResponseEntity.ok(patientService.getAllPatients());
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchPatients(
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) String contactNumber) {

        List<Patient> patients = patientService.searchPatients(firstName, lastName, contactNumber);

        if (patients.isEmpty()) {
            return new ResponseEntity<>("No patients found for the given search criteria.", HttpStatus.NOT_FOUND);
        } else {
            return new ResponseEntity<>(patients, HttpStatus.OK);
        }
    }
    @GetMapping("/{id}")
    public ResponseEntity<Patient> getPatientById(@PathVariable Long id) {
        Optional<Patient> patient = patientService.getPatientById(id);
        return patient.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/doctor/{doctorId}")
    public ResponseEntity<Page<PatientDTO>> getPatientsByDoctorId(
            @PathVariable Long doctorId,
            Pageable pageable) {
        return ResponseEntity.ok(patientService.getPatientsByDoctorId(doctorId, pageable));
    }

    @PostMapping
    public ResponseEntity<Patient> createPatient(@RequestBody Patient patient) {
        return ResponseEntity.ok(patientService.savePatient(patient));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Patient> updatePatient(@PathVariable Long id, @RequestBody Patient updatedPatient) {
        Optional<Patient> existingPatient = patientService.getPatientById(id);
        if (existingPatient.isPresent()) {
            updatedPatient.setId(id);
            return ResponseEntity.ok(patientService.savePatient(updatedPatient));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePatient(@PathVariable Long id) {
        patientService.deletePatient(id);
        return ResponseEntity.noContent().build();
    }
}
