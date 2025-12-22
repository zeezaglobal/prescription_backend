package com.zeezaglobal.prescription.Service;

import com.zeezaglobal.prescription.DTO.PatientDTO;
import com.zeezaglobal.prescription.Entities.Doctor;
import com.zeezaglobal.prescription.Entities.Patient;
import com.zeezaglobal.prescription.Repository.DoctorRepository;
import com.zeezaglobal.prescription.Repository.PatientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class PatientService {

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    private static final Logger logger = LoggerFactory.getLogger(PatientService.class);

    public Page<Patient> getAllPatients(Pageable pageable) {
        return patientRepository.findAll(pageable);
    }

    public Optional<Patient> getPatientById(Long id) {
        return patientRepository.findById(id);
    }

    public Page<PatientDTO> getPatientsByDoctorId(Long doctorId, Pageable pageable) {
        logger.info("Fetching patients for doctor ID: {}", doctorId);

        Page<Patient> patients = patientRepository.findByDoctorId(doctorId, pageable);

        return patients.map(this::convertToDTO);
    }

    public Page<PatientDTO> getPatientsByAuthenticatedDoctor(String username, Pageable pageable) {
        logger.info("Fetching patients for authenticated doctor: {}", username);

        Doctor doctor = doctorRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Doctor not found with username: " + username));

        Page<Patient> patients = patientRepository.findByDoctorId(doctor.getId(), pageable);

        return patients.map(this::convertToDTO);
    }

    public List<Patient> getPatientsByDoctorIdAsList(Long doctorId) {
        logger.info("Fetching all patients for doctor ID: {}", doctorId);
        return patientRepository.findByDoctorId(doctorId);
    }

    public Patient savePatient(Patient patient) {
        logger.info("Saving patient: {}", patient.getName());

        // Increment visit count if this is an existing patient
        if (patient.getId() != null) {
            Optional<Patient> existingPatient = patientRepository.findById(patient.getId());
            if (existingPatient.isPresent()) {
                patient.setNumberOfVisit(existingPatient.get().getNumberOfVisit() + 1);
            }
        } else {
            // New patient, set visit count to 0
            if (patient.getNumberOfVisit() == null) {
                patient.setNumberOfVisit(0);
            }
        }

        return patientRepository.save(patient);
    }

    public Patient createPatientForDoctor(Patient patient, Long doctorId) {
        logger.info("Creating patient for doctor ID: {}", doctorId);

        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new RuntimeException("Doctor not found with ID: " + doctorId));

        patient.setDoctor(doctor);
        patient.setNumberOfVisit(0);

        // Set default username if not provided (use email or generate one)
        if (patient.getUsername() == null || patient.getUsername().isEmpty()) {
            if (patient.getEmail() != null && !patient.getEmail().isEmpty()) {
                patient.setUsername(patient.getEmail());
            } else {
                // Generate a unique username based on phone and timestamp
                patient.setUsername("patient_" + patient.getPhone() + "_" + System.currentTimeMillis());
            }
        }

        // Set default password for patients (they don't need to login)
        if (patient.getPassword() == null || patient.getPassword().isEmpty()) {
            String defaultPassword = "Patient@123"; // Default password for all patients
            patient.setPassword(defaultPassword); // Password will be encoded in User entity's @PrePersist
        }

        return patientRepository.save(patient);
    }

    public Patient createPatientForAuthenticatedDoctor(Patient patient, String username) {
        logger.info("Creating patient for authenticated doctor: {}", username);

        Doctor doctor = doctorRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Doctor not found with username: " + username));

        patient.setDoctor(doctor);
        patient.setNumberOfVisit(0);

        // Set default username if not provided (use email or generate one)
        if (patient.getUsername() == null || patient.getUsername().isEmpty()) {
            if (patient.getEmail() != null && !patient.getEmail().isEmpty()) {
                patient.setUsername(patient.getEmail());
            } else {
                // Generate a unique username based on phone and timestamp
                patient.setUsername("patient_" + patient.getPhone() + "_" + System.currentTimeMillis());
            }
        }

        // Set default password for patients (they don't need to login)
        // Using a standard default password that can be changed later if needed
        if (patient.getPassword() == null || patient.getPassword().isEmpty()) {
            String defaultPassword = "Patient@123"; // Default password for all patients
            patient.setPassword(defaultPassword); // Password will be encoded in User entity's @PrePersist
        }

        return patientRepository.save(patient);
    }

    public void deletePatient(Long id) {
        logger.info("Deleting patient with ID: {}", id);
        patientRepository.deleteById(id);
    }

    public List<Patient> searchPatients(String searchTerm) {
        logger.info("Searching patients with term: {}", searchTerm);

        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            logger.warn("Search term is empty");
            return List.of();
        }

        List<Patient> patients = patientRepository.searchPatients(searchTerm.trim());

        if (patients.isEmpty()) {
            logger.info("No patients found for search term: {}", searchTerm);
        } else {
            logger.info("Found {} patient(s) matching the search term", patients.size());
        }

        return patients;
    }

    public List<Patient> searchPatients(String firstName, String lastName, String contactNumber) {
        logger.info("Searching patients with first name: {}, last name: {}, contact number: {}",
                firstName, lastName, contactNumber);

        List<Patient> patients = patientRepository
                .findByFirstNameContainingOrLastNameContainingOrContactNumberContaining(
                        firstName != null ? firstName : "",
                        lastName != null ? lastName : "",
                        contactNumber != null ? contactNumber : ""
                );

        if (patients.isEmpty()) {
            logger.info("No patients found for the given search criteria.");
        } else {
            logger.info("Found {} patient(s) matching the criteria.", patients.size());
        }

        return patients;
    }

    public Page<PatientDTO> searchPatientsByDoctor(Long doctorId, String searchTerm, Pageable pageable) {
        logger.info("Searching patients for doctor ID: {} with term: {}", doctorId, searchTerm);

        Page<Patient> patients = patientRepository.searchPatientsByDoctor(doctorId, searchTerm, pageable);

        return patients.map(this::convertToDTO);
    }

    public Page<PatientDTO> searchPatientsByAuthenticatedDoctor(String username, String searchTerm, Pageable pageable) {
        logger.info("Searching patients for authenticated doctor: {} with term: {}", username, searchTerm);

        Doctor doctor = doctorRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Doctor not found with username: " + username));

        Page<Patient> patients = patientRepository.searchPatientsByDoctor(doctor.getId(), searchTerm, pageable);

        return patients.map(this::convertToDTO);
    }

    public void incrementPatientVisit(Long patientId) {
        logger.info("Incrementing visit count for patient ID: {}", patientId);

        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new RuntimeException("Patient not found with ID: " + patientId));

        patient.setNumberOfVisit(patient.getNumberOfVisit() + 1);
        patientRepository.save(patient);
    }

    // Helper method to convert Patient entity to PatientDTO
    private PatientDTO convertToDTO(Patient patient) {
        PatientDTO dto = new PatientDTO();
        dto.setId(patient.getId());
        dto.setNumberOfVisit(patient.getNumberOfVisit());
        dto.setFirstName(patient.getFirstName());
        dto.setLastName(patient.getLastName());
        dto.setDateOfBirth(patient.getDateOfBirth());
        dto.setGender(patient.getGender());
        dto.setContactNumber(patient.getContactNumber());
        dto.setEmail(patient.getEmail());
        dto.setAddress(patient.getAddress());
        dto.setMedicalHistory(patient.getMedicalHistory());
        dto.setBloodGroup(patient.getBloodGroup());
        dto.setAllergies(patient.getAllergies());
        dto.setAge(patient.getAge());
        dto.setDoctorId(patient.getDoctor() != null ? patient.getDoctor().getId() : null);

        return dto;
    }
}