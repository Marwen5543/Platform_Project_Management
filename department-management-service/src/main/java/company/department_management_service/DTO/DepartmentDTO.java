package company.department_management_service.DTO;

import company.department_management_service.Models.Department;
import lombok.Data;

@Data
public class DepartmentDTO {
    private Long departmentId;
    private String name;
    private String description;
    private Long managerId;
    private String location;
    private Department.DepartmentStatus status;
}