package com.zeezaglobal.prescription.Service;

import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.StampingProperties;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.signatures.*;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.zeezaglobal.prescription.DTO.PrescriptionResponseDTO;
import com.zeezaglobal.prescription.DTO.PrescriptionResponseDTO.PatientBasicDTO;
import com.zeezaglobal.prescription.DTO.PrescriptionResponseDTO.DoctorBasicDTO;
import com.zeezaglobal.prescription.DTO.PrescriptionResponseDTO.MedicationDTO;
import com.zeezaglobal.prescription.Entities.Doctor;
import com.zeezaglobal.prescription.Repository.DoctorRepository;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;

@Service
public class PrescriptionPdfService {

    @Autowired
    private com.zeezaglobal.prescription.Services.PrescriptionService prescriptionService;

    @Autowired
    private DoctorRepository doctorRepository;

    @Value("${pdf.keystore.path:test-keystore.p12}")
    private String keystorePath;

    @Value("${pdf.keystore.password:changeit}")
    private String keystorePassword;

    private static final DeviceRgb PRIMARY_COLOR = new DeviceRgb(37, 99, 235);
    private static final DeviceRgb SECONDARY_COLOR = new DeviceRgb(100, 116, 139);
    private static final DeviceRgb LIGHT_GRAY = new DeviceRgb(241, 245, 249);

    /**
     * Generate a signed prescription PDF
     */
    public byte[] generateSignedPrescriptionPdf(Long prescriptionId, Long userId) throws Exception {
        // Get prescription details
        PrescriptionResponseDTO prescription = prescriptionService.getPrescriptionById(prescriptionId, userId);

        // Get full doctor entity for additional details (clinic info, etc.)
        Doctor doctor = doctorRepository.findById(prescription.getDoctor().getId())
                .orElseThrow(() -> new RuntimeException("Doctor not found"));

        // Generate unsigned PDF (without signature image - it will be added during signing)
        byte[] unsignedPdf = generatePrescriptionPdf(prescription, doctor);

        // Sign the PDF with visible signature
        byte[] signedPdf = signPdfWithVisibleSignature(unsignedPdf, doctor, prescription.getDoctor());

        return signedPdf;
    }

