package company.performance_management_service.Models;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Data
@Entity
public class PerformanceReview {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reviewId;

    private Long employeeId;
    private Long reviewerId;
    private String reviewPeriod;
    private LocalDate reviewDate;

    @Column(length = 2000)
    private String overallEvaluation;

    private Integer rating;
    private String strengths;
    private String areasForImprovement;
    private String developmentPlan;

    @Enumerated(EnumType.STRING)
    private ReviewStatus status;

    public enum ReviewStatus {
        DRAFT, SUBMITTED, ACKNOWLEDGED, COMPLETED
    }
}
