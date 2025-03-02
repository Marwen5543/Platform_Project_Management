package company.performance_management_service.DTO;

import company.performance_management_service.Models.Objective;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ObjectiveDTO {
    private Long objectiveId;
    private Long employeeId;
    private String title;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private Objective.ObjectiveStatus status;
    private String evaluation;
    private String comments;
    private Integer progressPercentage;
    private LocalDate lastUpdatedDate;
}
