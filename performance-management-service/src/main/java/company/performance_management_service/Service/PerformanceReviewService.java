package company.performance_management_service.Service;

import company.performance_management_service.DTO.PerformanceReviewDTO;
import company.performance_management_service.Models.PerformanceReview;
import company.performance_management_service.Repository.PerformanceReviewRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j  // Added Slf4j for logging
public class PerformanceReviewService {

    @Autowired
    private PerformanceReviewRepository reviewRepository;

    @Transactional
    public PerformanceReviewDTO createReview(Long employeeId, Long reviewerId, String reviewPeriod) {
        log.info("Creating performance review for employee ID: {}, reviewer ID: {}, period: {}", employeeId, reviewerId, reviewPeriod);

        PerformanceReview review = new PerformanceReview();
        review.setEmployeeId(employeeId);
        review.setReviewerId(reviewerId);
        review.setReviewPeriod(reviewPeriod);
        review.setReviewDate(LocalDate.now());
        review.setStatus(PerformanceReview.ReviewStatus.DRAFT);

        PerformanceReview savedReview = reviewRepository.save(review);
        log.info("Performance review created with ID: {}", savedReview.getReviewId());
        return convertToDTO(savedReview);
    }

    public List<PerformanceReviewDTO> getEmployeeReviews(Long employeeId) {
        log.info("Fetching performance reviews for employee ID: {}", employeeId);
        return reviewRepository.findByEmployeeId(employeeId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public PerformanceReviewDTO updateReview(Long reviewId, PerformanceReviewDTO updateRequest) {
        log.info("Updating performance review with ID: {}", reviewId);

        PerformanceReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> {
                    log.error("Review not found with ID: {}", reviewId);
                    return new RuntimeException("Review not found");
                });

        review.setOverallEvaluation(updateRequest.getOverallEvaluation());
        review.setRating(updateRequest.getRating());
        review.setStrengths(updateRequest.getStrengths());
        review.setAreasForImprovement(updateRequest.getAreasForImprovement());
        review.setDevelopmentPlan(updateRequest.getDevelopmentPlan());

        PerformanceReview updatedReview = reviewRepository.save(review);
        log.info("Performance review updated with ID: {}", updatedReview.getReviewId());
        return convertToDTO(updatedReview);
    }

    private PerformanceReviewDTO convertToDTO(PerformanceReview review) {
        log.debug("Converting PerformanceReview entity to DTO for review ID: {}", review.getReviewId());
        PerformanceReviewDTO dto = new PerformanceReviewDTO();
        dto.setReviewId(review.getReviewId());
        dto.setEmployeeId(review.getEmployeeId());
        dto.setReviewerId(review.getReviewerId());
        dto.setReviewPeriod(review.getReviewPeriod());
        dto.setReviewDate(review.getReviewDate());
        dto.setOverallEvaluation(review.getOverallEvaluation());
        dto.setRating(review.getRating());
        dto.setStrengths(review.getStrengths());
        dto.setAreasForImprovement(review.getAreasForImprovement());
        dto.setDevelopmentPlan(review.getDevelopmentPlan());
        dto.setStatus(review.getStatus());
        return dto;
    }
}
