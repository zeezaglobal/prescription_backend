package com.zeezaglobal.prescription.Service;



import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
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

    // Color scheme - Professional medical theme
    private static final DeviceRgb PRIMARY_COLOR = new DeviceRgb(22, 163, 74);      // Green
    private static final DeviceRgb SECONDARY_COLOR = new DeviceRgb(21, 128, 61);    // Darker green
    private static final DeviceRgb HEADER_BG = new DeviceRgb(240, 253, 244);        // Light green bg
    private static final DeviceRgb TABLE_HEADER_BG = new DeviceRgb(220, 252, 231);  // Green-100
    private static final DeviceRgb LIGHT_GRAY = new DeviceRgb(249, 250, 251);
    private static final DeviceRgb BORDER_COLOR = new DeviceRgb(229, 231, 235);
    private static final DeviceRgb TEXT_PRIMARY = new DeviceRgb(17, 24, 39);
    private static final DeviceRgb TEXT_SECONDARY = new DeviceRgb(107, 114, 128);

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");

    public byte[] generatePatientReportPdf(PatientReportDTO report) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc, PageSize.A4);
        document.setMargins(36, 36, 36, 36);

        try {
            PdfFont regular = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            PdfFont bold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);

            // Header Section
            addHeader(document, report, bold, regular);

            // Patient Information Section
            addPatientInformation(document, report, bold, regular);

            // Doctor Information Section
            if (report.getAssignedDoctor() != null) {
                addDoctorInformation(document, report.getAssignedDoctor(), bold, regular);
            }

            // Statistics Section
            if (report.getStatistics() != null) {
                addStatisticsSection(document, report.getStatistics(), bold, regular);
            }

            // Prescription History Section
            if (report.getPrescriptions() != null && !report.getPrescriptions().isEmpty()) {
                addPrescriptionHistory(document, report.getPrescriptions(), bold, regular);
            }

            // Footer
            addFooter(document, regular);

        } finally {
            document.close();
        }

        return baos.toByteArray();
    }

    private void addHeader(Document document, PatientReportDTO report, PdfFont bold, PdfFont regular) {
        // Main header container
        Table headerTable = new Table(UnitValue.createPercentArray(new float[]{70, 30}))
                .useAllAvailableWidth()
                .setMarginBottom(20);

        // Left side - Title and subtitle
        Cell leftCell = new Cell()
                .setBorder(Border.NO_BORDER)
                .setPadding(10)
                .setBackgroundColor(HEADER_BG);

        leftCell.add(new Paragraph("PATIENT MEDICAL REPORT")
                .setFont(bold)
                .setFontSize(24)
                .setFontColor(PRIMARY_COLOR)
                .setMarginBottom(5));

        leftCell.add(new Paragraph("Comprehensive Health Record")
                .setFont(regular)
                .setFontSize(12)
                .setFontColor(TEXT_SECONDARY));

        headerTable.addCell(leftCell);

        // Right side - Report metadata
        Cell rightCell = new Cell()
                .setBorder(Border.NO_BORDER)
                .setPadding(10)
                .setBackgroundColor(HEADER_BG)
                .setTextAlignment(TextAlignment.RIGHT);

        rightCell.add(new Paragraph("Report Generated")
                .setFont(regular)
                .setFontSize(10)
                .setFontColor(TEXT_SECONDARY));

        rightCell.add(new Paragraph(LocalDateTime.now().format(DATETIME_FORMATTER))
                .setFont(bold)
                .setFontSize(11)
                .setFontColor(TEXT_PRIMARY));

        rightCell.add(new Paragraph("Patient ID: " + report.getPatientId())
                .setFont(regular)
                .setFontSize(10)
                .setFontColor(TEXT_SECONDARY)
                .setMarginTop(5));

        headerTable.addCell(rightCell);

        document.add(headerTable);

        // Divider line
        document.add(new Div()
                .setHeight(3)
                .setBackgroundColor(PRIMARY_COLOR)
                .setMarginBottom(20));
    }

    private void addPatientInformation(Document document, PatientReportDTO report, PdfFont bold, PdfFont regular) {
        // Section header
        document.add(createSectionHeader("Patient Information", bold));

        // Patient info table
        Table infoTable = new Table(UnitValue.createPercentArray(new float[]{25, 25, 25, 25}))
                .useAllAvailableWidth()
                .setMarginBottom(20);

        // Row 1
        addInfoCell(infoTable, "Full Name", getFullName(report), bold, regular);
        addInfoCell(infoTable, "Date of Birth", formatDate(report.getDateOfBirth()), bold, regular);
        addInfoCell(infoTable, "Age", report.getAge() != null ? report.getAge() + " years" : "N/A", bold, regular);
        addInfoCell(infoTable, "Gender", report.getGender() != null ? report.getGender() : "N/A", bold, regular);

        // Row 2
        addInfoCell(infoTable, "Phone", report.getPhone() != null ? report.getPhone() : "N/A", bold, regular);
        addInfoCell(infoTable, "Email", report.getEmail() != null ? report.getEmail() : "N/A", bold, regular);
        addInfoCell(infoTable, "Blood Group", report.getBloodGroup() != null ? report.getBloodGroup() : "N/A", bold, regular);
        addInfoCell(infoTable, "Total Visits", report.getNumberOfVisits() != null ? String.valueOf(report.getNumberOfVisits()) : "0", bold, regular);

        document.add(infoTable);

        // Address (full width)
        if (report.getAddress() != null && !report.getAddress().isEmpty()) {
            Table addressTable = new Table(UnitValue.createPercentArray(new float[]{100}))
                    .useAllAvailableWidth()
                    .setMarginBottom(10);
            addInfoCell(addressTable, "Address", report.getAddress(), bold, regular);
            document.add(addressTable);
        }

        // Medical History and Allergies
        Table medicalTable = new Table(UnitValue.createPercentArray(new float[]{50, 50}))
                .useAllAvailableWidth()
                .setMarginBottom(20);

        addInfoCell(medicalTable, "Medical History",
                report.getMedicalHistory() != null && !report.getMedicalHistory().isEmpty()
                        ? report.getMedicalHistory() : "No medical history recorded",
                bold, regular);

        addInfoCell(medicalTable, "Known Allergies",
                report.getAllergies() != null && !report.getAllergies().isEmpty()
                        ? report.getAllergies() : "No allergies recorded",
                bold, regular, true);

        document.add(medicalTable);
    }

    private void addDoctorInformation(Document document, PatientReportDTO.DoctorSummary doctor, PdfFont bold, PdfFont regular) {
        document.add(createSectionHeader("Attending Physician", bold));

        Table doctorTable = new Table(UnitValue.createPercentArray(new float[]{25, 25, 25, 25}))
                .useAllAvailableWidth()
                .setMarginBottom(20);

        addInfoCell(doctorTable, "Doctor Name", doctor.getName() != null ? "Dr. " + doctor.getName() : "N/A", bold, regular);
        addInfoCell(doctorTable, "Specialization", doctor.getSpecialization() != null ? doctor.getSpecialization() : "N/A", bold, regular);
        addInfoCell(doctorTable, "Phone", doctor.getPhone() != null ? doctor.getPhone() : "N/A", bold, regular);
        addInfoCell(doctorTable, "Email", doctor.getEmail() != null ? doctor.getEmail() : "N/A", bold, regular);

        if (doctor.getClinicName() != null && !doctor.getClinicName().isEmpty()) {
            Table clinicTable = new Table(UnitValue.createPercentArray(new float[]{100}))
                    .useAllAvailableWidth();
            addInfoCell(clinicTable, "Clinic/Hospital", doctor.getClinicName(), bold, regular);
            document.add(clinicTable);
        }

        document.add(doctorTable);
    }

    private void addStatisticsSection(Document document, PatientReportDTO.PatientStatistics stats, PdfFont bold, PdfFont regular) {
        document.add(createSectionHeader("Visit Statistics", bold));

        // Statistics cards
        Table statsTable = new Table(UnitValue.createPercentArray(new float[]{20, 20, 20, 20, 20}))
                .useAllAvailableWidth()
                .setMarginBottom(15);

        addStatCard(statsTable, "Total Visits", String.valueOf(stats.getTotalVisits() != null ? stats.getTotalVisits() : 0), bold, regular, PRIMARY_COLOR);
        addStatCard(statsTable, "Active", String.valueOf(stats.getActivePrescriptions() != null ? stats.getActivePrescriptions() : 0), bold, regular, new DeviceRgb(34, 197, 94));
        addStatCard(statsTable, "Completed", String.valueOf(stats.getCompletedPrescriptions() != null ? stats.getCompletedPrescriptions() : 0), bold, regular, new DeviceRgb(59, 130, 246));
        addStatCard(statsTable, "Pending", String.valueOf(stats.getPendingPrescriptions() != null ? stats.getPendingPrescriptions() : 0), bold, regular, new DeviceRgb(234, 179, 8));
        addStatCard(statsTable, "Expired", String.valueOf(stats.getExpiredPrescriptions() != null ? stats.getExpiredPrescriptions() : 0), bold, regular, new DeviceRgb(239, 68, 68));

        document.add(statsTable);

        // Visit dates
        Table datesTable = new Table(UnitValue.createPercentArray(new float[]{50, 50}))
                .useAllAvailableWidth()
                .setMarginBottom(15);

        addInfoCell(datesTable, "First Visit", formatDate(stats.getFirstVisitDate()), bold, regular);
        addInfoCell(datesTable, "Last Visit", formatDate(stats.getLastVisitDate()), bold, regular);

        document.add(datesTable);

        // Frequent diagnoses and medications
        if ((stats.getFrequentDiagnoses() != null && !stats.getFrequentDiagnoses().isEmpty()) ||
                (stats.getFrequentMedications() != null && !stats.getFrequentMedications().isEmpty())) {

            Table frequentTable = new Table(UnitValue.createPercentArray(new float[]{50, 50}))
                    .useAllAvailableWidth()
                    .setMarginBottom(20);

            String diagnoses = stats.getFrequentDiagnoses() != null && !stats.getFrequentDiagnoses().isEmpty()
                    ? String.join(", ", stats.getFrequentDiagnoses())
                    : "None recorded";

            String medications = stats.getFrequentMedications() != null && !stats.getFrequentMedications().isEmpty()
                    ? String.join(", ", stats.getFrequentMedications())
                    : "None recorded";

            addInfoCell(frequentTable, "Frequent Diagnoses", diagnoses, bold, regular);
            addInfoCell(frequentTable, "Frequent Medications", medications, bold, regular);

            document.add(frequentTable);
        }
    }

    private void addPrescriptionHistory(Document document, List<PatientReportDTO.PrescriptionSummary> prescriptions, PdfFont bold, PdfFont regular) {
        document.add(createSectionHeader("Prescription History (" + prescriptions.size() + " Records)", bold));

        for (int i = 0; i < prescriptions.size(); i++) {
            PatientReportDTO.PrescriptionSummary prescription = prescriptions.get(i);

            // Prescription header
            Table prescHeader = new Table(UnitValue.createPercentArray(new float[]{60, 20, 20}))
                    .useAllAvailableWidth()
                    .setBackgroundColor(TABLE_HEADER_BG)
                    .setMarginTop(i > 0 ? 15 : 0);

            Cell idCell = new Cell()
                    .setBorder(Border.NO_BORDER)
                    .setPadding(8);
            idCell.add(new Paragraph("Prescription #" + prescription.getPrescriptionId())
                    .setFont(bold)
                    .setFontSize(12)
                    .setFontColor(SECONDARY_COLOR));
            prescHeader.addCell(idCell);

            Cell dateCell = new Cell()
                    .setBorder(Border.NO_BORDER)
                    .setPadding(8)
                    .setTextAlignment(TextAlignment.CENTER);
            dateCell.add(new Paragraph(formatDate(prescription.getPrescriptionDate()))
                    .setFont(regular)
                    .setFontSize(10)
                    .setFontColor(TEXT_PRIMARY));
            prescHeader.addCell(dateCell);

            Cell statusCell = new Cell()
                    .setBorder(Border.NO_BORDER)
                    .setPadding(8)
                    .setTextAlignment(TextAlignment.RIGHT);
            statusCell.add(createStatusBadge(prescription.getStatus(), bold));
            prescHeader.addCell(statusCell);

            document.add(prescHeader);

            // Prescription details
            Table detailsTable = new Table(UnitValue.createPercentArray(new float[]{25, 25, 25, 25}))
                    .useAllAvailableWidth()
                    .setBorder(new SolidBorder(BORDER_COLOR, 1));

            addInfoCell(detailsTable, "Diagnosis",
                    prescription.getDiagnosis() != null ? prescription.getDiagnosis() : "N/A",
                    bold, regular);
            addInfoCell(detailsTable, "Valid Until", formatDate(prescription.getValidUntil()), bold, regular);
            addInfoCell(detailsTable, "Prescribed By",
                    prescription.getPrescribedBy() != null ? "Dr. " + prescription.getPrescribedBy() : "N/A",
                    bold, regular);
            addInfoCell(detailsTable, "Created",
                    prescription.getCreatedAt() != null ? prescription.getCreatedAt().format(DATETIME_FORMATTER) : "N/A",
                    bold, regular);

            document.add(detailsTable);

            // Special instructions
            if (prescription.getSpecialInstructions() != null && !prescription.getSpecialInstructions().isEmpty()) {
                Table instructionsTable = new Table(UnitValue.createPercentArray(new float[]{100}))
                        .useAllAvailableWidth()
                        .setBorder(new SolidBorder(BORDER_COLOR, 1));

                Cell instrCell = new Cell()
                        .setBorder(Border.NO_BORDER)
                        .setPadding(10)
                        .setBackgroundColor(LIGHT_GRAY);

                instrCell.add(new Paragraph("Special Instructions")
                        .setFont(bold)
                        .setFontSize(10)
                        .setFontColor(TEXT_SECONDARY)
                        .setMarginBottom(3));
                instrCell.add(new Paragraph(prescription.getSpecialInstructions())
                        .setFont(regular)
                        .setFontSize(11)
                        .setFontColor(TEXT_PRIMARY));

                instructionsTable.addCell(instrCell);
                document.add(instructionsTable);
            }

            // Medications table
            if (prescription.getMedications() != null && !prescription.getMedications().isEmpty()) {
                document.add(new Paragraph("Medications")
                        .setFont(bold)
                        .setFontSize(11)
                        .setFontColor(TEXT_PRIMARY)
                        .setMarginTop(10)
                        .setMarginBottom(5));

                Table medTable = new Table(UnitValue.createPercentArray(new float[]{25, 15, 15, 15, 30}))
                        .useAllAvailableWidth()
                        .setBorder(new SolidBorder(BORDER_COLOR, 1));

                // Table header
                addTableHeader(medTable, "Medicine Name", bold);
                addTableHeader(medTable, "Dosage", bold);
                addTableHeader(medTable, "Frequency", bold);
                addTableHeader(medTable, "Duration", bold);
                addTableHeader(medTable, "Instructions", bold);

                // Table rows
                for (PatientReportDTO.MedicationItem med : prescription.getMedications()) {
                    addTableCell(medTable, med.getMedicineName() != null ? med.getMedicineName() : "N/A", regular);
                    addTableCell(medTable, med.getDosage() != null ? med.getDosage() : "N/A", regular);
                    addTableCell(medTable, med.getFrequency() != null ? med.getFrequency() : "N/A", regular);
                    addTableCell(medTable, med.getDuration() != null ? med.getDuration() : "N/A", regular);
                    addTableCell(medTable, med.getInstructions() != null ? med.getInstructions() : "-", regular);
                }

                document.add(medTable);
            }
        }
    }

    private void addFooter(Document document, PdfFont regular) {
        document.add(new Div()
                .setHeight(1)
                .setBackgroundColor(BORDER_COLOR)
                .setMarginTop(30)
                .setMarginBottom(10));

        Table footerTable = new Table(UnitValue.createPercentArray(new float[]{50, 50}))
                .useAllAvailableWidth();

        Cell leftFooter = new Cell()
                .setBorder(Border.NO_BORDER);
        leftFooter.add(new Paragraph("This is a computer-generated document.")
                .setFont(regular)
                .setFontSize(8)
                .setFontColor(TEXT_SECONDARY));
        leftFooter.add(new Paragraph("For medical purposes only. Please consult your healthcare provider.")
                .setFont(regular)
                .setFontSize(8)
                .setFontColor(TEXT_SECONDARY));
        footerTable.addCell(leftFooter);

        Cell rightFooter = new Cell()
                .setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.RIGHT);
        rightFooter.add(new Paragraph("Generated by IndigoRx")
                .setFont(regular)
                .setFontSize(8)
                .setFontColor(PRIMARY_COLOR));
        rightFooter.add(new Paragraph("© " + LocalDate.now().getYear() + " All rights reserved")
                .setFont(regular)
                .setFontSize(8)
                .setFontColor(TEXT_SECONDARY));
        footerTable.addCell(rightFooter);

        document.add(footerTable);
    }

    // Helper methods

    private Paragraph createSectionHeader(String title, PdfFont bold) {
        return new Paragraph(title)
                .setFont(bold)
                .setFontSize(14)
                .setFontColor(SECONDARY_COLOR)
                .setMarginTop(15)
                .setMarginBottom(10)
                .setBorderBottom(new SolidBorder(PRIMARY_COLOR, 2))
                .setPaddingBottom(5);
    }

    private void addInfoCell(Table table, String label, String value, PdfFont bold, PdfFont regular) {
        addInfoCell(table, label, value, bold, regular, false);
    }

    private void addInfoCell(Table table, String label, String value, PdfFont bold, PdfFont regular, boolean isAlert) {
        Cell cell = new Cell()
                .setBorder(new SolidBorder(BORDER_COLOR, 1))
                .setPadding(10)
                .setBackgroundColor(isAlert && value != null && !value.contains("No ") ? new DeviceRgb(254, 242, 242) : ColorConstants.WHITE);

        cell.add(new Paragraph(label)
                .setFont(bold)
                .setFontSize(9)
                .setFontColor(TEXT_SECONDARY)
                .setMarginBottom(2));

        cell.add(new Paragraph(value != null ? value : "N/A")
                .setFont(regular)
                .setFontSize(11)
                .setFontColor(isAlert && value != null && !value.contains("No ") ? new DeviceRgb(185, 28, 28) : TEXT_PRIMARY));

        table.addCell(cell);
    }

    private void addStatCard(Table table, String label, String value, PdfFont bold, PdfFont regular, DeviceRgb color) {
        Cell cell = new Cell()
                .setBorder(new SolidBorder(BORDER_COLOR, 1))
                .setPadding(10)
                .setTextAlignment(TextAlignment.CENTER)
                .setBackgroundColor(ColorConstants.WHITE);

        cell.add(new Paragraph(value)
                .setFont(bold)
                .setFontSize(24)
                .setFontColor(color)
                .setMarginBottom(2));

        cell.add(new Paragraph(label)
                .setFont(regular)
                .setFontSize(9)
                .setFontColor(TEXT_SECONDARY));

        table.addCell(cell);
    }

    private Paragraph createStatusBadge(String status, PdfFont bold) {
        DeviceRgb bgColor;
        DeviceRgb textColor;

        if (status == null) status = "UNKNOWN";

        switch (status.toUpperCase()) {
            case "ACTIVE":
                bgColor = new DeviceRgb(220, 252, 231);
                textColor = new DeviceRgb(21, 128, 61);
                break;
            case "COMPLETED":
                bgColor = new DeviceRgb(219, 234, 254);
                textColor = new DeviceRgb(29, 78, 216);
                break;
            case "PENDING":
                bgColor = new DeviceRgb(254, 249, 195);
                textColor = new DeviceRgb(161, 98, 7);
                break;
            case "CANCELLED":
                bgColor = new DeviceRgb(254, 226, 226);
                textColor = new DeviceRgb(185, 28, 28);
                break;
            case "EXPIRED":
                bgColor = new DeviceRgb(243, 244, 246);
                textColor = new DeviceRgb(75, 85, 99);
                break;
            default:
                bgColor = new DeviceRgb(243, 244, 246);
                textColor = new DeviceRgb(75, 85, 99);
        }

        return new Paragraph(status.toUpperCase())
                .setFont(bold)
                .setFontSize(9)
                .setFontColor(textColor)
                .setBackgroundColor(bgColor)
                .setPadding(3)
                .setPaddingLeft(8)
                .setPaddingRight(8);
    }

    private void addTableHeader(Table table, String text, PdfFont bold) {
        Cell cell = new Cell()
                .setBackgroundColor(TABLE_HEADER_BG)
                .setBorder(new SolidBorder(BORDER_COLOR, 1))
                .setPadding(8);

        cell.add(new Paragraph(text)
                .setFont(bold)
                .setFontSize(10)
                .setFontColor(SECONDARY_COLOR));

        table.addCell(cell);
    }

    private void addTableCell(Table table, String text, PdfFont regular) {
        Cell cell = new Cell()
                .setBorder(new SolidBorder(BORDER_COLOR, 1))
                .setPadding(8);

        cell.add(new Paragraph(text)
                .setFont(regular)
                .setFontSize(10)
                .setFontColor(TEXT_PRIMARY));

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
        return name.length() > 0 ? name.toString() : "N/A";
    }

    private String formatDate(LocalDate date) {
        return date != null ? date.format(DATE_FORMATTER) : "N/A";
    }
}
