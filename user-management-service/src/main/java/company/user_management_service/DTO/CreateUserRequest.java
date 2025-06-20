package company.user_management_service.DTO;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.Date;

@Data
public class CreateUserRequest {
    @NotBlank(message = "Username cannot be blank")
    @Size(min = 4, max = 20, message = "Username must be 4-20 characters")
    private String username;

    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password cannot be blank")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d@$!%*#?&]{8,}$",
            message = "Password must be 8+ chars with at least 1 letter and 1 number")
    private String password;

    private String role; // Will default to EMPLOYEE if null

    // Employee details
    private String firstName;
    private String lastName;

    @Pattern(regexp = "^\\+?[0-9\\s-]{10,}$", message = "Invalid phone number format")
    private String phone;

    private String address;
    private Date hireDate;
    private Long departmentId;
    private Long managerId;
    private String position;
}