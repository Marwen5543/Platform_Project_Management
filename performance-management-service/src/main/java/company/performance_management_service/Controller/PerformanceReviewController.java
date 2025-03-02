package company.performance_management_service.Controller;

import company.performance_management_service.DTO.PerformanceReviewDTO;
import company.performance_management_service.Service.PerformanceReviewService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@Slf4j
public class PerformanceReviewController {

    @Autowired
    private PerformanceReviewService reviewService;

    @PostMapping
    public ResponseEntity<PerformanceReviewDTO> createReview(
            @RequestParam Long employeeId,
            @RequestParam Long reviewerId,
            @RequestParam String reviewPeriod) {
        log.info("Received request to create performance review for employee ID: {}, reviewer ID: {}, review period: {}", employeeId, reviewerId, reviewPeriod);

        PerformanceReviewDTO review = reviewService.createReview(employeeId, reviewerId, reviewPeriod);
        log.info("Performance review created with ID: {}", review.getReviewId());
        return ResponseEntity.ok(review);
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<PerformanceReviewDTO>> getEmployeeReviews(@PathVariable Long employeeId) {
        log.info("Received request to fetch reviews for employee ID: {}", employeeId);
        List<PerformanceReviewDTO> reviews = reviewService.getEmployeeReviews(employeeId);
        return ResponseEntity.ok(reviews);
    }

    @PutMapping("/{reviewId}")
    public ResponseEntity<PerformanceReviewDTO> updateReview(
            @PathVariable Long reviewId,
            @RequestBody PerformanceReviewDTO updateRequest) {
        log.info("Received request to update review with ID: {}", reviewId);
        PerformanceReviewDTO review = reviewService.updateReview(reviewId, updateRequest);
        return ResponseEntity.ok(review);
    }
}
