package company.employee_management_service.Service;

import company.employee_management_service.DTO.CreateEmployeeRequest;
import company.employee_management_service.DTO.EmployeeDTO;
import company.employee_management_service.Models.Employee;
import company.employee_management_service.Repository.EmployeeRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class EmployeeService {

    private final EmployeeRepository employeeRepository;

    @Autowired
    public EmployeeService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
        log.info("EmployeeService initialized");
    }

    @PostConstruct
    public void init() {
        log.info("EmployeeService post-construct initialization completed");
    }

    @Transactional
    public EmployeeDTO createEmployee(CreateEmployeeRequest request) {
        log.info("Creating employee with email: {}", request.getEmail());
        try {
            if (employeeRepository.existsByEmail(request.getEmail())) {
                log.warn("Attempt to create employee with existing email: {}", request.getEmail());
                throw new RuntimeException("Email already exists");
            }

            Employee employee = new Employee();
            employee.setFirstName(request.getFirstName());
            employee.setLastName(request.getLastName());
            employee.setEmail(request.getEmail());
            employee.setPhone(request.getPhone());
            employee.setAddress(request.getAddress());
            employee.setHireDate(request.getHireDate());
            employee.setDepartmentId(request.getDepartmentId());
            employee.setManagerId(request.getManagerId());
            employee.setPosition(request.getPosition());
            employee.setStatus(Employee.EmployeeStatus.ACTIVE);

            Employee savedEmployee = employeeRepository.save(employee);
            log.info("Successfully created employee with ID: {}", savedEmployee.getEmployeeId());
            return convertToDTO(savedEmployee);
        } catch (Exception e) {
            log.error("Error creating employee: ", e);
            throw e;
        }
    }

    public EmployeeDTO getEmployeeById(Long employeeId) {
        log.info("Fetching employee with ID: {}", employeeId);
        try {
            Employee employee = employeeRepository.findById(employeeId)
                    .orElseThrow(() -> new RuntimeException("Employee not found"));
            log.info("Found employee: {}", employee.getEmail());
            return convertToDTO(employee);
        } catch (Exception e) {
            log.error("Error fetching employee with ID {}: ", employeeId, e);
            throw e;
        }
    }

    public List<EmployeeDTO> getEmployeesByDepartment(Long departmentId) {
        log.info("Fetching employees for department ID: {}", departmentId);
        try {
            List<EmployeeDTO> employees = employeeRepository.findByDepartmentId(departmentId)
                    .stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            log.info("Found {} employees in department {}", employees.size(), departmentId);
            return employees;
        } catch (Exception e) {
            log.error("Error fetching employees for department {}: ", departmentId, e);
            throw e;
        }
    }

    public List<EmployeeDTO> getEmployeesByManager(Long managerId) {
        log.info("Fetching employees for manager ID: {}", managerId);
        try {
            List<EmployeeDTO> employees = employeeRepository.findByManagerId(managerId)
                    .stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            log.info("Found {} employees under manager {}", employees.size(), managerId);
            return employees;
        } catch (Exception e) {
            log.error("Error fetching employees for manager {}: ", managerId, e);
            throw e;
        }
    }

    @Transactional
    public EmployeeDTO updateEmployee(Long employeeId, CreateEmployeeRequest request) {
        log.info("Updating employee with ID: {}", employeeId);
        try {
            Employee employee = employeeRepository.findById(employeeId)
                    .orElseThrow(() -> new RuntimeException("Employee not found"));

            if (!employee.getEmail().equals(request.getEmail()) &&
                    employeeRepository.existsByEmail(request.getEmail())) {
                log.warn("Attempt to update employee with existing email: {}", request.getEmail());
                throw new RuntimeException("Email already exists");
            }

            employee.setFirstName(request.getFirstName());
            employee.setLastName(request.getLastName());
            employee.setEmail(request.getEmail());
            employee.setPhone(request.getPhone());
            employee.setAddress(request.getAddress());
            employee.setHireDate(request.getHireDate());
            employee.setDepartmentId(request.getDepartmentId());
            employee.setManagerId(request.getManagerId());
            employee.setPosition(request.getPosition());

            Employee updatedEmployee = employeeRepository.save(employee);
            log.info("Successfully updated employee with ID: {}", employeeId);
            return convertToDTO(updatedEmployee);
        } catch (Exception e) {
            log.error("Error updating employee with ID {}: ", employeeId, e);
            throw e;
        }
    }

    private EmployeeDTO convertToDTO(Employee employee) {
        EmployeeDTO dto = new EmployeeDTO();
        dto.setEmployeeId(employee.getEmployeeId());
        dto.setFirstName(employee.getFirstName());
        dto.setLastName(employee.getLastName());
        dto.setEmail(employee.getEmail());
        dto.setPhone(employee.getPhone());
        dto.setAddress(employee.getAddress());
        dto.setHireDate(employee.getHireDate());
        dto.setDepartmentId(employee.getDepartmentId());
        dto.setManagerId(employee.getManagerId());
        dto.setPosition(employee.getPosition());
        dto.setStatus(employee.getStatus());
        return dto;
    }
}