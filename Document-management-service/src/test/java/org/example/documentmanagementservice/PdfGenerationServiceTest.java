package org.example.documentmanagementservice;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import org.example.documentmanagementservice.Services.PdfGenerationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class PdfGenerationServiceTest {

    @InjectMocks
    private PdfGenerationService pdfGenerationService;

    @Mock
    private ClassPathResource classPathResource;

    @BeforeEach
    void setUp() throws IOException, NoSuchFieldException, IllegalAccessException {
        // Mock the ClassPathResource to simulate logo loading failure
        lenient().when(classPathResource.getURL()).thenThrow(new IOException("Logo not found"));

        // Set @Value fields using reflection to avoid null values
        setField("companyName", "Tunisys");
        setField("companyAddress", "Avenue de la Liberté, Tunis, TN");
        setField("companyContact", "Tunisys@gmail.com.tn");
    }

    private void setField(String fieldName, String value) throws NoSuchFieldException, IllegalAccessException {
        Field field = PdfGenerationService.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(pdfGenerationService, value);
    }

    @Test
    void testGeneratePayslip_Success() throws IOException {
        // Act
        byte[] pdf = pdfGenerationService.generatePayslip("user123", "2023-01", "John", "Doe");

        // Assert
        assertNotNull(pdf);
        assertTrue(pdf.length > 0);

        try (PdfReader reader = new PdfReader(new ByteArrayInputStream(pdf));
             PdfDocument pdfDoc = new PdfDocument(reader)) {
            assertEquals(1, pdfDoc.getNumberOfPages());
            String content = PdfTextExtractor.getTextFromPage(pdfDoc.getPage(1)).toLowerCase();
            assertTrue(content.contains("payslip"));
            assertTrue(content.contains("john doe"));
            assertTrue(content.contains("user123"));
            assertTrue(content.contains("2023-01"));
            assertTrue(content.contains("tunisys"));
            assertTrue(content.contains("total net salary"));
        }
    }

    @Test
    void testGenerateWorkAttestation_Success() throws IOException {
        // Act
        byte[] pdf = pdfGenerationService.generateWorkAttestation("user123", "John", "Doe");

        // Assert
        assertNotNull(pdf);
        assertTrue(pdf.length > 0);

        try (PdfReader reader = new PdfReader(new ByteArrayInputStream(pdf));
             PdfDocument pdfDoc = new PdfDocument(reader)) {
            assertEquals(1, pdfDoc.getNumberOfPages());
            String content = PdfTextExtractor.getTextFromPage(pdfDoc.getPage(1)).toLowerCase();
            assertTrue(content.contains("work attestation"));
            assertTrue(content.contains("john doe"));
            assertTrue(content.contains("user123"));
            assertTrue(content.contains("tunisys"));
        }
    }

    @Test
    void testGenerateCertificate_Success() throws IOException {
        // Act
        byte[] pdf = pdfGenerationService.generateCertificate("user123", "John", "Doe");

        // Assert
        assertNotNull(pdf);
        assertTrue(pdf.length > 0);

        try (PdfReader reader = new PdfReader(new ByteArrayInputStream(pdf));
             PdfDocument pdfDoc = new PdfDocument(reader)) {
            assertEquals(1, pdfDoc.getNumberOfPages());
            String content = PdfTextExtractor.getTextFromPage(pdfDoc.getPage(1)).toLowerCase();
            assertTrue(content.contains("certificate of employment"));
            assertTrue(content.contains("john doe"));
            assertTrue(content.contains("tunisys"));
        }
    }

    @Test
    void testGeneratePayslip_NullEmployeeId() {
        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            pdfGenerationService.generatePayslip(null, "2023-01", "John", "Doe");
        });
        assertEquals("PDF generation failed", exception.getMessage());
        assertInstanceOf(IllegalArgumentException.class, exception.getCause());
    }

    @Test
    void testGenerateWorkAttestation_NullFirstName() throws IOException {
        // Act
        byte[] pdf = pdfGenerationService.generateWorkAttestation("user123", null, "Doe");

        // Assert
        assertNotNull(pdf);
        assertTrue(pdf.length > 0);

        try (PdfReader reader = new PdfReader(new ByteArrayInputStream(pdf));
             PdfDocument pdfDoc = new PdfDocument(reader)) {
            assertEquals(1, pdfDoc.getNumberOfPages());
            String content = PdfTextExtractor.getTextFromPage(pdfDoc.getPage(1)).toLowerCase();
            assertTrue(content.contains("work attestation"));
            assertTrue(content.contains("null doe")); // Reflects null firstName concatenated with lastName
            assertTrue(content.contains("user123"));
            assertTrue(content.contains("tunisys"));
        }
    }
}