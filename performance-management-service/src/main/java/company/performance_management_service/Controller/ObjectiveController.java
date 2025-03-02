package company.performance_management_service.Controller;

import company.performance_management_service.DTO.CreateObjectiveRequest;
import company.performance_management_service.DTO.ObjectiveDTO;
import company.performance_management_service.Service.ObjectiveService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/objectives")
@Slf4j
public class ObjectiveController {

    @Autowired
    private ObjectiveService objectiveService;

    @PostMapping
    public ResponseEntity<ObjectiveDTO> createObjective(@Valid @RequestBody CreateObjectiveRequest request) {
        log.info("Received request to create objective for employee ID: {}", request.getEmployeeId());
        ObjectiveDTO objective = objectiveService.createObjective(request);
        log.info("Objective created with ID: {}", objective.getObjectiveId());
        return ResponseEntity.ok(objective);
    }

    @GetMapping("/{objectiveId}")
    public ResponseEntity<ObjectiveDTO> getObjective(@PathVariable Long objectiveId) {
        log.info("Received request to fetch objective with ID: {}", objectiveId);
        ObjectiveDTO objective = objectiveService.getObjectiveById(objectiveId);
        return ResponseEntity.ok(objective);
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<ObjectiveDTO>> getEmployeeObjectives(@PathVariable Long employeeId) {
        log.info("Received request to fetch objectives for employee ID: {}", employeeId);
        List<ObjectiveDTO> objectives = objectiveService.getEmployeeObjectives(employeeId);
        return ResponseEntity.ok(objectives);
    }

    @PatchMapping("/{objectiveId}/progress")
    public ResponseEntity<ObjectiveDTO> updateObjectiveProgress(
            @PathVariable Long objectiveId,
            @RequestParam Integer progressPercentage,
            @RequestParam(required = false) String comments) {
        log.info("Received request to update progress for objective ID: {}, progress: {}%", objectiveId, progressPercentage);
        ObjectiveDTO objective = objectiveService.updateObjectiveProgress(objectiveId, progressPercentage, comments);
        return ResponseEntity.ok(objective);
    }
}
