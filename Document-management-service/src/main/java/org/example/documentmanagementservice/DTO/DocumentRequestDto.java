package org.example.documentmanagementservice.DTO;


import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.example.documentmanagementservice.Model.DocumentType;

@Data
public class DocumentRequestDto {
    @NotNull(message = "Document type is required")
    private DocumentType documentType;

    @NotNull(message = "Month/Year is required")
    private String monthYear;

    public DocumentType getDocumentType() {
        return documentType;
    }

    public void setDocumentType(DocumentType documentType) {
        this.documentType = documentType;
    }

    public String getMonthYear() {
        return monthYear;
    }

    public void setMonthYear(String monthYear) {
        this.monthYear = monthYear;
    }
}
