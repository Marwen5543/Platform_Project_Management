package org.example.documentmanagementservice.Services;

import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
@Slf4j
public class PdfGenerationService {

    @Value("${company.name:Tunisys}")
    private String companyName;

    @Value("${company.address:Avenue de la Liberté , Tunis, TN}")
    private String companyAddress;

    @Value("${company.contact:Tunisys@gmail.com.tn}")
    private String companyContact;

    private static final String LOGO_PATH = "D:/Frontend/Management/Managment/src/assets/tunisys.png";
    private static final float HEADER_FONT_SIZE = 16;
    private static final float BODY_FONT_SIZE = 12;

    public byte[] generatePayslip(String employeeId, String monthYear, String firstName, String lastName) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = initDocument(pdf);

            // Header
            addHeader(document, "PAYSLIP");

            // Employee Information
            addEmployeeInfo(document, employeeId, firstName, lastName, monthYear);

            // Pay Details Table
            Table table = new Table(UnitValue.createPercentArray(new float[]{30, 70}))
                    .setWidth(UnitValue.createPercentValue(100))
                    .setMarginTop(20);

            addTableRow(table, "Earnings", "");
            addTableRow(table, "Basic Salary", "$5,000.00");
            addTableRow(table, "Allowances", "$500.00");
            addTableRow(table, "Deductions", "");
            addTableRow(table, "Tax", "$750.00");
            addTableRow(table, "Insurance", "$200.00");
            addTableRow(table, "Total Net Salary", "$4,550.00", true);

            document.add(table);

            // Footer
            addFooter(document);

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

            addHeader(document, "WORK ATTESTATION");

            addEmployeeInfo(document, employeeId, firstName, lastName, null);

            Paragraph content = new Paragraph()
                    .setFontSize(BODY_FONT_SIZE)
                    .add("This is to certify that ")
                    .add(new Text(firstName + " " + lastName).setBold())
                    .add(", bearing employee ID ")
                    .add(new Text(employeeId).setBold())
                    .add(", is currently employed at ")
                    .add(new Text(companyName).setBold())
                    .add(" as a full-time employee since [START DATE].")
                    .setMarginTop(20);

            document.add(content);
            addFooter(document);
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

            addHeader(document, "CERTIFICATE OF EMPLOYMENT");

            Paragraph certificateText = new Paragraph()
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(14)
                    .setBold()
                    .add("This Certificate is Proudly Presented To")
                    .setMarginTop(30);

            Paragraph nameText = new Paragraph()
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(18)
                    .setBold()
                    .add(firstName + " " + lastName)
                    .setMarginTop(10);

            Paragraph details = new Paragraph()
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(BODY_FONT_SIZE)
                    .add("In recognition of their dedicated service at " + companyName)
                    .setMarginTop(20);

            document.add(certificateText);
            document.add(nameText);
            document.add(details);
            addFooter(document);
            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            handlePdfError(employeeId, "certificate", e);
            throw new RuntimeException("PDF generation failed", e);
        }
    }

    private Document initDocument(PdfDocument pdf) {
        Document document = new Document(pdf, PageSize.A4);
        document.setMargins(50, 30, 50, 30);
        return document;
    }

    private void addHeader(Document document, String title) throws IOException {
        // Create a table with 2 columns for logo and company info
        Table headerTable = new Table(UnitValue.createPercentArray(new float[]{20, 80}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginBottom(20);

        // Add logo
        try {
            Image logo = new Image(ImageDataFactory.create(new ClassPathResource(LOGO_PATH).getURL()))
                    .setWidth(100)
                    .setAutoScaleHeight(true);
            headerTable.addCell(createCell(logo).setBorder(Border.NO_BORDER));
        } catch (IOException e) {
            log.warn("Company logo not found at path: {}", LOGO_PATH);
        }

        // Add company info
        Paragraph companyInfo = new Paragraph()
                .setTextAlignment(TextAlignment.RIGHT)
                .add(new Text(companyName + "\n").setFontSize(HEADER_FONT_SIZE).setBold())
                .add(new Text(companyAddress + "\n"))
                .add(new Text(companyContact));

        headerTable.addCell(createCell(companyInfo).setBorder(Border.NO_BORDER));
        document.add(headerTable);

        // Add title
        document.add(new Paragraph(title)
                .setFontSize(20)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(10)
                .setMarginBottom(20));
    }

    private void addEmployeeInfo(Document document, String employeeId,
                                 String firstName, String lastName, String monthYear) {
        Table infoTable = new Table(UnitValue.createPercentArray(2))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginBottom(15);

        // Updated to use createTextCell instead of createCell
        infoTable.addCell(createTextCell("Employee Name:", true));
        infoTable.addCell(createTextCell(firstName + " " + lastName, false));
        infoTable.addCell(createTextCell("Employee ID:", true));
        infoTable.addCell(createTextCell(employeeId, false));

        if (monthYear != null) {
            infoTable.addCell(createTextCell("Period:", true));
            infoTable.addCell(createTextCell(monthYear, false));
        }

        document.add(infoTable);
    }
    private Cell createTextCell(String text, boolean isBold) {
        Paragraph p = new Paragraph(text);
        if (isBold) {
            p.setBold();
        }
        return new Cell()
                .add(p)
                .setPadding(5)
                .setBorder(Border.NO_BORDER);
    }
    private void addFooter(Document document) throws IOException {
        Paragraph footer = new Paragraph()
                .setFontSize(10)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(30)
                .add("Official Document - " + companyName + "\n")
                .add("For any inquiries, please contact: " + companyContact);

        document.add(footer);
    }

    // Change from BlockElement to IElement
    private Cell createCell(Image image) {
        return new Cell()
                .add(new Paragraph().add(image))
                .setPadding(5)
                .setBorder(Border.NO_BORDER);
    }

    private Cell createCell(Paragraph paragraph) {
        return new Cell()
                .add(paragraph)
                .setPadding(5)
                .setBorder(Border.NO_BORDER);
    }


    private void addTableRow(Table table, String label, String value) {
        addTableRow(table, label, value, false);
    }

    private void addTableRow(Table table, String label, String value, boolean isTotal) {
        Paragraph labelPara = new Paragraph(label)
                .setBold()
                .setFontSize(BODY_FONT_SIZE);

        Paragraph valuePara = new Paragraph(value)
                .setFontSize(BODY_FONT_SIZE);

        if (isTotal) {
            labelPara.setBold().setFontColor(ColorConstants.DARK_GRAY);
            valuePara.setBold().setFontColor(ColorConstants.DARK_GRAY);
        }

        table.addCell(new Cell().add(labelPara).setBackgroundColor(isTotal ? ColorConstants.LIGHT_GRAY : null));
        table.addCell(new Cell().add(valuePara).setBackgroundColor(isTotal ? ColorConstants.LIGHT_GRAY : null));
    }

    private void handlePdfError(String employeeId, String docType, Exception e) {
        log.error("Failed to generate {} for employee {}: {}", docType, employeeId, e.getMessage());
    }
}