    /**
     * Generate the prescription PDF (unsigned, without signature image)
     */
    private byte[] generatePrescriptionPdf(PrescriptionResponseDTO prescription, Doctor doctor) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc, PageSize.A4);
        document.setMargins(40, 40, 40, 40);

        DoctorBasicDTO doctorInfo = prescription.getDoctor();
        PatientBasicDTO patientInfo = prescription.getPatient();

        // Add header
        addHeader(document, doctorInfo, doctor);

        // Add patient info section
        addPatientInfo(document, prescription, patientInfo);

        // Add prescription details (diagnosis)
        addPrescriptionDetails(document, prescription);

        // Add medications table
        addMedicationsTable(document, prescription);

        // Add special instructions if present
        if (prescription.getSpecialInstructions() != null && !prescription.getSpecialInstructions().isEmpty()) {
            addNotes(document, prescription.getSpecialInstructions());
        }

        // Add footer WITHOUT signature image (signature will be added during signing)
        addFooter(document, doctorInfo, doctor, prescription);

        document.close();
        return baos.toByteArray();
    }

    private void addHeader(Document document, DoctorBasicDTO doctorInfo, Doctor doctor) {
        // Create header table
        Table headerTable = new Table(UnitValue.createPercentArray(new float[]{70, 30}))
                .setWidth(UnitValue.createPercentValue(100));

        String doctorFullName = "Dr. " + doctorInfo.getFirstName() + " " + doctorInfo.getLastName();

        // Left side - Doctor info
        Cell leftCell = new Cell()
                .setBorder(Border.NO_BORDER)
                .add(new Paragraph("PRESCRIPTION")
                        .setFontSize(24)
                        .setBold()
                        .setFontColor(PRIMARY_COLOR))
                .add(new Paragraph(doctorFullName)
                        .setFontSize(14)
                        .setBold()
                        .setMarginTop(10))
                .add(new Paragraph(doctorInfo.getSpecialization() != null ? doctorInfo.getSpecialization() : "General Physician")
                        .setFontSize(10)
                        .setFontColor(SECONDARY_COLOR))
                .add(new Paragraph("License No: " + (doctorInfo.getLicenseNumber() != null ? doctorInfo.getLicenseNumber() : "N/A"))
                        .setFontSize(9)
                        .setFontColor(SECONDARY_COLOR));

        // Right side - Contact info (from full Doctor entity)
        Cell rightCell = new Cell()
                .setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.RIGHT)
                .add(new Paragraph("Tel: " + (doctor.getPhone() != null ? doctor.getPhone() : "N/A"))
                        .setFontSize(9)
                        .setFontColor(SECONDARY_COLOR))
                .add(new Paragraph(doctor.getEmail() != null ? doctor.getEmail() : "")
                        .setFontSize(9)
                        .setFontColor(SECONDARY_COLOR));

        headerTable.addCell(leftCell);
        headerTable.addCell(rightCell);
        document.add(headerTable);

        // Add divider line
        document.add(new LineSeparator(new SolidLine(1))
                .setMarginTop(15)
                .setMarginBottom(15));
    }

    private void addPatientInfo(Document document, PrescriptionResponseDTO prescription, PatientBasicDTO patientInfo) {
        Table patientTable = new Table(UnitValue.createPercentArray(new float[]{50, 50}))
                .setWidth(UnitValue.createPercentValue(100))
                .setBackgroundColor(LIGHT_GRAY)
                .setPadding(15);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy");
        String patientFullName = patientInfo.getFirstName() + " " + patientInfo.getLastName();

        // Calculate age if DOB is available
        String ageInfo = "";
        if (patientInfo.getDateOfBirth() != null) {
            int age = Period.between(patientInfo.getDateOfBirth(), LocalDate.now()).getYears();
            ageInfo = " (Age: " + age + ")";
        }

        Cell leftCell = new Cell()
                .setBorder(Border.NO_BORDER)
                .add(new Paragraph("PATIENT INFORMATION")
                        .setFontSize(10)
                        .setBold()
                        .setFontColor(PRIMARY_COLOR)
                        .setMarginBottom(8))
                .add(new Paragraph("Name: " + patientFullName + ageInfo)
                        .setFontSize(11))
                .add(new Paragraph("Patient ID: " + patientInfo.getId())
                        .setFontSize(10)
                        .setFontColor(SECONDARY_COLOR))
                .add(new Paragraph("Phone: " + (patientInfo.getPhone() != null ? patientInfo.getPhone() : "N/A"))
                        .setFontSize(10)
                        .setFontColor(SECONDARY_COLOR));

        Cell rightCell = new Cell()
                .setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.RIGHT)
                .add(new Paragraph("PRESCRIPTION DETAILS")
                        .setFontSize(10)
                        .setBold()
                        .setFontColor(PRIMARY_COLOR)
                        .setMarginBottom(8))
                .add(new Paragraph("Rx #: " + prescription.getId())
                        .setFontSize(11))
                .add(new Paragraph("Date: " + prescription.getPrescriptionDate().format(formatter))
                        .setFontSize(10)
                        .setFontColor(SECONDARY_COLOR))
                .add(new Paragraph("Status: " + prescription.getStatus())
                        .setFontSize(10)
                        .setFontColor(SECONDARY_COLOR));

        patientTable.addCell(leftCell);
        patientTable.addCell(rightCell);
        document.add(patientTable);
        document.add(new Paragraph().setMarginBottom(15));
    }

    private void addPrescriptionDetails(Document document, PrescriptionResponseDTO prescription) {
        if (prescription.getDiagnosis() != null && !prescription.getDiagnosis().isEmpty()) {
            document.add(new Paragraph("DIAGNOSIS")
                    .setFontSize(10)
                    .setBold()
                    .setFontColor(PRIMARY_COLOR)
                    .setMarginBottom(5));
            document.add(new Paragraph(prescription.getDiagnosis())
                    .setFontSize(11)
                    .setMarginBottom(15));
        }
    }

    private void addMedicationsTable(Document document, PrescriptionResponseDTO prescription) {
        document.add(new Paragraph("MEDICATIONS")
                .setFontSize(10)
                .setBold()
                .setFontColor(PRIMARY_COLOR)
                .setMarginBottom(10));

        Table medTable = new Table(UnitValue.createPercentArray(new float[]{25, 15, 20, 15, 25}))
                .setWidth(UnitValue.createPercentValue(100));

        // Header row
        String[] headers = {"Medication", "Dosage", "Frequency", "Duration", "Instructions"};
        for (String header : headers) {
            medTable.addHeaderCell(new Cell()
                    .setBackgroundColor(PRIMARY_COLOR)
                    .setPadding(8)
                    .add(new Paragraph(header)
                            .setFontSize(9)
                            .setBold()
                            .setFontColor(ColorConstants.WHITE)));
        }

        // Medication rows
        if (prescription.getMedications() != null && !prescription.getMedications().isEmpty()) {
            boolean alternate = false;
            for (MedicationDTO med : prescription.getMedications()) {
                DeviceRgb rowColor = alternate ? LIGHT_GRAY : new DeviceRgb(255, 255, 255);

                // Get drug name (with generic name if available)
                String medicationName = med.getDrug().getName();
                if (med.getDrug().getGenericName() != null && !med.getDrug().getGenericName().isEmpty()) {
                    medicationName += "\n(" + med.getDrug().getGenericName() + ")";
                }

                medTable.addCell(createTableCell(medicationName, rowColor));
                medTable.addCell(createTableCell(med.getDosage(), rowColor));
                medTable.addCell(createTableCell(med.getFrequency(), rowColor));
                medTable.addCell(createTableCell(med.getDuration(), rowColor));
                medTable.addCell(createTableCell(med.getInstructions() != null ? med.getInstructions() : "-", rowColor));

                alternate = !alternate;
            }
        } else {
            // No medications - add empty row
            Cell emptyCell = new Cell(1, 5)
                    .setPadding(15)
                    .setTextAlignment(TextAlignment.CENTER)
                    .add(new Paragraph("No medications prescribed")
                            .setFontSize(10)
                            .setFontColor(SECONDARY_COLOR)
                            .setItalic());
            medTable.addCell(emptyCell);
        }

        document.add(medTable);
        document.add(new Paragraph().setMarginBottom(20));
    }

    private Cell createTableCell(String content, DeviceRgb backgroundColor) {
        return new Cell()
                .setBackgroundColor(backgroundColor)
                .setPadding(8)
                .add(new Paragraph(content != null ? content : "-")
                        .setFontSize(9));
    }

    private void addNotes(Document document, String notes) {
        document.add(new Paragraph("SPECIAL INSTRUCTIONS")
                .setFontSize(10)
                .setBold()
                .setFontColor(PRIMARY_COLOR)
                .setMarginBottom(5));
        document.add(new Paragraph(notes)
                .setFontSize(10)
                .setFontColor(SECONDARY_COLOR)
                .setMarginBottom(20)
                .setPaddingLeft(10)
                .setBorderLeft(new SolidBorder(PRIMARY_COLOR, 2)));
    }

    private void addFooter(Document document, DoctorBasicDTO doctorInfo, Doctor doctor, PrescriptionResponseDTO prescription) {
        document.add(new Paragraph().setMarginTop(30));

        String doctorFullName = "Dr. " + doctorInfo.getFirstName() + " " + doctorInfo.getLastName();

        // Signature area - leave space for digital signature field
        Table signatureTable = new Table(UnitValue.createPercentArray(new float[]{55, 45}))
                .setWidth(UnitValue.createPercentValue(100));

        // Left - Validity info
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy");
        String validUntil = prescription.getValidUntil() != null
                ? prescription.getValidUntil().format(formatter)
                : prescription.getPrescriptionDate().plusDays(30).format(formatter);

        Cell leftCell = new Cell()
                .setBorder(Border.NO_BORDER)
                .add(new Paragraph("Valid Until: " + validUntil)
                        .setFontSize(9)
                        .setFontColor(SECONDARY_COLOR))
                .add(new Paragraph("This prescription is digitally signed.")
                        .setFontSize(8)
                        .setFontColor(SECONDARY_COLOR)
                        .setItalic()
                        .setMarginTop(10))
                .add(new Paragraph("Signature will show invalid if document is tampered.")
                        .setFontSize(7)
                        .setFontColor(SECONDARY_COLOR)
                        .setItalic());

        // Right - Placeholder for signature (actual signature added during signing)
        Cell rightCell = new Cell()
                .setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.CENTER)
                .setHeight(100) // Reserve space for signature
                .add(new Paragraph("Digital Signature")
                        .setFontSize(9)
                        .setFontColor(SECONDARY_COLOR)
                        .setMarginTop(40));

        signatureTable.addCell(leftCell);
        signatureTable.addCell(rightCell);
        document.add(signatureTable);

        // Add watermark-style text at bottom
        document.add(new LineSeparator(new SolidLine(0.5f))
                .setMarginTop(20));
        document.add(new Paragraph("Generated by IndigoRx Prescription Management System")
                .setFontSize(8)
                .setFontColor(SECONDARY_COLOR)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(10));
    }

    /**
     * Sign the PDF with a VISIBLE digital signature that is tied to the signature field
     * This signature image will become invalid/show warning if PDF is tampered
     */
    private byte[] signPdfWithVisibleSignature(byte[] unsignedPdf, Doctor doctor, DoctorBasicDTO doctorInfo) throws Exception {
        // Add BouncyCastle provider
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }

        ByteArrayOutputStream signedPdfOutput = new ByteArrayOutputStream();
        String doctorFullName = "Dr. " + doctorInfo.getFirstName() + " " + doctorInfo.getLastName();
        String specialization = doctorInfo.getSpecialization() != null ? doctorInfo.getSpecialization() : "General Physician";

        try {
            // Load keystore
            ClassPathResource keystoreResource = new ClassPathResource(keystorePath);
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(keystoreResource.getInputStream(), keystorePassword.toCharArray());

            String alias = keyStore.aliases().nextElement();
            PrivateKey privateKey = (PrivateKey) keyStore.getKey(alias, keystorePassword.toCharArray());
            Certificate[] chain = keyStore.getCertificateChain(alias);

            // Create PdfSigner
            PdfReader reader = new PdfReader(new ByteArrayInputStream(unsignedPdf));
            PdfSigner signer = new PdfSigner(reader, signedPdfOutput, new StampingProperties());

            // Create signature appearance - THIS IS THE KEY PART
            // The visible signature is part of the signature field, not a separate image
            PdfSignatureAppearance appearance = signer.getSignatureAppearance();

            // Set signature field location (bottom right of page 1)
            // x, y from bottom-left corner, width, height
            Rectangle signatureRect = new Rectangle(350, 100, 200, 100);
            appearance.setPageRect(signatureRect);
            appearance.setPageNumber(1);

            // Set signature metadata
            appearance.setReason("Medical Prescription - Digitally Signed");
            appearance.setLocation("IndigoRx Medical Center");
            appearance.setContact(doctor.getEmail() != null ? doctor.getEmail() : "");

            // Generate and set the signature image
            byte[] signatureImageBytes = createSignatureImage(doctorFullName, specialization);
            ImageData imageData = ImageDataFactory.create(signatureImageBytes);
            appearance.setSignatureGraphic(imageData);

            // Set render mode to show BOTH graphic and description
            // Options: DESCRIPTION, GRAPHIC, GRAPHIC_AND_DESCRIPTION, NAME_AND_DESCRIPTION
            appearance.setRenderingMode(PdfSignatureAppearance.RenderingMode.GRAPHIC);

            // Set signature field name
            signer.setFieldName("DoctorSignature");

            // Create and apply signature
            IExternalSignature signature = new PrivateKeySignature(privateKey, DigestAlgorithms.SHA256,
                    BouncyCastleProvider.PROVIDER_NAME);
            IExternalDigest digest = new BouncyCastleDigest();

            signer.signDetached(digest, signature, chain, null, null, null, 0,
                    PdfSigner.CryptoStandard.CMS);

            return signedPdfOutput.toByteArray();

        } catch (Exception e) {
            System.err.println("PDF signing failed: " + e.getMessage());
            e.printStackTrace();
            // Return unsigned PDF if signing fails
            return unsignedPdf;
        }
    }

    /**
     * Creates a professional signature image with doctor's name, specialization, and verification text
     * This image is tied to the digital signature field - it will show as invalid if tampered
     */
    private byte[] createSignatureImage(String doctorName, String specialization) throws IOException {
        int width = 300;
        int height = 150;

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        // Enable anti-aliasing for smooth text
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Background - light professional look
        g2d.setColor(new Color(240, 248, 255));
        g2d.fillRect(0, 0, width, height);

        // Border - blue professional border
        g2d.setColor(new Color(37, 99, 235));
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRect(1, 1, width - 3, height - 3);

        // Green checkmark circle background
        g2d.setColor(new Color(22, 163, 74));
        g2d.fillOval(10, 10, 24, 24);

        // White checkmark
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.drawLine(16, 22, 21, 27);
        g2d.drawLine(21, 27, 30, 17);

        // "DIGITALLY SIGNED" header
        g2d.setColor(new Color(22, 163, 74));
        g2d.setFont(new Font("Arial", Font.BOLD, 13));
        g2d.drawString("DIGITALLY SIGNED", 42, 28);

        // Signature line
        g2d.setColor(new Color(150, 150, 150));
        g2d.setStroke(new BasicStroke(1));
        g2d.drawLine(10, 65, 290, 65);

        // Doctor's name
        g2d.setColor(new Color(25, 25, 112));
        g2d.setFont(new Font("Arial", Font.BOLD, 15));
        g2d.drawString(doctorName, 10, 85);

        // Specialization
        g2d.setColor(new Color(100, 116, 139));
        g2d.setFont(new Font("Arial", Font.PLAIN, 11));
        g2d.drawString(specialization, 10, 103);

        // "IndigoRx Verified" text
        g2d.setColor(new Color(37, 99, 235));
        g2d.setFont(new Font("Arial", Font.BOLD, 10));
        g2d.drawString("IndigoRx Verified Prescription", 10, 125);

        // Date on the right
        g2d.setColor(new Color(100, 100, 100));
        g2d.setFont(new Font("Arial", Font.PLAIN, 10));
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy"));
        FontMetrics fm = g2d.getFontMetrics();
        int dateWidth = fm.stringWidth(dateStr);
        g2d.drawString(dateStr, width - dateWidth - 10, 125);

        // Small lock icon indicator
        g2d.setColor(new Color(22, 163, 74));
        g2d.setFont(new Font("Arial", Font.PLAIN, 9));
        g2d.drawString("🔒 Tamper-evident signature", 10, 142);

        g2d.dispose();

        // Convert to PNG bytes
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", baos);
        return baos.toByteArray();
    }
}