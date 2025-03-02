package company.department_management_service.Models;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "departments")
public class Department {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long departmentId;

    @Column(unique = true)
    private String name;

    private String description;
    private Long managerId;
    private String location;

    @Enumerated(EnumType.STRING)
    private DepartmentStatus status;

    public enum DepartmentStatus {
        ACTIVE, INACTIVE, RESTRUCTURING
    }
}