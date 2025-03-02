package company.performance_management_service.DTO;

import company.performance_management_service.Models.PerformanceReview;
import lombok.Data;

import java.time.LocalDate;

@Data

public class PerformanceReviewDTO {
    private Long reviewId;
    private Long employeeId;
    private Long reviewerId;
    private String reviewPeriod;
    private LocalDate reviewDate;
    private String overallEvaluation;
    private Integer rating;
    private String strengths;
    private String areasForImprovement;
    private String developmentPlan;
    private PerformanceReview.ReviewStatus status;
}