package org.example.documentmanagementservice.Model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
public class Notification {
    @Id
    private UUID id = UUID.randomUUID();
    private String recipientRole; // e.g., "HR"
    private UUID documentRequestId;
    private String message;
    private LocalDateTime createdAt;
    private boolean read;
}

