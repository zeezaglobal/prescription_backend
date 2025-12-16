package com.zeezaglobal.prescription.Service;

import com.zeezaglobal.prescription.DTO.*;
import com.zeezaglobal.prescription.Entities.Drug;
import com.zeezaglobal.prescription.Entities.Patient;
import com.zeezaglobal.prescription.Repository.DrugRepository;
import com.zeezaglobal.prescription.Repository.PatientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;


@Service
public class PatientService {

    @Autowired
    private PatientRepository patientRepository;
    private static final Logger logger = LoggerFactory.getLogger(PatientService.class);

    public List<Patient> getAllPatients() {
        return patientRepository.findAll();
    }

    public Optional<Patient> getPatientById(Long id) {
        return patientRepository.findById(id);
    }

    public Page<PatientDTO> getPatientsByDoctorId(Long doctorId, Pageable pageable) {
        Page<Patient> patients = patientRepository.findByDoctorId(doctorId, pageable);

        return patients.map(patient ->
                new PatientDTO(
                        patient.getId(),
                        patient.getNumberOfVisit(),
                        patient.getFirstName(),
                        patient.getLastName(),
                        patient.getDateOfBirth(),
                        patient.getGender(),
                        patient.getContactNumber(),
                        patient.getEmail(),
                        patient.getAddress(),
                        patient.getMedicalHistory(),
                        patient.getDoctor().getId()
                )
        );

    }

    public Patient savePatient(Patient patient) {
        return patientRepository.save(patient);
    }

    public void deletePatient(Long id) {
        patientRepository.deleteById(id);
    }

    public List<Patient> searchPatients(String firstName, String lastName, String contactNumber) {
        // Log the search parameters
        logger.info("Searching patients with first name: {}, last name: {}, contact number: {}", firstName, lastName, contactNumber);

        List<Patient> patients = patientRepository.findByFirstNameContainingOrLastNameContainingOrContactNumberContaining(firstName, lastName, contactNumber);

        if (patients.isEmpty()) {
            logger.info("No patients found for the given search criteria.");
        } else {
            logger.info("Found {} patient(s) matching the criteria.", patients.size());
        }

        return patients;
    }



}
