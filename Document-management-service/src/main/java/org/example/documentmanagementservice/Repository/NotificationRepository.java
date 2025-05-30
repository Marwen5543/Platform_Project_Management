package org.example.documentmanagementservice.Repository;

import org.example.documentmanagementservice.Model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    List<Notification> findByRecipientRoleAndReadFalse(String role);
}
