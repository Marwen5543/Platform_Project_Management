package company.Leave_Management_service.DTO;

import lombok.Data;

@Data
public class UserDTO {
    private String userId;
    private String username;
    private String email;
    private UserRole role;
    private UserStatus status;
    private String firstName;
    private String lastName;
    private String phone;
    private String address;
    private String hireDate;
    private Long departmentId;
    private Long managerId;
    private String position;

    public enum UserRole {
        EMPLOYEE, MANAGER, HR, ADMIN, SUPER_ADMIN
    }

    public enum UserStatus {
        ACTIVE, INACTIVE
    }
}