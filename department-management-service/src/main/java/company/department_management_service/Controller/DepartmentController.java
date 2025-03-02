package company.department_management_service.Controller;

import company.department_management_service.DTO.CreateDepartmentRequest;
import company.department_management_service.DTO.DepartmentDTO;
import company.department_management_service.Models.Department;
import company.department_management_service.Service.DepartmentService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/departments")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class DepartmentController {

    @Autowired
    private DepartmentService departmentService;

    @PostMapping
    public ResponseEntity<DepartmentDTO> createDepartment(@Valid @RequestBody CreateDepartmentRequest request) {
        log.info("Received request to create department: {}", request.getName());
        DepartmentDTO department = departmentService.createDepartment(request);
        log.info("Successfully created department with ID: {}", department.getDepartmentId());
        return ResponseEntity.ok(department);
    }

    @GetMapping("/{departmentId}")
    public ResponseEntity<DepartmentDTO> getDepartment(@PathVariable Long departmentId) {
        log.debug("Received request to fetch department with ID: {}", departmentId);
        DepartmentDTO department = departmentService.getDepartmentById(departmentId);
        log.debug("Retrieved department: {}", department.getName());
        return ResponseEntity.ok(department);
    }

    @GetMapping
    public ResponseEntity<List<DepartmentDTO>> getAllDepartments() {
        log.debug("Received request to fetch all departments");
        List<DepartmentDTO> departments = departmentService.getAllDepartments();
        log.debug("Retrieved {} departments", departments.size());
        return ResponseEntity.ok(departments);
    }

    @GetMapping("/location/{location}")
    public ResponseEntity<List<DepartmentDTO>> getDepartmentsByLocation(@PathVariable String location) {
        log.debug("Received request to fetch departments for location: {}", location);
        List<DepartmentDTO> departments = departmentService.getDepartmentsByLocation(location);
        log.debug("Retrieved {} departments for location: {}", departments.size(), location);
        return ResponseEntity.ok(departments);
    }

    @PutMapping("/{departmentId}")
    public ResponseEntity<DepartmentDTO> updateDepartment(
            @PathVariable Long departmentId,
            @Valid @RequestBody CreateDepartmentRequest request) {
        log.info("Received request to update department with ID: {}", departmentId);
        DepartmentDTO department = departmentService.updateDepartment(departmentId, request);
        log.info("Successfully updated department: {}", department.getName());
        return ResponseEntity.ok(department);
    }

    @PatchMapping("/{departmentId}/status")
    public ResponseEntity<?> updateDepartmentStatus(
            @PathVariable Long departmentId,
            @RequestBody Map<String, String> payload) {
        try {
            String statusValue = payload.get("status"); // Récupère la valeur du JSON
            if (statusValue == null) {
                return ResponseEntity.badRequest().body("Missing 'status' field in request body");
            }

            Department.DepartmentStatus departmentStatus = Department.DepartmentStatus.valueOf(statusValue.toUpperCase());
            log.info("Updating status to {} for department ID: {}", departmentStatus, departmentId);
            departmentService.updateDepartmentStatus(departmentId, departmentStatus);
            log.info("Successfully updated status for department ID: {}", departmentId);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            log.error("Invalid status value: {}", payload.get("status"));
            return ResponseEntity.badRequest().body("Invalid status. Allowed values: ACTIVE, INACTIVE, ...");
        }
    }
}