package company.employee_management_service.DTO;

import company.employee_management_service.Models.Employee;
import lombok.Data;
import java.time.LocalDate;

@Data
public class EmployeeDTO {
    private Long employeeId;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String address;
    private LocalDate hireDate;
    private Long departmentId;
    private Long managerId;
    private String position;
    private Employee.EmployeeStatus status;
}