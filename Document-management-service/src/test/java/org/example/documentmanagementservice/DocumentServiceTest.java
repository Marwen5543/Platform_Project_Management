package org.example.documentmanagementservice;

import org.example.documentmanagementservice.Services.DocumentService;
import org.example.documentmanagementservice.Services.PdfGenerationService;
import org.example.documentmanagementservice.Services.UserClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.example.documentmanagementservice.DTO.DocumentRequestDto;
import org.example.documentmanagementservice.DTO.UserDTO;
import org.example.documentmanagementservice.Execeptions.DocumentNotFoundException;
import org.example.documentmanagementservice.Model.DocumentRequest;
import org.example.documentmanagementservice.Model.DocumentStatus;
import org.example.documentmanagementservice.Model.DocumentType;
import org.example.documentmanagementservice.Repository.DocumentRequestRepository;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DocumentRequestRepository repository;

    @Mock
    private PdfGenerationService pdfService;

    @Mock
    private UserClient userClient;

    @InjectMocks
    private DocumentService documentService;

    private Authentication auth;

    @BeforeEach
    void setUp() {
        // Set up JWT-based authentication with lenient stubbing
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", "user123")
                .build();
        auth = mock(Authentication.class);
        lenient().when(auth.getPrincipal()).thenReturn(jwt);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void testRequestDocument_Success() {
        // Arrange
        DocumentRequestDto dto = new DocumentRequestDto();
        dto.setDocumentType(DocumentType.PAYSLIP);
        dto.setMonthYear("2023-01");

        UserDTO user = new UserDTO("user123", "John", "Doe", "john.doe");
        when(userClient.getUserById("user123")).thenReturn(user);

        DocumentRequest savedRequest = new DocumentRequest();
        savedRequest.setId(UUID.randomUUID());
        savedRequest.setEmployeeId("user123");
        savedRequest.setDocumentType(DocumentType.PAYSLIP);
        savedRequest.setMonthYear("2023-01");
        savedRequest.setStatus(DocumentStatus.PENDING_APPROVAL);
        when(repository.save(any(DocumentRequest.class))).thenReturn(savedRequest);

        // Act
        DocumentRequest result = documentService.requestDocument(dto);

        // Assert
        assertNotNull(result);
        assertEquals("user123", result.getEmployeeId());
        assertEquals(DocumentType.PAYSLIP, result.getDocumentType());
        assertEquals(DocumentStatus.PENDING_APPROVAL, result.getStatus());
        verify(repository, times(1)).save(any(DocumentRequest.class));
    }

    @Test
    void testRequestDocument_Unauthenticated() {
        // Arrange
        SecurityContextHolder.clearContext(); // Simulate unauthenticated user
        DocumentRequestDto dto = new DocumentRequestDto();

        // Act & Assert
        assertThrows(org.springframework.security.authentication.AuthenticationCredentialsNotFoundException.class,
                () -> documentService.requestDocument(dto));
    }

    @Test
    void testRequestDocument_UserClientFallback() {
        // Arrange
        DocumentRequestDto dto = new DocumentRequestDto();
        dto.setDocumentType(DocumentType.CERTIFICATE);

        // Simulate fallback behavior
        UserDTO fallbackUser = new UserDTO("user123", "Unknown", "User", "");
        when(userClient.getUserById("user123")).thenReturn(fallbackUser);

        DocumentRequest savedRequest = new DocumentRequest();
        savedRequest.setId(UUID.randomUUID());
        savedRequest.setEmployeeId("user123");
        savedRequest.setDocumentType(DocumentType.CERTIFICATE);
        savedRequest.setStatus(DocumentStatus.PENDING_APPROVAL);
        when(repository.save(any(DocumentRequest.class))).thenReturn(savedRequest);

        // Act
        DocumentRequest result = documentService.requestDocument(dto);

        // Assert
        assertNotNull(result);
        assertEquals("user123", result.getEmployeeId());
        assertEquals(DocumentType.CERTIFICATE, result.getDocumentType());
        assertEquals(DocumentStatus.PENDING_APPROVAL, result.getStatus());
        verify(repository, times(1)).save(any(DocumentRequest.class));
        verify(userClient, times(1)).getUserById("user123");
    }

    @Test
    void testApproveDocument_Success() {
        // Arrange
        UUID id = UUID.randomUUID();
        DocumentRequest request = new DocumentRequest();
        request.setId(id);
        request.setEmployeeId("user123");
        request.setDocumentType(DocumentType.PAYSLIP);
        request.setMonthYear("2023-01");
        request.setStatus(DocumentStatus.PENDING_APPROVAL);
        when(repository.findById(id)).thenReturn(Optional.of(request));

        // Set HR role for authorization

        UserDTO user = new UserDTO("user123", "John", "Doe", "john.doe");
        when(userClient.getUserById("user123")).thenReturn(user);

        byte[] pdf = "PDF content".getBytes();
        when(pdfService.generatePayslip("user123", "2023-01", "John", "Doe")).thenReturn(pdf);

        DocumentRequest savedRequest = new DocumentRequest();
        savedRequest.setId(id);
        savedRequest.setStatus(DocumentStatus.APPROVED);
        savedRequest.setFilePath("/app/documents/" + id + ".pdf");
        when(repository.save(any(DocumentRequest.class))).thenReturn(savedRequest);

        // Act
        DocumentRequest result = documentService.approveDocument(id);

        // Assert
        assertEquals(DocumentStatus.APPROVED, result.getStatus());
        assertEquals("/app/documents/" + id + ".pdf", result.getFilePath());
        verify(repository, times(1)).save(any(DocumentRequest.class));
    }


    @Test
    void testGetDocumentHistory_Success() {
        // Arrange
        DocumentRequest request1 = new DocumentRequest();
        request1.setId(UUID.randomUUID());
        request1.setEmployeeId("user123");
        request1.setDocumentType(DocumentType.PAYSLIP);
        request1.setStatus(DocumentStatus.APPROVED);

        DocumentRequest request2 = new DocumentRequest();
        request2.setId(UUID.randomUUID());
        request2.setEmployeeId("user123");
        request2.setDocumentType(DocumentType.CERTIFICATE);
        request2.setStatus(DocumentStatus.PENDING_APPROVAL);

        when(repository.findByEmployeeId("user123")).thenReturn(List.of(request1, request2));

        // Act
        List<DocumentRequest> result = documentService.getDocumentHistory();

        // Assert
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(r -> r.getDocumentType() == DocumentType.PAYSLIP));
        assertTrue(result.stream().anyMatch(r -> r.getDocumentType() == DocumentType.CERTIFICATE));
        verify(repository, times(1)).findByEmployeeId("user123");
    }


    @Test
    void testApproveDocument_PdfGenerationFailure() {
        // Arrange
        UUID id = UUID.randomUUID();
        DocumentRequest request = new DocumentRequest();
        request.setId(id);
        request.setEmployeeId("user123");
        request.setDocumentType(DocumentType.PAYSLIP);
        request.setMonthYear("2023-01");
        request.setStatus(DocumentStatus.PENDING_APPROVAL);
        when(repository.findById(id)).thenReturn(Optional.of(request));

        // Set HR role for authorization

        UserDTO user = new UserDTO("user123", "John", "Doe", "john.doe");
        when(userClient.getUserById("user123")).thenReturn(user);

        when(pdfService.generatePayslip("user123", "2023-01", "John", "Doe"))
                .thenThrow(new RuntimeException("PDF generation failed"));

        DocumentRequest savedRequest = new DocumentRequest();
        savedRequest.setId(id);
        savedRequest.setStatus(DocumentStatus.FAILED);
        when(repository.save(any(DocumentRequest.class))).thenReturn(savedRequest);

        // Act
        DocumentRequest result = documentService.approveDocument(id);

        // Assert
        assertEquals(DocumentStatus.FAILED, result.getStatus());
        verify(repository, times(1)).save(any(DocumentRequest.class));
    }

}