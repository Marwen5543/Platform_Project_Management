package company.employee_management_service.Controller;

import company.employee_management_service.DTO.CreateEmployeeRequest;
import company.employee_management_service.DTO.EmployeeDTO;
import company.employee_management_service.Service.EmployeeService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/employees")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@Slf4j
public class EmployeeController {

    private final EmployeeService employeeService;

    @Autowired
    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
        log.info("EmployeeController initialized");
    }

    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("Employee service is working");
    }

    @PostMapping
    public ResponseEntity<EmployeeDTO> createEmployee(@Valid @RequestBody CreateEmployeeRequest request) {
        log.info("Received request to create employee with email: {}", request.getEmail());
        try {
            EmployeeDTO employee = employeeService.createEmployee(request);
            log.info("Successfully created employee with ID: {}", employee.getEmployeeId());
            return ResponseEntity.ok(employee);
        } catch (Exception e) {
            log.error("Error creating employee: ", e);
            throw e;
        }
    }

    @GetMapping("/{employeeId}")
    public ResponseEntity<EmployeeDTO> getEmployee(@PathVariable Long employeeId) {
        log.info("Received request to get employee with ID: {}", employeeId);
        try {
            EmployeeDTO employee = employeeService.getEmployeeById(employeeId);
            log.info("Successfully retrieved employee with ID: {}", employeeId);
            return ResponseEntity.ok(employee);
        } catch (Exception e) {
            log.error("Error retrieving employee with ID {}: ", employeeId, e);
            throw e;
        }
    }

    @GetMapping("/department/{departmentId}")
    public ResponseEntity<List<EmployeeDTO>> getEmployeesByDepartment(@PathVariable Long departmentId) {
        log.info("Received request to get employees for department ID: {}", departmentId);
        try {
            List<EmployeeDTO> employees = employeeService.getEmployeesByDepartment(departmentId);
            log.info("Found {} employees in department {}", employees.size(), departmentId);
            return ResponseEntity.ok(employees);
        } catch (Exception e) {
            log.error("Error retrieving employees for department {}: ", departmentId, e);
            throw e;
        }
    }

    @GetMapping("/manager/{managerId}")
    public ResponseEntity<List<EmployeeDTO>> getEmployeesByManager(@PathVariable Long managerId) {
        log.info("Received request to get employees for manager ID: {}", managerId);
        try {
            List<EmployeeDTO> employees = employeeService.getEmployeesByManager(managerId);
            log.info("Found {} employees under manager {}", employees.size(), managerId);
            return ResponseEntity.ok(employees);
        } catch (Exception e) {
            log.error("Error retrieving employees for manager {}: ", managerId, e);
            throw e;
        }
    }

    @PutMapping("/{employeeId}")
    public ResponseEntity<EmployeeDTO> updateEmployee(
            @PathVariable Long employeeId,
            @Valid @RequestBody CreateEmployeeRequest request) {
        log.info("Received request to update employee with ID: {}", employeeId);
        try {
            EmployeeDTO employee = employeeService.updateEmployee(employeeId, request);
            log.info("Successfully updated employee with ID: {}", employeeId);
            return ResponseEntity.ok(employee);
        } catch (Exception e) {
            log.error("Error updating employee with ID {}: ", employeeId, e);
            throw e;
        }
    }

}