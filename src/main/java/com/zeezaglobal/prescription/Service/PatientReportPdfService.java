package com.zeezaglobal.prescription.Service;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import com.zeezaglobal.prescription.DTO.PatientReportDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Slf4j
public class PatientReportPdfService {

    // Professional Medical Color Palette
    private static final DeviceRgb PRIMARY_DARK = new DeviceRgb(15, 118, 110);       // Teal-700
    private static final DeviceRgb PRIMARY_MEDIUM = new DeviceRgb(20, 184, 166);     // Teal-500
    private static final DeviceRgb PRIMARY_LIGHT = new DeviceRgb(204, 251, 241);     // Teal-100
    private static final DeviceRgb PRIMARY_SUBTLE = new DeviceRgb(240, 253, 250);    // Teal-50

    private static final DeviceRgb ACCENT_BLUE = new DeviceRgb(59, 130, 246);        // Blue-500
    private static final DeviceRgb ACCENT_GREEN = new DeviceRgb(34, 197, 94);        // Green-500
    private static final DeviceRgb ACCENT_AMBER = new DeviceRgb(245, 158, 11);       // Amber-500
    private static final DeviceRgb ACCENT_RED = new DeviceRgb(239, 68, 68);          // Red-500

    private static final DeviceRgb TEXT_DARK = new DeviceRgb(15, 23, 42);            // Slate-900
    private static final DeviceRgb TEXT_MEDIUM = new DeviceRgb(71, 85, 105);         // Slate-600
    private static final DeviceRgb TEXT_LIGHT = new DeviceRgb(148, 163, 184);        // Slate-400

    private static final DeviceRgb BG_WHITE = new DeviceRgb(255, 255, 255);
    private static final DeviceRgb BG_GRAY = new DeviceRgb(248, 250, 252);           // Slate-50
    private static final DeviceRgb BORDER_LIGHT = new DeviceRgb(226, 232, 240);      // Slate-200
    private static final DeviceRgb BORDER_MEDIUM = new DeviceRgb(203, 213, 225);     // Slate-300

