package company.department_management_service.DTO;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateDepartmentRequest {
    @NotBlank(message = "Department name is required")
    private String name;

    private String description;
    private Long managerId;

    @NotBlank(message = "Location is required")
    private String location;
}
