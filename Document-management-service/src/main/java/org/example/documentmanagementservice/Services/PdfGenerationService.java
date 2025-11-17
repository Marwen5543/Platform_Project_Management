package org.example.documentmanagementservice.Services;

import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
public class PdfGenerationService {

    @Value("${company.name:Tunisys}")
    private String companyName;

    @Value("${company.address:Avenue de la Liberté, Tunis, TN}")
    private String companyAddress;

    @Value("${company.contact:contact@tunisys.com.tn}")
    private String companyContact;

    @Value("${company.phone:+216 71 XXX XXX}")
    private String companyPhone;

    private static final String LOGO_PATH = "C:/Users/Marwen/Desktop/Sprint 1/tunisys.png";

    // Tunisys Brand Colors - Black, Red, White
    private static final Color TUNISYS_RED = new DeviceRgb(220, 20, 60);         // Crimson Red
    private static final Color TUNISYS_BLACK = ColorConstants.BLACK;
    private static final Color LIGHT_GRAY = new DeviceRgb(245, 245, 245);

    private static final float HEADER_FONT_SIZE = 18;
    private static final float TITLE_FONT_SIZE = 22;
    private static final float BODY_FONT_SIZE = 11;
    private static final float SMALL_FONT_SIZE = 9;

    public byte[] generatePayslip(String employeeId, String monthYear, String firstName, String lastName) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = initDocument(pdf);