    // Alert Colors
    private static final DeviceRgb ALERT_RED_BG = new DeviceRgb(254, 242, 242);
    private static final DeviceRgb ALERT_RED_TEXT = new DeviceRgb(153, 27, 27);
    private static final DeviceRgb ALERT_RED_BORDER = new DeviceRgb(252, 165, 165);

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMMM dd, yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' hh:mm a");
    private static final DateTimeFormatter SHORT_DATE = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    public byte[] generatePatientReportPdf(PatientReportDTO report) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc, PageSize.A4);
        document.setMargins(40, 40, 50, 40);

        try {
            PdfFont regular = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            PdfFont bold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont italic = PdfFontFactory.createFont(StandardFonts.HELVETICA_OBLIQUE);

            // Document Header with Logo Area
            addDocumentHeader(document, report, bold, regular);

            // Patient Demographics Card
            addPatientDemographics(document, report, bold, regular, italic);

            // Medical Information Card
            addMedicalInformation(document, report, bold, regular);

            // Attending Physician Card
            if (report.getAssignedDoctor() != null) {
                addPhysicianInformation(document, report.getAssignedDoctor(), bold, regular);
            }

            // Visit Statistics Dashboard
            if (report.getStatistics() != null) {
                addStatisticsDashboard(document, report.getStatistics(), bold, regular);
            }

            // Prescription History
            if (report.getPrescriptions() != null && !report.getPrescriptions().isEmpty()) {
                addPrescriptionHistory(document, report.getPrescriptions(), bold, regular, italic);
            }

            // Professional Footer
            addProfessionalFooter(document, report, regular, italic);

        } finally {
            document.close();
        }

        return baos.toByteArray();
    }

    private void addDocumentHeader(Document document, PatientReportDTO report, PdfFont bold, PdfFont regular) {
        // Header Container with subtle background
        Table headerTable = new Table(UnitValue.createPercentArray(new float[]{55, 45}))
                .useAllAvailableWidth()
                .setMarginBottom(5);

        // Left: Title Section
        Cell titleCell = new Cell()
                .setBorder(Border.NO_BORDER)
                .setPadding(15)
                .setPaddingLeft(0);

        // Main Title
        Paragraph mainTitle = new Paragraph("COMPREHENSIVE")
                .setFont(bold)
                .setFontSize(11)
                .setFontColor(PRIMARY_MEDIUM)
                .setCharacterSpacing(3)
                .setMarginBottom(0);
        titleCell.add(mainTitle);

        Paragraph subTitle = new Paragraph("PATIENT MEDICAL REPORT")
                .setFont(bold)
                .setFontSize(22)
                .setFontColor(TEXT_DARK)
                .setMarginTop(-2)
                .setMarginBottom(8);
        titleCell.add(subTitle);

        // Decorative Line
        titleCell.add(new Div()
                .setWidth(80)
                .setHeight(3)
                .setBackgroundColor(PRIMARY_MEDIUM)
                .setMarginBottom(8));

        titleCell.add(new Paragraph("Complete Health Record & Treatment History")
                .setFont(regular)
                .setFontSize(10)
                .setFontColor(TEXT_MEDIUM));

        headerTable.addCell(titleCell);

        // Right: Report Metadata
        Cell metaCell = new Cell()
                .setBorder(Border.NO_BORDER)
                .setPadding(15)
                .setBackgroundColor(PRIMARY_SUBTLE)
                .setTextAlignment(TextAlignment.RIGHT);

        // Report ID
        metaCell.add(new Paragraph("REPORT ID")
                .setFont(bold)
                .setFontSize(8)
                .setFontColor(TEXT_LIGHT)
                .setCharacterSpacing(1)
                .setMarginBottom(2));

        metaCell.add(new Paragraph("RPT-" + report.getPatientId() + "-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")))
                .setFont(bold)
                .setFontSize(11)
                .setFontColor(PRIMARY_DARK)
                .setMarginBottom(12));

        // Generation Date
        metaCell.add(new Paragraph("GENERATED")
                .setFont(bold)
                .setFontSize(8)
                .setFontColor(TEXT_LIGHT)
                .setCharacterSpacing(1)
                .setMarginBottom(2));

        metaCell.add(new Paragraph(LocalDateTime.now().format(DATETIME_FORMATTER))
                .setFont(regular)
                .setFontSize(10)
                .setFontColor(TEXT_DARK));

        headerTable.addCell(metaCell);
        document.add(headerTable);

        // Accent Line
        document.add(new Div()
                .setHeight(2)
                .setBackgroundColor(PRIMARY_MEDIUM)
                .setMarginBottom(20));
    }

    private void addPatientDemographics(Document document, PatientReportDTO report, PdfFont bold, PdfFont regular, PdfFont italic) {
        // Section Header
        document.add(createSectionHeader("Patient Demographics", "Personal identification and contact details", bold, regular));

        // Main Demographics Table
        Table demoTable = new Table(UnitValue.createPercentArray(new float[]{25, 25, 25, 25}))
                .useAllAvailableWidth()
                .setMarginBottom(15);

        // Row 1: Name, DOB, Age, Gender
        addDemographicCell(demoTable, "Full Name", getFullName(report), bold, regular, true);
        addDemographicCell(demoTable, "Date of Birth", formatDate(report.getDateOfBirth()), bold, regular, false);
        addDemographicCell(demoTable, "Age", report.getAge() != null ? report.getAge() + " years" : "—", bold, regular, false);
        addDemographicCell(demoTable, "Gender", report.getGender() != null ? capitalizeFirst(report.getGender()) : "—", bold, regular, false);

        // Row 2: Contact Information
        addDemographicCell(demoTable, "Phone Number", report.getPhone() != null ? report.getPhone() : "—", bold, regular, false);
        addDemographicCell(demoTable, "Email Address", report.getEmail() != null ? report.getEmail() : "—", bold, regular, false);
        addDemographicCell(demoTable, "Blood Group", report.getBloodGroup() != null ? report.getBloodGroup() : "—", bold, regular, false);
        addDemographicCell(demoTable, "Patient ID", "PID-" + report.getPatientId(), bold, regular, false);

        document.add(demoTable);

        // Address Row
        if (report.getAddress() != null && !report.getAddress().isEmpty()) {
            Table addressTable = new Table(UnitValue.createPercentArray(new float[]{100}))
                    .useAllAvailableWidth()
                    .setMarginBottom(20);

            Cell addressCell = new Cell()
                    .setBorder(new SolidBorder(BORDER_LIGHT, 1))
                    .setPadding(12)
                    .setBackgroundColor(BG_WHITE);

            addressCell.add(new Paragraph("Residential Address")
                    .setFont(bold)
                    .setFontSize(8)
                    .setFontColor(TEXT_LIGHT)
                    .setCharacterSpacing(0.5f)
                    .setMarginBottom(4));

            addressCell.add(new Paragraph(report.getAddress())
                    .setFont(regular)
                    .setFontSize(10)
                    .setFontColor(TEXT_DARK));

            addressTable.addCell(addressCell);
            document.add(addressTable);
        }
    }

    private void addMedicalInformation(Document document, PatientReportDTO report, PdfFont bold, PdfFont regular) {
        document.add(createSectionHeader("Medical Information", "Health history and known conditions", bold, regular));

        Table medTable = new Table(UnitValue.createPercentArray(new float[]{50, 50}))
                .useAllAvailableWidth()
                .setMarginBottom(20);

        // Medical History
        Cell historyCell = new Cell()
                .setBorder(new SolidBorder(BORDER_LIGHT, 1))
                .setPadding(15)
                .setBackgroundColor(BG_WHITE);

        historyCell.add(new Paragraph("MEDICAL HISTORY")
                .setFont(bold)
                .setFontSize(9)
                .setFontColor(PRIMARY_DARK)
                .setCharacterSpacing(0.5f)
                .setMarginBottom(8));

        String medHistory = (report.getMedicalHistory() != null && !report.getMedicalHistory().isEmpty())
                ? report.getMedicalHistory()
                : "No significant medical history recorded";

        historyCell.add(new Paragraph(medHistory)
                .setFont(regular)
                .setFontSize(10)
                .setFontColor(medHistory.contains("No significant") ? TEXT_LIGHT : TEXT_DARK)
               );

        medTable.addCell(historyCell);

        // Allergies (with alert styling if allergies exist)
        boolean hasAllergies = report.getAllergies() != null && !report.getAllergies().isEmpty()
                && !report.getAllergies().toLowerCase().contains("no ") && !report.getAllergies().toLowerCase().contains("none");

        Cell allergyCell = new Cell()
                .setBorder(new SolidBorder(hasAllergies ? ALERT_RED_BORDER : BORDER_LIGHT, hasAllergies ? 2 : 1))
                .setPadding(15)
                .setBackgroundColor(hasAllergies ? ALERT_RED_BG : BG_WHITE);

        Paragraph allergyHeader = new Paragraph()
                .setMarginBottom(8);

        if (hasAllergies) {
            allergyHeader.add(new Text("⚠ ").setFontColor(ACCENT_RED));
        }
        allergyHeader.add(new Text("KNOWN ALLERGIES")
                .setFont(bold)
                .setFontSize(9)
                .setFontColor(hasAllergies ? ALERT_RED_TEXT : PRIMARY_DARK)
                .setCharacterSpacing(0.5f));

        allergyCell.add(allergyHeader);

        String allergies = (report.getAllergies() != null && !report.getAllergies().isEmpty())
                ? report.getAllergies()
                : "No known allergies (NKDA)";

        allergyCell.add(new Paragraph(allergies)
                .setFont(hasAllergies ? bold : regular)
                .setFontSize(10)
                .setFontColor(hasAllergies ? ALERT_RED_TEXT : TEXT_LIGHT)
           );

        medTable.addCell(allergyCell);

        document.add(medTable);
    }

    private void addPhysicianInformation(Document document, PatientReportDTO.DoctorSummary doctor, PdfFont bold, PdfFont regular) {
        document.add(createSectionHeader("Attending Physician", "Primary healthcare provider information", bold, regular));

        Table docTable = new Table(UnitValue.createPercentArray(new float[]{100}))
                .useAllAvailableWidth()
                .setMarginBottom(20);

        Cell docCell = new Cell()
                .setBorder(new SolidBorder(PRIMARY_LIGHT, 2))
                .setPadding(0)
                .setBackgroundColor(BG_WHITE);

        // Doctor Header
        Table innerTable = new Table(UnitValue.createPercentArray(new float[]{60, 40}))
                .useAllAvailableWidth();

        // Left: Doctor Name and Specialization
        Cell leftInner = new Cell()
                .setBorder(Border.NO_BORDER)
                .setPadding(15);

        String doctorName = doctor.getName() != null ? "Dr. " + doctor.getName() : "Not Assigned";
        leftInner.add(new Paragraph(doctorName)
                .setFont(bold)
                .setFontSize(14)
                .setFontColor(TEXT_DARK)
                .setMarginBottom(4));

        String specialization = doctor.getSpecialization() != null ? doctor.getSpecialization() : "General Practice";
        leftInner.add(new Paragraph(specialization)
                .setFont(regular)
                .setFontSize(10)
                .setFontColor(PRIMARY_DARK)
                .setMarginBottom(8));

        if (doctor.getClinicName() != null && !doctor.getClinicName().isEmpty()) {
            leftInner.add(new Paragraph(doctor.getClinicName())
                    .setFont(regular)
                    .setFontSize(9)
                    .setFontColor(TEXT_MEDIUM));
        }

        innerTable.addCell(leftInner);

        // Right: Contact Information
        Cell rightInner = new Cell()
                .setBorder(Border.NO_BORDER)
                .setPadding(15)
                .setBackgroundColor(PRIMARY_SUBTLE)
                .setTextAlignment(TextAlignment.RIGHT);

        if (doctor.getPhone() != null) {
            rightInner.add(new Paragraph("☎ " + doctor.getPhone())
                    .setFont(regular)
                    .setFontSize(10)
                    .setFontColor(TEXT_DARK)
                    .setMarginBottom(4));
        }

        if (doctor.getEmail() != null) {
            rightInner.add(new Paragraph("✉ " + doctor.getEmail())
                    .setFont(regular)
                    .setFontSize(9)
                    .setFontColor(TEXT_MEDIUM));
        }

        innerTable.addCell(rightInner);

        docCell.add(innerTable);
        docTable.addCell(docCell);
        document.add(docTable);
    }

    private void addStatisticsDashboard(Document document, PatientReportDTO.PatientStatistics stats, PdfFont bold, PdfFont regular) {
        document.add(createSectionHeader("Visit Statistics", "Summary of patient visits and prescriptions", bold, regular));

        // Statistics Cards Row
        Table statsTable = new Table(UnitValue.createPercentArray(new float[]{20, 20, 20, 20, 20}))
                .useAllAvailableWidth()
                .setMarginBottom(15);

        addStatisticCard(statsTable, "TOTAL VISITS",
                String.valueOf(stats.getTotalVisits() != null ? stats.getTotalVisits() : 0),
                PRIMARY_DARK, bold, regular);

        addStatisticCard(statsTable, "ACTIVE",
                String.valueOf(stats.getActivePrescriptions() != null ? stats.getActivePrescriptions() : 0),
                ACCENT_GREEN, bold, regular);

        addStatisticCard(statsTable, "COMPLETED",
                String.valueOf(stats.getCompletedPrescriptions() != null ? stats.getCompletedPrescriptions() : 0),
                ACCENT_BLUE, bold, regular);

        addStatisticCard(statsTable, "PENDING",
                String.valueOf(stats.getPendingPrescriptions() != null ? stats.getPendingPrescriptions() : 0),
                ACCENT_AMBER, bold, regular);

        addStatisticCard(statsTable, "EXPIRED",
                String.valueOf(stats.getExpiredPrescriptions() != null ? stats.getExpiredPrescriptions() : 0),
                ACCENT_RED, bold, regular);

        document.add(statsTable);

        // Visit Timeline
        Table timelineTable = new Table(UnitValue.createPercentArray(new float[]{50, 50}))
                .useAllAvailableWidth()
                .setMarginBottom(15);

        addTimelineCell(timelineTable, "First Visit", formatDate(stats.getFirstVisitDate()), "●", bold, regular);
        addTimelineCell(timelineTable, "Most Recent Visit", formatDate(stats.getLastVisitDate()), "◉", bold, regular);

        document.add(timelineTable);

        // Frequent Diagnoses and Medications
        if ((stats.getFrequentDiagnoses() != null && !stats.getFrequentDiagnoses().isEmpty()) ||
                (stats.getFrequentMedications() != null && !stats.getFrequentMedications().isEmpty())) {

            Table freqTable = new Table(UnitValue.createPercentArray(new float[]{50, 50}))
                    .useAllAvailableWidth()
                    .setMarginBottom(20);

            // Frequent Diagnoses
            Cell diagCell = new Cell()
                    .setBorder(new SolidBorder(BORDER_LIGHT, 1))
                    .setPadding(15)
                    .setBackgroundColor(BG_WHITE);

            diagCell.add(new Paragraph("FREQUENT DIAGNOSES")
                    .setFont(bold)
                    .setFontSize(8)
                    .setFontColor(TEXT_LIGHT)
                    .setCharacterSpacing(0.5f)
                    .setMarginBottom(10));

            if (stats.getFrequentDiagnoses() != null && !stats.getFrequentDiagnoses().isEmpty()) {
                for (String diagnosis : stats.getFrequentDiagnoses()) {
                    diagCell.add(new Paragraph("• " + diagnosis)
                            .setFont(regular)
                            .setFontSize(10)
                            .setFontColor(TEXT_DARK)
                            .setMarginBottom(3));
                }
            } else {
                diagCell.add(new Paragraph("No frequent diagnoses recorded")
                        .setFont(regular)
                        .setFontSize(10)
                        .setFontColor(TEXT_LIGHT));
            }

            freqTable.addCell(diagCell);

            // Frequent Medications
            Cell medCell = new Cell()
                    .setBorder(new SolidBorder(BORDER_LIGHT, 1))
                    .setPadding(15)
                    .setBackgroundColor(BG_WHITE);

            medCell.add(new Paragraph("FREQUENT MEDICATIONS")
                    .setFont(bold)
                    .setFontSize(8)
                    .setFontColor(TEXT_LIGHT)
                    .setCharacterSpacing(0.5f)
                    .setMarginBottom(10));

            if (stats.getFrequentMedications() != null && !stats.getFrequentMedications().isEmpty()) {
                for (String medication : stats.getFrequentMedications()) {
                    medCell.add(new Paragraph("• " + medication)
                            .setFont(regular)
                            .setFontSize(10)
                            .setFontColor(TEXT_DARK)
                            .setMarginBottom(3));
                }
            } else {
                medCell.add(new Paragraph("No frequent medications recorded")
                        .setFont(regular)
                        .setFontSize(10)
                        .setFontColor(TEXT_LIGHT));
            }

            freqTable.addCell(medCell);

            document.add(freqTable);
        }
    }

    private void addPrescriptionHistory(Document document, List<PatientReportDTO.PrescriptionSummary> prescriptions, PdfFont bold, PdfFont regular, PdfFont italic) {
        document.add(createSectionHeader("Prescription History",
                prescriptions.size() + " prescription record" + (prescriptions.size() > 1 ? "s" : "") + " on file",
                bold, regular));

        for (int i = 0; i < prescriptions.size(); i++) {
            PatientReportDTO.PrescriptionSummary rx = prescriptions.get(i);

            // Prescription Card Container
            Table rxCard = new Table(UnitValue.createPercentArray(new float[]{100}))
                    .useAllAvailableWidth()
                    .setMarginBottom(15);

            Cell cardCell = new Cell()
                    .setBorder(new SolidBorder(BORDER_MEDIUM, 1))
                    .setPadding(0)
                    .setBackgroundColor(BG_WHITE);

            // Card Header
            Table headerRow = new Table(UnitValue.createPercentArray(new float[]{50, 25, 25}))
                    .useAllAvailableWidth()
                    .setBackgroundColor(BG_GRAY);

            // Prescription ID
            Cell idCell = new Cell()
                    .setBorder(Border.NO_BORDER)
                    .setPadding(12);

            idCell.add(new Paragraph("RX-" + rx.getPrescriptionId())
                    .setFont(bold)
                    .setFontSize(12)
                    .setFontColor(PRIMARY_DARK));

            if (rx.getPrescribedBy() != null) {
                idCell.add(new Paragraph("Prescribed by Dr. " + rx.getPrescribedBy())
                        .setFont(regular)
                        .setFontSize(9)
                        .setFontColor(TEXT_MEDIUM)
                        .setMarginTop(2));
            }

            headerRow.addCell(idCell);

            // Date
            Cell dateCell = new Cell()
                    .setBorder(Border.NO_BORDER)
                    .setPadding(12)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setVerticalAlignment(VerticalAlignment.MIDDLE);

            dateCell.add(new Paragraph(formatDateShort(rx.getPrescriptionDate()))
                    .setFont(regular)
                    .setFontSize(10)
                    .setFontColor(TEXT_DARK));

            headerRow.addCell(dateCell);

            // Status Badge
            Cell statusCell = new Cell()
                    .setBorder(Border.NO_BORDER)
                    .setPadding(12)
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setVerticalAlignment(VerticalAlignment.MIDDLE);

            statusCell.add(createStatusBadge(rx.getStatus(), bold));

            headerRow.addCell(statusCell);

            cardCell.add(headerRow);

            // Card Body
            Table bodyTable = new Table(UnitValue.createPercentArray(new float[]{100}))
                    .useAllAvailableWidth();

            Cell bodyCell = new Cell()
                    .setBorder(Border.NO_BORDER)
                    .setPadding(15);

            // Diagnosis
            if (rx.getDiagnosis() != null && !rx.getDiagnosis().isEmpty()) {
                Table diagRow = new Table(UnitValue.createPercentArray(new float[]{15, 85}))
                        .useAllAvailableWidth()
                        .setMarginBottom(10);

                Cell labelCell = new Cell()
                        .setBorder(Border.NO_BORDER)
                        .add(new Paragraph("Diagnosis")
                                .setFont(bold)
                                .setFontSize(9)
                                .setFontColor(TEXT_LIGHT));
                diagRow.addCell(labelCell);

                Cell valueCell = new Cell()
                        .setBorder(Border.NO_BORDER)
                        .add(new Paragraph(rx.getDiagnosis())
                                .setFont(regular)
                                .setFontSize(10)
                                .setFontColor(TEXT_DARK));
                diagRow.addCell(valueCell);

                bodyCell.add(diagRow);
            }

            // Valid Until
            Table validRow = new Table(UnitValue.createPercentArray(new float[]{15, 35, 15, 35}))
                    .useAllAvailableWidth()
                    .setMarginBottom(10);

            addCompactInfoPair(validRow, "Valid Until", formatDateShort(rx.getValidUntil()), bold, regular);
            addCompactInfoPair(validRow, "Created", rx.getCreatedAt() != null ? rx.getCreatedAt().format(SHORT_DATE) : "—", bold, regular);

            bodyCell.add(validRow);

            // Special Instructions
            if (rx.getSpecialInstructions() != null && !rx.getSpecialInstructions().isEmpty()) {
                Table instrTable = new Table(UnitValue.createPercentArray(new float[]{100}))
                        .useAllAvailableWidth()
                        .setMarginTop(10)
                        .setMarginBottom(10);

                Cell instrCell = new Cell()
                        .setBorder(new SolidBorder(ACCENT_AMBER, 1))
                        .setBorderLeft(new SolidBorder(ACCENT_AMBER, 3))
                        .setPadding(10)
                        .setBackgroundColor(new DeviceRgb(255, 251, 235)); // Amber-50

                instrCell.add(new Paragraph("Special Instructions")
                        .setFont(bold)
                        .setFontSize(8)
                        .setFontColor(new DeviceRgb(180, 83, 9)) // Amber-700
                        .setCharacterSpacing(0.5f)
                        .setMarginBottom(4));

                instrCell.add(new Paragraph(rx.getSpecialInstructions())
                        .setFont(italic)
                        .setFontSize(10)
                        .setFontColor(new DeviceRgb(120, 53, 15))); // Amber-900

                instrTable.addCell(instrCell);
                bodyCell.add(instrTable);
            }

            // Medications Table
            if (rx.getMedications() != null && !rx.getMedications().isEmpty()) {
                bodyCell.add(new Paragraph("PRESCRIBED MEDICATIONS")
                        .setFont(bold)
                        .setFontSize(9)
                        .setFontColor(TEXT_LIGHT)
                        .setCharacterSpacing(0.5f)
                        .setMarginTop(10)
                        .setMarginBottom(8));

                Table medTable = new Table(UnitValue.createPercentArray(new float[]{28, 14, 16, 14, 28}))
                        .useAllAvailableWidth();

                // Table Header
                addMedicationTableHeader(medTable, "Medication", bold);
                addMedicationTableHeader(medTable, "Dosage", bold);
                addMedicationTableHeader(medTable, "Frequency", bold);
                addMedicationTableHeader(medTable, "Duration", bold);
                addMedicationTableHeader(medTable, "Instructions", bold);

                // Table Rows
                boolean isAlternate = false;
                for (PatientReportDTO.MedicationItem med : rx.getMedications()) {
                    addMedicationTableCell(medTable, med.getMedicineName() != null ? med.getMedicineName() : "—", regular, isAlternate, true);
                    addMedicationTableCell(medTable, med.getDosage() != null ? med.getDosage() : "—", regular, isAlternate, false);
                    addMedicationTableCell(medTable, med.getFrequency() != null ? med.getFrequency() : "—", regular, isAlternate, false);
                    addMedicationTableCell(medTable, med.getDuration() != null ? med.getDuration() : "—", regular, isAlternate, false);
                    addMedicationTableCell(medTable, med.getInstructions() != null ? med.getInstructions() : "—", regular, isAlternate, false);
                    isAlternate = !isAlternate;
                }

                bodyCell.add(medTable);
            }

            bodyTable.addCell(bodyCell);
            cardCell.add(bodyTable);
            rxCard.addCell(cardCell);
            document.add(rxCard);
        }
    }

    private void addProfessionalFooter(Document document, PatientReportDTO report, PdfFont regular, PdfFont italic) {
        // Separator
        document.add(new Div()
                .setHeight(1)
                .setBackgroundColor(BORDER_LIGHT)
                .setMarginTop(25)
                .setMarginBottom(15));

        Table footerTable = new Table(UnitValue.createPercentArray(new float[]{60, 40}))
                .useAllAvailableWidth();

        // Left: Disclaimer
        Cell leftCell = new Cell()
                .setBorder(Border.NO_BORDER);

        leftCell.add(new Paragraph("CONFIDENTIAL MEDICAL DOCUMENT")
                .setFont(italic)
                .setFontSize(7)
                .setFontColor(TEXT_LIGHT)
                .setCharacterSpacing(0.5f)
                .setMarginBottom(3));

        leftCell.add(new Paragraph("This document contains protected health information. Unauthorized disclosure is prohibited by law.")
                .setFont(regular)
                .setFontSize(7)
                .setFontColor(TEXT_LIGHT)
                .setMarginBottom(2));

        leftCell.add(new Paragraph("This is a computer-generated report. Please consult your healthcare provider for medical advice.")
                .setFont(regular)
                .setFontSize(7)
                .setFontColor(TEXT_LIGHT));

        footerTable.addCell(leftCell);

        // Right: Branding
        Cell rightCell = new Cell()
                .setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.RIGHT);

        rightCell.add(new Paragraph("IndigoRx")
                .setFont(italic)
                .setFontSize(14)
                .setFontColor(PRIMARY_MEDIUM)
                .setMarginBottom(2));

        rightCell.add(new Paragraph("Healthcare Management System")
                .setFont(regular)
                .setFontSize(7)
                .setFontColor(TEXT_LIGHT)
                .setMarginBottom(2));

        rightCell.add(new Paragraph("© " + LocalDate.now().getYear() + " All rights reserved")
                .setFont(regular)
                .setFontSize(7)
                .setFontColor(TEXT_LIGHT));

        footerTable.addCell(rightCell);

        document.add(footerTable);
    }

    // ==================== HELPER METHODS ====================

    private Paragraph createSectionHeader(String title, String subtitle, PdfFont bold, PdfFont regular) {
        Div container = new Div()
                .setMarginTop(20)
                .setMarginBottom(12);

        Paragraph header = new Paragraph()
                .add(new Text(title)
                        .setFont(bold)
                        .setFontSize(13)
                        .setFontColor(TEXT_DARK))
                .setMarginBottom(2);

        container.add(header);

        if (subtitle != null && !subtitle.isEmpty()) {
            container.add(new Paragraph(subtitle)
                    .setFont(regular)
                    .setFontSize(9)
                    .setFontColor(TEXT_MEDIUM)
                    .setMarginBottom(8));
        }

        container.add(new Div()
                .setWidth(50)
                .setHeight(2)
                .setBackgroundColor(PRIMARY_MEDIUM));

        // Return as part of a Paragraph to add to document
        Paragraph result = new Paragraph()
                .add(new Text(title)
                        .setFont(bold)
                        .setFontSize(13)
                        .setFontColor(TEXT_DARK))
                .setMarginTop(20)
                .setMarginBottom(2);

        return result;
    }

    private void addDemographicCell(Table table, String label, String value, PdfFont bold, PdfFont regular, boolean isFirst) {
        Cell cell = new Cell()
                .setBorder(new SolidBorder(BORDER_LIGHT, 1))
                .setPadding(12)
                .setBackgroundColor(BG_WHITE);

        cell.add(new Paragraph(label.toUpperCase())
                .setFont(bold)
                .setFontSize(8)
                .setFontColor(TEXT_LIGHT)
                .setCharacterSpacing(0.3f)
                .setMarginBottom(4));

        cell.add(new Paragraph(value != null ? value : "—")
                .setFont(isFirst ? bold : regular)
                .setFontSize(isFirst ? 11 : 10)
                .setFontColor(TEXT_DARK));

        table.addCell(cell);
    }

    private void addStatisticCard(Table table, String label, String value, DeviceRgb color, PdfFont bold, PdfFont regular) {
        Cell cell = new Cell()
                .setBorder(new SolidBorder(BORDER_LIGHT, 1))
                .setPadding(12)
                .setTextAlignment(TextAlignment.CENTER)
                .setBackgroundColor(BG_WHITE);

        cell.add(new Paragraph(value)
                .setFont(bold)
                .setFontSize(28)
                .setFontColor(color)
                .setMarginBottom(4));

        cell.add(new Paragraph(label)
                .setFont(bold)
                .setFontSize(7)
                .setFontColor(TEXT_LIGHT)
                .setCharacterSpacing(0.5f));

        table.addCell(cell);
    }

    private void addTimelineCell(Table table, String label, String value, String icon, PdfFont bold, PdfFont regular) {
        Cell cell = new Cell()
                .setBorder(new SolidBorder(BORDER_LIGHT, 1))
                .setPadding(12)
                .setBackgroundColor(BG_WHITE);

        Paragraph p = new Paragraph()
                .add(new Text(icon + " ")
                        .setFont(regular)
                        .setFontSize(10)
                        .setFontColor(PRIMARY_MEDIUM))
                .add(new Text(label)
                        .setFont(bold)
                        .setFontSize(9)
                        .setFontColor(TEXT_LIGHT))
                .setMarginBottom(4);

        cell.add(p);

        cell.add(new Paragraph(value)
                .setFont(regular)
                .setFontSize(11)
                .setFontColor(TEXT_DARK));

        table.addCell(cell);
    }

    private void addCompactInfoPair(Table table, String label, String value, PdfFont bold, PdfFont regular) {
        Cell labelCell = new Cell()
                .setBorder(Border.NO_BORDER)
                .add(new Paragraph(label)
                        .setFont(bold)
                        .setFontSize(9)
                        .setFontColor(TEXT_LIGHT));
        table.addCell(labelCell);

        Cell valueCell = new Cell()
                .setBorder(Border.NO_BORDER)
                .add(new Paragraph(value)
                        .setFont(regular)
                        .setFontSize(10)
                        .setFontColor(TEXT_DARK));
        table.addCell(valueCell);
    }

    private Paragraph createStatusBadge(String status, PdfFont bold) {
        DeviceRgb bgColor;
        DeviceRgb textColor;

        if (status == null) status = "UNKNOWN";

        switch (status.toUpperCase()) {
            case "ACTIVE":
                bgColor = new DeviceRgb(220, 252, 231);  // Green-100
                textColor = new DeviceRgb(21, 128, 61);  // Green-700
                break;
            case "COMPLETED":
                bgColor = new DeviceRgb(219, 234, 254);  // Blue-100
                textColor = new DeviceRgb(29, 78, 216);  // Blue-700
                break;
            case "PENDING":
                bgColor = new DeviceRgb(254, 249, 195);  // Yellow-100
                textColor = new DeviceRgb(161, 98, 7);   // Yellow-700
                break;
            case "CANCELLED":
                bgColor = new DeviceRgb(254, 226, 226);  // Red-100
                textColor = new DeviceRgb(185, 28, 28);  // Red-700
                break;
            case "EXPIRED":
                bgColor = new DeviceRgb(243, 244, 246);  // Gray-100
                textColor = new DeviceRgb(75, 85, 99);   // Gray-600
                break;
            default:
                bgColor = new DeviceRgb(243, 244, 246);
                textColor = new DeviceRgb(75, 85, 99);
        }

        return new Paragraph(status.toUpperCase())
                .setFont(bold)
                .setFontSize(8)
                .setFontColor(textColor)
                .setBackgroundColor(bgColor)
                .setPadding(4)
                .setPaddingLeft(10)
                .setPaddingRight(10)
                .setCharacterSpacing(0.5f);
    }

    private void addMedicationTableHeader(Table table, String text, PdfFont bold) {
        Cell cell = new Cell()
                .setBackgroundColor(PRIMARY_SUBTLE)
                .setBorder(new SolidBorder(BORDER_LIGHT, 1))
                .setPadding(8);

        cell.add(new Paragraph(text.toUpperCase())
                .setFont(bold)
                .setFontSize(8)
                .setFontColor(PRIMARY_DARK)
                .setCharacterSpacing(0.3f));

        table.addCell(cell);
    }

    private void addMedicationTableCell(Table table, String text, PdfFont regular, boolean isAlternate, boolean isBold) {
        Cell cell = new Cell()
                .setBorder(new SolidBorder(BORDER_LIGHT, 1))
                .setPadding(8)
                .setBackgroundColor(isAlternate ? BG_GRAY : BG_WHITE);

        PdfFont font = regular;
        try {
            if (isBold) {
                font = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            }
        } catch (IOException e) {
            // Use regular font as fallback
        }

        cell.add(new Paragraph(text)
                .setFont(font)
                .setFontSize(9)
                .setFontColor(TEXT_DARK));

        table.addCell(cell);
    }

    private String getFullName(PatientReportDTO report) {
        if (report.getName() != null && !report.getName().isEmpty()) {
            return report.getName();
        }
        StringBuilder name = new StringBuilder();
        if (report.getFirstName() != null) {
            name.append(report.getFirstName());
        }
        if (report.getLastName() != null) {
            if (name.length() > 0) name.append(" ");
            name.append(report.getLastName());
        }
        return name.length() > 0 ? name.toString() : "—";
    }

    private String formatDate(LocalDate date) {
        return date != null ? date.format(DATE_FORMATTER) : "—";
    }

    private String formatDateShort(LocalDate date) {
        return date != null ? date.format(SHORT_DATE) : "—";
    }

    private String capitalizeFirst(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
}