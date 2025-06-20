package company.user_management_service.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {

    private String userId;
    private String username;
    private String email;
    private UserRole role;
    private UserStatus status;

    // Employee-specific fields
    private String firstName;
    private String lastName;
    private String phone;
    private String address;
    private String hireDate;
    private Long departmentId;
    private Long managerId;
    private String position;
    private List<String> projectTitles;

    public enum UserRole {
        EMPLOYEE, MANAGER, HR, ADMIN, SUPER_ADMIN
    }

    public enum UserStatus {
        ACTIVE, INACTIVE
    }

    public List<String> getProjectTitles() {
        return projectTitles;
    }

    public void setProjectTitles(List<String> projectTitles) {
        this.projectTitles = projectTitles;
    }
}