            addProfessionalHeader(document, "MONTHLY PAYSLIP");
            addEmployeeInfo(document, firstName, lastName, monthYear);
            addPayslipTable(document);
            addProfessionalFooter(document);

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            handlePdfError(employeeId, "payslip", e);
            throw new RuntimeException("PDF generation failed", e);
        }
    }

    public byte[] generateWorkAttestation(String employeeId, String firstName, String lastName) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = initDocument(pdf);

            addProfessionalHeader(document, "EMPLOYMENT ATTESTATION");
            addDateReference(document);
            addAttestationContent(document, firstName, lastName);
            addSignatureSection(document);
            addProfessionalFooter(document);

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            handlePdfError(employeeId, "work attestation", e);
            throw new RuntimeException("PDF generation failed", e);
        }
    }

    public byte[] generateCertificate(String employeeId, String firstName, String lastName) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = initDocument(pdf);

            addCertificateBorder(document);
            addProfessionalHeader(document, "CERTIFICATE OF EMPLOYMENT");
            addCertificateContent(document, firstName, lastName);
            addCertificateSignature(document);
            addProfessionalFooter(document);

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            handlePdfError(employeeId, "certificate", e);
            throw new RuntimeException("PDF generation failed", e);
        }
    }

    private Document initDocument(PdfDocument pdf) {
        Document document = new Document(pdf, PageSize.A4);
        document.setMargins(40, 40, 40, 40);
        return document;
    }

    private void addProfessionalHeader(Document document, String title) throws IOException {
        // Red top bar
        Table redBar = new Table(1)
                .setWidth(UnitValue.createPercentValue(100))
                .setBorder(Border.NO_BORDER);

        Cell redCell = new Cell()
                .setHeight(8)
                .setBackgroundColor(TUNISYS_RED)
                .setBorder(Border.NO_BORDER);
        redBar.addCell(redCell);
        document.add(redBar);

        // Header with logo and company info
        Table headerTable = new Table(UnitValue.createPercentArray(new float[]{25, 75}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginTop(15)
                .setMarginBottom(20);

        // Add logo
        try {
            Image logo = new Image(ImageDataFactory.create(LOGO_PATH))
                    .setWidth(80)
                    .setAutoScaleHeight(true);

            Cell logoCell = new Cell()
                    .add(logo)
                    .setBorder(Border.NO_BORDER)
                    .setVerticalAlignment(VerticalAlignment.MIDDLE);
            headerTable.addCell(logoCell);
        } catch (Exception e) {
            log.warn("Company logo not found at path: {}", LOGO_PATH);
            headerTable.addCell(new Cell().setBorder(Border.NO_BORDER));
        }

        // Company info
        Paragraph companyInfo = new Paragraph()
                .setTextAlignment(TextAlignment.RIGHT)
                .add(new Text(companyName + "\n")
                        .setFontSize(HEADER_FONT_SIZE)
                        .setBold()
                        .setFontColor(TUNISYS_BLACK))
                .add(new Text(companyAddress + "\n")
                        .setFontSize(SMALL_FONT_SIZE)
                        .setFontColor(TUNISYS_BLACK))
                .add(new Text("Tel: " + companyPhone + " | " + companyContact)
                        .setFontSize(SMALL_FONT_SIZE)
                        .setFontColor(TUNISYS_BLACK));

        headerTable.addCell(new Cell()
                .add(companyInfo)
                .setBorder(Border.NO_BORDER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE));

        document.add(headerTable);

        // Separator line
        SolidLine line = new SolidLine(1f);
        line.setColor(TUNISYS_BLACK);
        LineSeparator separator = new LineSeparator(line);
        document.add(separator);

        // Title with red accent
        Paragraph titlePara = new Paragraph()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(20)
                .setMarginBottom(20);

        titlePara.add(new Text(title)
                .setFontSize(TITLE_FONT_SIZE)
                .setBold()
                .setFontColor(TUNISYS_RED));

        document.add(titlePara);
    }

    private void addEmployeeInfo(Document document, String firstName, String lastName, String monthYear) {
        Table infoTable = new Table(UnitValue.createPercentArray(new float[]{30, 70}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginBottom(20)
                .setMarginTop(10);

        infoTable.addCell(createInfoCell("Employee Name:", true));
        infoTable.addCell(createInfoCell(firstName + " " + lastName, false));

        if (monthYear != null) {
            infoTable.addCell(createInfoCell("Period:", true));
            infoTable.addCell(createInfoCell(monthYear, false));
        }

        infoTable.addCell(createInfoCell("Issue Date:", true));
        infoTable.addCell(createInfoCell(getCurrentDate(), false));

        document.add(infoTable);
    }

    private void addPayslipTable(Document document) {
        // Earnings Section
        Table earningsTable = new Table(UnitValue.createPercentArray(new float[]{60, 40}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginTop(15);

        // Header
        earningsTable.addCell(createTableHeader("EARNINGS"));
        earningsTable.addCell(createTableHeader("AMOUNT"));

        // Earnings rows
        earningsTable.addCell(createTableCell("Basic Salary", false));
        earningsTable.addCell(createTableCell("5,000.00 TND", false));

        earningsTable.addCell(createTableCell("Allowances", false));
        earningsTable.addCell(createTableCell("500.00 TND", false));

        earningsTable.addCell(createTableCell("Bonuses", false));
        earningsTable.addCell(createTableCell("300.00 TND", false));

        document.add(earningsTable);

        // Deductions Section
        Table deductionsTable = new Table(UnitValue.createPercentArray(new float[]{60, 40}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginTop(15);

        deductionsTable.addCell(createTableHeader("DEDUCTIONS"));
        deductionsTable.addCell(createTableHeader("AMOUNT"));

        deductionsTable.addCell(createTableCell("Tax", false));
        deductionsTable.addCell(createTableCell("750.00 TND", false));

        deductionsTable.addCell(createTableCell("Social Security", false));
        deductionsTable.addCell(createTableCell("200.00 TND", false));

        deductionsTable.addCell(createTableCell("Insurance", false));
        deductionsTable.addCell(createTableCell("100.00 TND", false));

        document.add(deductionsTable);

        // Total Section with red background
        Table totalTable = new Table(UnitValue.createPercentArray(new float[]{60, 40}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginTop(15);

        Cell totalLabelCell = new Cell()
                .add(new Paragraph("NET SALARY")
                        .setBold()
                        .setFontSize(BODY_FONT_SIZE + 1)
                        .setFontColor(ColorConstants.WHITE))
                .setBackgroundColor(TUNISYS_RED)
                .setPadding(10)
                .setBorder(Border.NO_BORDER);

        Cell totalValueCell = new Cell()
                .add(new Paragraph("4,750.00 TND")
                        .setBold()
                        .setFontSize(BODY_FONT_SIZE + 1)
                        .setFontColor(ColorConstants.WHITE)
                        .setTextAlignment(TextAlignment.RIGHT))
                .setBackgroundColor(TUNISYS_RED)
                .setPadding(10)
                .setBorder(Border.NO_BORDER);

        totalTable.addCell(totalLabelCell);
        totalTable.addCell(totalValueCell);
        document.add(totalTable);
    }

    private void addDateReference(Document document) {
        Paragraph datePara = new Paragraph()
                .setTextAlignment(TextAlignment.RIGHT)
                .setMarginTop(10)
                .setMarginBottom(20)
                .add(new Text("Date: " + getCurrentDate())
                        .setFontSize(BODY_FONT_SIZE)
                        .setFontColor(TUNISYS_BLACK));
        document.add(datePara);
    }

    private void addAttestationContent(Document document, String firstName, String lastName) {
        Paragraph refPara = new Paragraph()
                .setMarginTop(20)
                .setMarginBottom(15)
                .add(new Text("TO WHOM IT MAY CONCERN")
                        .setBold()
                        .setFontSize(BODY_FONT_SIZE + 1)
                        .setFontColor(TUNISYS_RED));
        document.add(refPara);

        Paragraph content = new Paragraph()
                .setFontSize(BODY_FONT_SIZE)
                .setTextAlignment(TextAlignment.JUSTIFIED)
                .setMarginTop(15)
                .setMarginBottom(15)
                .setFirstLineIndent(20);

        content.add("This is to certify that ")
                .add(new Text(firstName + " " + lastName)
                        .setBold()
                        .setFontColor(TUNISYS_BLACK))
                .add(" is currently employed with ")
                .add(new Text(companyName)
                        .setBold()
                        .setFontColor(TUNISYS_RED))
                .add(" as a full-time employee. ");

        content.add("The employee has been working with us and continues to be an active member of our organization. ");

        content.add("This attestation is issued upon the employee's request for official purposes.");

        document.add(content);

        Paragraph purpose = new Paragraph()
                .setFontSize(BODY_FONT_SIZE)
                .setMarginTop(15)
                .add("This certificate is valid and issued without any amendments or alterations.");
        document.add(purpose);
    }

    private void addCertificateContent(Document document, String firstName, String lastName) {
        // Decorative top section
        Paragraph presented = new Paragraph()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(40)
                .setMarginBottom(20)
                .add(new Text("This Certificate is Proudly Presented To")
                        .setFontSize(14)
                        .setFontColor(TUNISYS_BLACK));
        document.add(presented);

        // Employee name with red color
        Paragraph namePara = new Paragraph()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(30)
                .add(new Text(firstName + " " + lastName)
                        .setFontSize(24)
                        .setBold()
                        .setFontColor(TUNISYS_RED));
        document.add(namePara);

        // Recognition text
        Paragraph recognition = new Paragraph()
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(BODY_FONT_SIZE + 1)
                .setMarginBottom(20)
                .add("In Recognition of Dedicated Service and Outstanding Contribution to\n")
                .add(new Text(companyName)
                        .setBold()
                        .setFontColor(TUNISYS_RED));
        document.add(recognition);

        Paragraph details = new Paragraph()
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(BODY_FONT_SIZE)
                .setMarginTop(20)
                .add("We acknowledge and appreciate the exceptional dedication, professionalism,\n")
                .add("and valuable contributions made during their tenure with our organization.");
        document.add(details);
    }

    private void addSignatureSection(Document document) {
        Table sigTable = new Table(UnitValue.createPercentArray(new float[]{50, 50}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginTop(50);

        Cell leftCell = new Cell()
                .setBorder(Border.NO_BORDER)
                .add(new Paragraph("\n\n_____________________\n")
                        .setTextAlignment(TextAlignment.CENTER))
                .add(new Paragraph("Human Resources")
                        .setTextAlignment(TextAlignment.CENTER)
                        .setFontSize(SMALL_FONT_SIZE)
                        .setBold());

        Cell rightCell = new Cell()
                .setBorder(Border.NO_BORDER)
                .add(new Paragraph("\n\n_____________________\n")
                        .setTextAlignment(TextAlignment.CENTER))
                .add(new Paragraph("Authorized Signature")
                        .setTextAlignment(TextAlignment.CENTER)
                        .setFontSize(SMALL_FONT_SIZE)
                        .setBold());

        sigTable.addCell(leftCell);
        sigTable.addCell(rightCell);
        document.add(sigTable);
    }

    private void addCertificateSignature(Document document) {
        Table sigTable = new Table(2)
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginTop(60);

        Cell dateCell = new Cell()
                .setBorder(Border.NO_BORDER)
                .add(new Paragraph("Date: " + getCurrentDate())
                        .setTextAlignment(TextAlignment.LEFT)
                        .setFontSize(BODY_FONT_SIZE));

        Cell signCell = new Cell()
                .setBorder(Border.NO_BORDER)
                .add(new Paragraph("\n_____________________")
                        .setTextAlignment(TextAlignment.CENTER))
                .add(new Paragraph("Authorized Signature")
                        .setTextAlignment(TextAlignment.CENTER)
                        .setFontSize(SMALL_FONT_SIZE)
                        .setBold()
                        .setFontColor(TUNISYS_RED));

        sigTable.addCell(dateCell);
        sigTable.addCell(signCell);
        document.add(sigTable);
    }

    private void addCertificateBorder(Document document) {
        // Red border decoration
        document.add(new Paragraph("\n"));
    }

    private void addProfessionalFooter(Document document) {
        // Black separator line
        SolidLine line = new SolidLine(0.5f);
        line.setColor(TUNISYS_BLACK);
        LineSeparator separator = new LineSeparator(line);
        separator.setMarginTop(30);
        document.add(separator);

        // Footer content
        Paragraph footer = new Paragraph()
                .setFontSize(SMALL_FONT_SIZE)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(10)
                .setFontColor(TUNISYS_BLACK);

        footer.add(new Text("Official Document - " + companyName)
                        .setBold())
                .add("\n")
                .add(companyAddress + " | Tel: " + companyPhone)
                .add("\n")
                .add("Email: " + companyContact);

        document.add(footer);

        // Small red bottom bar
        Table redBar = new Table(1)
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginTop(10)
                .setBorder(Border.NO_BORDER);

        Cell redCell = new Cell()
                .setHeight(5)
                .setBackgroundColor(TUNISYS_RED)
                .setBorder(Border.NO_BORDER);
        redBar.addCell(redCell);
        document.add(redBar);
    }

    private Cell createInfoCell(String text, boolean isBold) {
        Paragraph p = new Paragraph(text)
                .setFontSize(BODY_FONT_SIZE);
        if (isBold) {
            p.setBold().setFontColor(TUNISYS_BLACK);
        }
        return new Cell()
                .add(p)
                .setPadding(8)
                .setBorder(Border.NO_BORDER)
                .setBackgroundColor(isBold ? LIGHT_GRAY : ColorConstants.WHITE);
    }

    private Cell createTableHeader(String text) {
        return new Cell()
                .add(new Paragraph(text)
                        .setBold()
                        .setFontSize(BODY_FONT_SIZE)
                        .setFontColor(ColorConstants.WHITE))
                .setBackgroundColor(TUNISYS_BLACK)
                .setPadding(8)
                .setBorder(Border.NO_BORDER);
    }

    private Cell createTableCell(String text, boolean isTotal) {
        Paragraph p = new Paragraph(text)
                .setFontSize(BODY_FONT_SIZE);

        if (isTotal) {
            p.setBold();
        }

        return new Cell()
                .add(p)
                .setPadding(8)
                .setBorder(new SolidBorder(LIGHT_GRAY, 0.5f))
                .setBackgroundColor(ColorConstants.WHITE);
    }

    private String getCurrentDate() {
        return LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy"));
    }

    private void handlePdfError(String employeeId, String docType, Exception e) {
        log.error("Failed to generate {} for employee: {}", docType, e.getMessage());
    }
}