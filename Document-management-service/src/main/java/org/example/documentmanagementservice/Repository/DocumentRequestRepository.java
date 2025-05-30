package org.example.documentmanagementservice.Repository;

import org.example.documentmanagementservice.Model.DocumentRequest;
import org.example.documentmanagementservice.Model.DocumentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DocumentRequestRepository extends JpaRepository<DocumentRequest, UUID> {
    List<DocumentRequest> findByEmployeeId(String employeeId);
    List<DocumentRequest> findByStatus(DocumentStatus status);
}
