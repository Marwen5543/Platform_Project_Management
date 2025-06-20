package org.example.documentmanagementservice.Controllers;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.example.documentmanagementservice.DTO.DocumentRequestDto;
import org.example.documentmanagementservice.Model.DocumentRequest;
import org.example.documentmanagementservice.Services.DocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/documents")
public class DocumentController {
    @Autowired
    private DocumentService documentService;

    @PostMapping("/request")
    @PreAuthorize("isAuthenticated() and hasAnyRole('EMPLOYEE', 'MANAGER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> requestDocument(@Valid @RequestBody DocumentRequestDto dto) {
        try {
            DocumentRequest request = documentService.requestDocument(dto);
            return ResponseEntity.ok(request);
        } catch (AuthenticationCredentialsNotFoundException e) {
            return ResponseEntity.status(401).body("Authentication required");
        } catch (Exception e) {
            log.error("Document request failed", e);
            return ResponseEntity.internalServerError().body("Request processing failed");
        }
    }

    @GetMapping("/history")
    @PreAuthorize("isAuthenticated() and hasAnyRole('EMPLOYEE', 'MANAGER', 'ADMIN', 'HR')")
    public ResponseEntity<List<DocumentRequest>> getDocumentHistory() {
        return ResponseEntity.ok(documentService.getDocumentHistory());
    }

    @GetMapping("/download/{id}")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'ADMIN', 'HR')")
    public ResponseEntity<byte[]> downloadDocument(@PathVariable UUID id) {
        byte[] pdf = documentService.downloadDocument(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=document-" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @PostMapping("/approve/{id}")
    @PreAuthorize("hasRole('HR')")
    public ResponseEntity<?> approveDocument(@PathVariable UUID id) {
        try {
            DocumentRequest request = documentService.approveDocument(id);
            return ResponseEntity.ok(request);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error approving document: " + e.getMessage());
        }
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('HR')")
    public ResponseEntity<List<DocumentRequest>> getPendingDocumentRequests() {
        try {
            List<DocumentRequest> requests = documentService.getPendingDocumentRequests();
            return ResponseEntity.ok(requests);
        } catch (Exception e) {
            // Log the error for debugging
            System.err.println("Error fetching pending document requests: " + e.getMessage());
            // Return an empty list with 500 status to maintain type consistency
            return ResponseEntity.status(500).body(new ArrayList<>());
        }
    }
}