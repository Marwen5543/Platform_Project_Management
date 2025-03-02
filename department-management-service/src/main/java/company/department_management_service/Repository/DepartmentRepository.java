package company.department_management_service.Repository;

import company.department_management_service.Models.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
    Optional<Department> findByName(String name);
    List<Department> findByManagerId(Long managerId);
    List<Department> findByLocation(String location);
    boolean existsByName(String name);
}
