package company.performance_management_service.Models;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Data
@Entity
@Table(name = "objectives")
public class Objective {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long objectiveId;

    private Long employeeId;
    private String title;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    private ObjectiveStatus status;

    @Column(length = 1000)
    private String evaluation;

    @Column(length = 1000)
    private String comments;

    private Integer progressPercentage;
    private LocalDate lastUpdatedDate;

    public enum ObjectiveStatus {
        DRAFT, IN_PROGRESS, COMPLETED, CANCELLED
    }
}