package org.example.documentmanagementservice;

import org.example.documentmanagementservice.Controllers.DocumentController;
import org.example.documentmanagementservice.DTO.DocumentRequestDto;
import org.example.documentmanagementservice.Model.DocumentRequest;
import org.example.documentmanagementservice.Model.DocumentStatus;
import org.example.documentmanagementservice.Model.DocumentType;
import org.example.documentmanagementservice.Services.DocumentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;


@WebMvcTest(DocumentController.class)
class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DocumentService documentService;



    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void testRequestDocument_Success() throws Exception {
        DocumentRequest request = new DocumentRequest();
        request.setId(UUID.randomUUID());
        request.setDocumentType(DocumentType.PAYSLIP);
        request.setStatus(DocumentStatus.PENDING_APPROVAL);
        when(documentService.requestDocument(any(DocumentRequestDto.class))).thenReturn(request);

        mockMvc.perform(post("/api/documents/request")
                        .with(csrf()) // ✅ ajouté
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"documentType\":\"PAYSLIP\",\"monthYear\":\"2023-01\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.documentType").value("PAYSLIP"));
    }


    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void testGetDocumentHistory_Success() throws Exception {
        List<DocumentRequest> requests = List.of(new DocumentRequest(), new DocumentRequest());
        when(documentService.getDocumentHistory()).thenReturn(requests);

        mockMvc.perform(get("/api/documents/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void testDownloadDocument_Success() throws Exception {
        UUID id = UUID.randomUUID();
        byte[] pdf = "PDF content".getBytes();
        when(documentService.downloadDocument(id)).thenReturn(pdf);

        mockMvc.perform(get("/api/documents/download/" + id))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=document-" + id + ".pdf"))
                .andExpect(content().contentType(MediaType.APPLICATION_PDF_VALUE))
                .andExpect(content().bytes(pdf));
    }

    @Test
    @WithMockUser(roles = "HR")
    void testApproveDocument_Success() throws Exception {
        UUID id = UUID.randomUUID();
        DocumentRequest request = new DocumentRequest();
        request.setId(id);
        request.setStatus(DocumentStatus.APPROVED);
        when(documentService.approveDocument(id)).thenReturn(request);

        mockMvc.perform(post("/api/documents/approve/" + id)
                        .with(csrf())) // CSRF
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }


    @Test
    @WithMockUser(roles = "HR")
    void testGetPendingDocumentRequests_Success() throws Exception {
        List<DocumentRequest> requests = List.of(new DocumentRequest());
        when(documentService.getPendingDocumentRequests()).thenReturn(requests);

        mockMvc.perform(get("/api/documents/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }
}
