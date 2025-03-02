package company.performance_management_service.Service;

import company.performance_management_service.DTO.CreateObjectiveRequest;
import company.performance_management_service.DTO.ObjectiveDTO;
import company.performance_management_service.Models.Objective;
import company.performance_management_service.Repository.ObjectiveRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j  // Added Slf4j for logging
public class ObjectiveService {

    @Autowired
    private ObjectiveRepository objectiveRepository;

    @Transactional
    public ObjectiveDTO createObjective(CreateObjectiveRequest request) {
        log.info("Creating objective for employee ID: {}, title: {}", request.getEmployeeId(), request.getTitle());
        validateObjective(request);

        Objective objective = new Objective();
        objective.setEmployeeId(request.getEmployeeId());
        objective.setTitle(request.getTitle());
        objective.setDescription(request.getDescription());
        objective.setStartDate(request.getStartDate());
        objective.setEndDate(request.getEndDate());
        objective.setStatus(Objective.ObjectiveStatus.DRAFT);
        objective.setProgressPercentage(0);
        objective.setLastUpdatedDate(LocalDate.now());

        Objective savedObjective = objectiveRepository.save(objective);
        log.info("Objective created with ID: {}", savedObjective.getObjectiveId());
        return convertToDTO(savedObjective);
    }

    public ObjectiveDTO getObjectiveById(Long objectiveId) {
        log.info("Fetching objective with ID: {}", objectiveId);
        Objective objective = objectiveRepository.findById(objectiveId)
                .orElseThrow(() -> {
                    log.error("Objective not found with ID: {}", objectiveId);
                    return new RuntimeException("Objective not found");
                });
        return convertToDTO(objective);
    }

    public List<ObjectiveDTO> getEmployeeObjectives(Long employeeId) {
        log.info("Fetching objectives for employee ID: {}", employeeId);
        return objectiveRepository.findByEmployeeId(employeeId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public ObjectiveDTO updateObjectiveProgress(Long objectiveId, Integer progressPercentage, String comments) {
        log.info("Updating progress for objective ID: {}, progress: {}%", objectiveId, progressPercentage);

        Objective objective = objectiveRepository.findById(objectiveId)
                .orElseThrow(() -> {
                    log.error("Objective not found with ID: {}", objectiveId);
                    return new RuntimeException("Objective not found");
                });

        if (progressPercentage < 0 || progressPercentage > 100) {
            log.error("Invalid progress percentage: {} for objective ID: {}", progressPercentage, objectiveId);
            throw new RuntimeException("Progress percentage must be between 0 and 100");
        }

        objective.setProgressPercentage(progressPercentage);
        objective.setComments(comments);
        objective.setLastUpdatedDate(LocalDate.now());

        if (progressPercentage == 100) {
            objective.setStatus(Objective.ObjectiveStatus.COMPLETED);
        } else if (objective.getStatus() == Objective.ObjectiveStatus.DRAFT) {
            objective.setStatus(Objective.ObjectiveStatus.IN_PROGRESS);
        }

        Objective updatedObjective = objectiveRepository.save(objective);
        log.info("Objective updated with ID: {}", updatedObjective.getObjectiveId());
        return convertToDTO(updatedObjective);
    }

    private void validateObjective(CreateObjectiveRequest request) {
        if (request.getStartDate().isAfter(request.getEndDate())) {
            log.error("Start date {} cannot be after end date {}", request.getStartDate(), request.getEndDate());
            throw new RuntimeException("Start date cannot be after end date");
        }
    }

    private ObjectiveDTO convertToDTO(Objective objective) {
        log.debug("Converting Objective entity to DTO for objective ID: {}", objective.getObjectiveId());
        ObjectiveDTO dto = new ObjectiveDTO();
        dto.setObjectiveId(objective.getObjectiveId());
        dto.setEmployeeId(objective.getEmployeeId());
        dto.setTitle(objective.getTitle());
        dto.setDescription(objective.getDescription());
        dto.setStartDate(objective.getStartDate());
        dto.setEndDate(objective.getEndDate());
        dto.setStatus(objective.getStatus());
        dto.setEvaluation(objective.getEvaluation());
        dto.setComments(objective.getComments());
        dto.setProgressPercentage(objective.getProgressPercentage());
        dto.setLastUpdatedDate(objective.getLastUpdatedDate());
        return dto;
    }
}
