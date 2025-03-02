package company.department_management_service.Service;

import company.department_management_service.DTO.CreateDepartmentRequest;
import company.department_management_service.DTO.DepartmentDTO;
import company.department_management_service.Models.Department;
import company.department_management_service.Repository.DepartmentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DepartmentService {

    @Autowired
    private DepartmentRepository departmentRepository;

    @Transactional
    public DepartmentDTO createDepartment(CreateDepartmentRequest request) {
        log.info("Creating new department with name: {}", request.getName());

        if (departmentRepository.existsByName(request.getName())) {
            log.warn("Attempt to create department with existing name: {}", request.getName());
            throw new RuntimeException("Department name already exists");
        }

        Department department = new Department();
        department.setName(request.getName());
        department.setDescription(request.getDescription());
        department.setManagerId(request.getManagerId());
        department.setLocation(request.getLocation());
        department.setStatus(Department.DepartmentStatus.ACTIVE);

        Department savedDepartment = departmentRepository.save(department);
        log.info("Successfully created department with ID: {}", savedDepartment.getDepartmentId());
        return convertToDTO(savedDepartment);
    }

    public DepartmentDTO getDepartmentById(Long departmentId) {
        log.debug("Fetching department with ID: {}", departmentId);

        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> {
                    log.error("Department not found with ID: {}", departmentId);
                    return new RuntimeException("Department not found");
                });
        return convertToDTO(department);
    }

    public List<DepartmentDTO> getAllDepartments() {
        log.debug("Fetching all departments");
        List<DepartmentDTO> departments = departmentRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        log.debug("Retrieved {} departments", departments.size());
        return departments;
    }

    public List<DepartmentDTO> getDepartmentsByLocation(String location) {
        log.debug("Fetching departments for location: {}", location);
        List<DepartmentDTO> departments = departmentRepository.findByLocation(location)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        log.debug("Found {} departments in location: {}", departments.size(), location);
        return departments;
    }

    @Transactional
    public DepartmentDTO updateDepartment(Long departmentId, CreateDepartmentRequest request) {
        log.info("Updating department with ID: {}", departmentId);

        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> {
                    log.error("Department not found with ID: {}", departmentId);
                    return new RuntimeException("Department not found");
                });

        if (!department.getName().equals(request.getName()) &&
                departmentRepository.existsByName(request.getName())) {
            log.warn("Attempt to update department with existing name: {}", request.getName());
            throw new RuntimeException("Department name already exists");
        }

        department.setName(request.getName());
        department.setDescription(request.getDescription());
        department.setManagerId(request.getManagerId());
        department.setLocation(request.getLocation());

        Department updatedDepartment = departmentRepository.save(department);
        log.info("Successfully updated department with ID: {}", departmentId);
        return convertToDTO(updatedDepartment);
    }

    @Transactional
    public void updateDepartmentStatus(Long departmentId, Department.DepartmentStatus status) {
        log.info("Updating status to {} for department ID: {}", status, departmentId);

        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> {
                    log.error("Department not found with ID: {}", departmentId);
                    return new RuntimeException("Department not found");
                });
        department.setStatus(status);
        departmentRepository.save(department);
        log.info("Successfully updated status to {} for department ID: {}", status, departmentId);
    }

    private DepartmentDTO convertToDTO(Department department) {
        DepartmentDTO dto = new DepartmentDTO();
        dto.setDepartmentId(department.getDepartmentId());
        dto.setName(department.getName());
        dto.setDescription(department.getDescription());
        dto.setManagerId(department.getManagerId());
        dto.setLocation(department.getLocation());
        dto.setStatus(department.getStatus());
        return dto;
    }
}