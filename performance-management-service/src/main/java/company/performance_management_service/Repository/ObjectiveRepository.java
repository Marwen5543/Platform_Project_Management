package company.performance_management_service.Repository;

import company.performance_management_service.Models.Objective;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ObjectiveRepository extends JpaRepository<Objective, Long> {
    List<Objective> findByEmployeeId(Long employeeId);
    List<Objective> findByEmployeeIdAndStatus(Long employeeId, Objective.ObjectiveStatus status);
}
