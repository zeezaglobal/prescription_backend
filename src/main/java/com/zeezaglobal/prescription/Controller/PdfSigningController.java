package com.zeezaglobal.prescription.Controller;


import com.zeezaglobal.prescription.Entities.Doctor;
import com.zeezaglobal.prescription.Repository.DoctorRepository;
import com.zeezaglobal.prescription.Service.PdfSigningService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

@RestController
@RequestMapping("/api/pdf")
public class PdfSigningController {

    @Autowired
    private PdfSigningService pdfSigningService;

    @Autowired
    private DoctorRepository doctorRepository;

    @PostMapping("/sign")
    public ResponseEntity<byte[]> signPdf(@RequestParam("file") MultipartFile pdfFile) {
        try {
            // Get logged-in doctor's info from JWT
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();

            // Fetch doctor details
            Doctor doctor = doctorRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Doctor not found"));

            String doctorName = "Dr. " + doctor.getName();

            // Load keystore
            ClassPathResource keystoreResource = new ClassPathResource("test-keystore.p12");
            InputStream keystoreStream = keystoreResource.getInputStream();

            ByteArrayOutputStream outputPdf = new ByteArrayOutputStream();

            pdfSigningService.signPdf(
                    pdfFile.getInputStream(),
                    outputPdf,
                    keystoreStream,
                    "changeit".toCharArray(),
                    doctorName
            );

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=signed_" + pdfFile.getOriginalFilename())
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(outputPdf.toByteArray());

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}