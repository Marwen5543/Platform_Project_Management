package org.example.documentmanagementservice.Services;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.example.documentmanagementservice.DTO.DocumentRequestDto;
import org.example.documentmanagementservice.DTO.UserDTO;
import org.example.documentmanagementservice.Execeptions.DocumentNotFoundException;
import org.example.documentmanagementservice.Execeptions.DocumentStorageException;
import org.example.documentmanagementservice.Model.DocumentRequest;
import org.example.documentmanagementservice.Model.DocumentStatus;
import org.example.documentmanagementservice.Model.DocumentType;
import org.example.documentmanagementservice.Repository.DocumentRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class DocumentService {
    @Autowired
    private DocumentRequestRepository repository;

    @Autowired
    private PdfGenerationService pdfService;

    @Autowired
    private UserClient userClient;

    @Value("${document.storage.path:/app/documents}")
    private String storagePath;

    public DocumentRequest requestDocument(DocumentRequestDto dto) {
        String userId = getCurrentUserId();
        log.info("Creating document request for user: {}, type: {}", userId, dto.getDocumentType());

        UserDTO user = getUserWithFallback(userId);

        DocumentRequest request = new DocumentRequest();
        request.setEmployeeId(userId);
        request.setDocumentType(dto.getDocumentType());
        request.setMonthYear(dto.getMonthYear());
        request.setStatus(DocumentStatus.PENDING_APPROVAL);
        request.setCreatedAt(LocalDateTime.now());
        request = repository.save(request);

        notifyHr(request);

        return request;
    }

    @PreAuthorize("hasRole('HR')")
    public DocumentRequest approveDocument(UUID id) {
        log.info("Approving document {}", id);
        DocumentRequest request = repository.findById(id)
                .orElseThrow(() -> new DocumentNotFoundException("Document ID " + id + " not found"));

        if (request.getStatus() != DocumentStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("Document is not in PENDING_APPROVAL status");
        }

        try {
            UserDTO user = getUserWithFallback(request.getEmployeeId());
            byte[] pdf = switch (request.getDocumentType()) {
                case PAYSLIP -> pdfService.generatePayslip(
                        request.getEmployeeId(), request.getMonthYear(), user.getFirstName(), user.getLastName());
                case WORK_ATTESTATION -> pdfService.generateWorkAttestation(
                        request.getEmployeeId(), user.getFirstName(), user.getLastName());
                case CERTIFICATE -> pdfService.generateCertificate(
                        request.getEmployeeId(), user.getFirstName(), user.getLastName());
            };
            String filePath = savePdfToStorage(pdf, request.getId());
            request.setFilePath(filePath);
            request.setStatus(DocumentStatus.APPROVED);
        } catch (Exception e) {
            log.error("Failed to generate document for request {}: {}", id, e.getMessage());
            request.setStatus(DocumentStatus.FAILED);
        }
        return repository.save(request);
    }

    public List<DocumentRequest> getDocumentHistory() {
        String userId = getCurrentUserId();
        log.info("Fetching document history for user: {}", userId);
        return repository.findByEmployeeId(userId);
    }

    public byte[] downloadDocument(UUID id) {
        String userId = getCurrentUserId();
        log.info("Downloading document {} for user: {}", id, userId);

        DocumentRequest request = repository.findById(id)
                .orElseThrow(() -> new DocumentNotFoundException("Document ID " + id + " not found"));

        if (!request.getEmployeeId().equals(userId) && !SecurityContextHolder.getContext()
                .getAuthentication().getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_HR"))) {
            throw new SecurityException("Unauthorized access to document");
        }

        try {
            return Files.readAllBytes(Paths.get(request.getFilePath()));
        } catch (IOException e) {
            log.error("Failed to download document {}: {}", id, e.getMessage());
            throw new DocumentNotFoundException("Failed to access document file: " + id);
        }
    }

    private String savePdfToStorage(byte[] pdf, UUID id) {
        String filePath = storagePath + "/" + id + ".pdf";
        try {
            Files.createDirectories(Paths.get(storagePath));
            Files.write(Paths.get(filePath), pdf);
            return filePath;
        } catch (IOException e) {
            log.error("Failed to save PDF for document {}: {}", id, e.getMessage());
            throw new DocumentStorageException("Failed to save document: " + id);
        }
    }

    private String getCurrentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        log.debug("Authentication object: {}", auth);
        if (auth == null || !(auth.getPrincipal() instanceof Jwt jwt)) {
            log.error("Authentication is null or principal is not a JWT");
            throw new AuthenticationCredentialsNotFoundException("User not authenticated");
        }
        log.debug("JWT subject: {}", jwt.getSubject());
        return jwt.getSubject();
    }

    @CircuitBreaker(name = "userService", fallbackMethod = "getUserFallback")
    private UserDTO getUserWithFallback(String userId) {
        log.info("Fetching user details for userId: {}", userId);
        try {
            return userClient.getUserById(userId);
        } catch (Exception e) {
            log.error("Error calling user service for userId: {}, error: {}", userId, e.getMessage());
            throw e; // Re-throw to trigger circuit breaker
        }
    }

    private UserDTO getUserFallback(String userId, Exception ex) {
        log.warn("Circuit breaker fallback triggered for user {}: {}", userId, ex.getMessage());
        return new UserDTO(userId, "Unknown", "User", "unknown_user_" + userId);
    }

    private void notifyHr(DocumentRequest request) {
        log.info("Notifying HR about new document request: {}", request.getId());
    }

    // Primary method - now with working error handling
    @PreAuthorize("hasRole('HR')")
    public List<DocumentRequest> getPendingDocumentRequests() {
        log.info("Fetching pending document requests for HR");
        List<DocumentRequest> requests = repository.findByStatus(DocumentStatus.PENDING_APPROVAL);

        for (DocumentRequest request : requests) {
            String username = getUsernameWithFallback(request.getEmployeeId());
            request.setUsername(username);
        }

        return requests;
    }

    private String getUsernameWithFallback(String employeeId) {
        try {
            UserDTO user = getUserWithFallback(employeeId);
            if (user != null) {
                if (user.getUsername() != null && !user.getUsername().isEmpty()) {
                    return user.getUsername();
                } else if (user.getFirstName() != null || user.getLastName() != null) {
                    String firstName = user.getFirstName() != null ? user.getFirstName() : "";
                    String lastName = user.getLastName() != null ? user.getLastName() : "";
                    String fullName = (firstName + " " + lastName).trim();
                    return fullName.isEmpty() ? "Unknown User" : fullName;
                }
            }
        } catch (Exception e) {
            log.error("Failed to fetch user details for employeeId: {}, using fallback", employeeId, e);
        }
        return "Unknown User";
    }

    // Alternative approach: Direct call with manual fallback (bypasses circuit breaker issues)
    @PreAuthorize("hasRole('HR')")
    public List<DocumentRequest> getPendingDocumentRequestsSimple() {
        log.info("Fetching pending document requests for HR (simple version)");
        List<DocumentRequest> requests = repository.findByStatus(DocumentStatus.PENDING_APPROVAL);

        for (DocumentRequest request : requests) {
            String username = getUsernameDirectly(request.getEmployeeId());
            request.setUsername(username);
        }

        return requests;
    }

    private String getUsernameDirectly(String employeeId) {
        try {
            log.debug("Fetching user details directly for userId: {}", employeeId);
            UserDTO user = userClient.getUserById(employeeId);

            if (user != null) {
                if (user.getUsername() != null && !user.getUsername().isEmpty()) {
                    return user.getUsername();
                } else if (user.getFirstName() != null || user.getLastName() != null) {
                    String firstName = user.getFirstName() != null ? user.getFirstName() : "";
                    String lastName = user.getLastName() != null ? user.getLastName() : "";
                    String fullName = (firstName + " " + lastName).trim();
                    return fullName.isEmpty() ? "Unknown User" : fullName;
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch user details for employeeId: {}, using fallback: {}", employeeId, e.getMessage());
        }
        return "Unknown User";
    }
}