package org.example.documentmanagementservice;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import org.example.documentmanagementservice.Services.PdfGenerationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class PdfGenerationServiceTest {

    @InjectMocks
    private PdfGenerationService pdfGenerationService;

    @BeforeEach
    void setUp() throws NoSuchFieldException, IllegalAccessException {
        // Set @Value fields using reflection to avoid null values
        setField("companyName", "Tunisys");
        setField("companyAddress", "Avenue de la Liberté, Tunis, TN");
        setField("companyContact", "contact@tunisys.com.tn");
        setField("companyPhone", "+216 71 XXX XXX");
    }

    private void setField(String fieldName, String value) throws NoSuchFieldException, IllegalAccessException {
        Field field = PdfGenerationService.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(pdfGenerationService, value);
    }

    @Test
    void testGeneratePayslip_Success() throws IOException {
        // Act
        byte[] pdf = pdfGenerationService.generatePayslip("EMP123", "January 2024", "John", "Doe");

        // Assert
        assertNotNull(pdf);
        assertTrue(pdf.length > 0);

        try (PdfReader reader = new PdfReader(new ByteArrayInputStream(pdf));
             PdfDocument pdfDoc = new PdfDocument(reader)) {
            assertEquals(1, pdfDoc.getNumberOfPages());
            String content = PdfTextExtractor.getTextFromPage(pdfDoc.getPage(1)).toLowerCase();

            // Check for key content
            assertTrue(content.contains("payslip") || content.contains("monthly payslip"));
            assertTrue(content.contains("john") && content.contains("doe"));
            assertTrue(content.contains("january 2024"));
            assertTrue(content.contains("tunisys"));
            assertTrue(content.contains("net salary"));
            assertTrue(content.contains("earnings"));
            assertTrue(content.contains("deductions"));
        }
    }

    @Test
    void testGenerateWorkAttestation_Success() throws IOException {
        // Act
        byte[] pdf = pdfGenerationService.generateWorkAttestation("EMP123", "John", "Doe");

        // Assert
        assertNotNull(pdf);
        assertTrue(pdf.length > 0);

        try (PdfReader reader = new PdfReader(new ByteArrayInputStream(pdf));
             PdfDocument pdfDoc = new PdfDocument(reader)) {
            assertEquals(1, pdfDoc.getNumberOfPages());
            String content = PdfTextExtractor.getTextFromPage(pdfDoc.getPage(1)).toLowerCase();

            // Check for key content
            assertTrue(content.contains("attestation") || content.contains("employment attestation"));
            assertTrue(content.contains("john") && content.contains("doe"));
            assertTrue(content.contains("tunisys"));
            assertTrue(content.contains("to whom it may concern"));
        }
    }

    @Test
    void testGenerateCertificate_Success() throws IOException {
        // Act
        byte[] pdf = pdfGenerationService.generateCertificate("EMP123", "John", "Doe");

        // Assert
        assertNotNull(pdf);
        assertTrue(pdf.length > 0);

        try (PdfReader reader = new PdfReader(new ByteArrayInputStream(pdf));
             PdfDocument pdfDoc = new PdfDocument(reader)) {
            assertEquals(1, pdfDoc.getNumberOfPages());
            String content = PdfTextExtractor.getTextFromPage(pdfDoc.getPage(1)).toLowerCase();

            // Check for key content
            assertTrue(content.contains("certificate"));
            assertTrue(content.contains("john") && content.contains("doe"));
            assertTrue(content.contains("tunisys"));
            assertTrue(content.contains("recognition") || content.contains("dedicated service"));
        }
    }

    @Test
    void testGeneratePayslip_WithDifferentNames() throws IOException {
        // Act
        byte[] pdf = pdfGenerationService.generatePayslip("EMP456", "February 2024", "Jane", "Smith");

        // Assert
        assertNotNull(pdf);
        assertTrue(pdf.length > 0);

        try (PdfReader reader = new PdfReader(new ByteArrayInputStream(pdf));
             PdfDocument pdfDoc = new PdfDocument(reader)) {
            String content = PdfTextExtractor.getTextFromPage(pdfDoc.getPage(1)).toLowerCase();
            assertTrue(content.contains("jane") && content.contains("smith"));
            assertTrue(content.contains("february 2024"));
        }
    }

    @Test
    void testGenerateWorkAttestation_WithDifferentNames() throws IOException {
        // Act
        byte[] pdf = pdfGenerationService.generateWorkAttestation("EMP456", "Jane", "Smith");

        // Assert
        assertNotNull(pdf);
        assertTrue(pdf.length > 0);

        try (PdfReader reader = new PdfReader(new ByteArrayInputStream(pdf));
             PdfDocument pdfDoc = new PdfDocument(reader)) {
            String content = PdfTextExtractor.getTextFromPage(pdfDoc.getPage(1)).toLowerCase();
            assertTrue(content.contains("jane") && content.contains("smith"));
        }
    }

    @Test
    void testGenerateCertificate_WithDifferentNames() throws IOException {
        // Act
        byte[] pdf = pdfGenerationService.generateCertificate("EMP456", "Jane", "Smith");

        // Assert
        assertNotNull(pdf);
        assertTrue(pdf.length > 0);

        try (PdfReader reader = new PdfReader(new ByteArrayInputStream(pdf));
             PdfDocument pdfDoc = new PdfDocument(reader)) {
            String content = PdfTextExtractor.getTextFromPage(pdfDoc.getPage(1)).toLowerCase();
            assertTrue(content.contains("jane") && content.contains("smith"));
        }
    }

    @Test
    void testGeneratePayslip_ContainsCompanyInfo() throws IOException {
        // Act
        byte[] pdf = pdfGenerationService.generatePayslip("EMP123", "March 2024", "John", "Doe");

        // Assert
        assertNotNull(pdf);

        try (PdfReader reader = new PdfReader(new ByteArrayInputStream(pdf));
             PdfDocument pdfDoc = new PdfDocument(reader)) {
            String content = PdfTextExtractor.getTextFromPage(pdfDoc.getPage(1)).toLowerCase();
            assertTrue(content.contains("tunisys"));
            assertTrue(content.contains("tunis"));
        }
    }

    @Test
    void testGenerateWorkAttestation_ContainsDate() throws IOException {
        // Act
        byte[] pdf = pdfGenerationService.generateWorkAttestation("EMP123", "John", "Doe");

        // Assert
        assertNotNull(pdf);

        try (PdfReader reader = new PdfReader(new ByteArrayInputStream(pdf));
             PdfDocument pdfDoc = new PdfDocument(reader)) {
            String content = PdfTextExtractor.getTextFromPage(pdfDoc.getPage(1)).toLowerCase();
            // Should contain some date (current date)
            assertTrue(content.contains("202")); // Year check
        }
    }

    @Test
    void testGenerateCertificate_ContainsSignature() throws IOException {
        // Act
        byte[] pdf = pdfGenerationService.generateCertificate("EMP123", "John", "Doe");

        // Assert
        assertNotNull(pdf);

        try (PdfReader reader = new PdfReader(new ByteArrayInputStream(pdf));
             PdfDocument pdfDoc = new PdfDocument(reader)) {
            String content = PdfTextExtractor.getTextFromPage(pdfDoc.getPage(1)).toLowerCase();
            assertTrue(content.contains("signature") || content.contains("authorized"));
        }
    }
}