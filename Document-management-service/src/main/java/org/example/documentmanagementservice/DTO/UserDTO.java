package org.example.documentmanagementservice.DTO;

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

    public UserDTO(String userId, String firstName, String lastName, String email) {
        this.userId = userId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
    }

    public enum UserRole {
        EMPLOYEE, MANAGER, HR, ADMIN, SUPER_ADMIN
    }

    public enum UserStatus {
        ACTIVE, INACTIVE
    }
}