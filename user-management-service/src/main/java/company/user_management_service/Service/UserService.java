package company.user_management_service.Service;

import company.user_management_service.DTO.CreateUserRequest;
import company.user_management_service.DTO.LoginRequest;
import company.user_management_service.DTO.LoginResponse;
import company.user_management_service.DTO.UserDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.security.core.GrantedAuthority;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UserService {

    @Autowired
    private KeycloakService keycloakService;

    public void createUser(CreateUserRequest request) {
        log.info("Creating user in Keycloak: username={}, email={}", request.getUsername(), request.getEmail());
        try {
            String hireDateToStore;

            // FIXED: Always ensure we have a valid hireDate
            if (request.getHireDate() != null && !request.getHireDate().trim().isEmpty()) {
                hireDateToStore = request.getHireDate().trim();
                log.info("Using provided hire date: {}", hireDateToStore);
            } else {
                // Auto-generate today's date when hireDate is not provided
                hireDateToStore = LocalDate.now().toString(); // Format: "YYYY-MM-DD"
                log.info("Auto-generating hire date: {}", hireDateToStore);
            }

            // Create user with proper hireDate
            keycloakService.createUser(
                    request.getUsername(),
                    request.getEmail(),
                    request.getPassword(),
                    request.getRole() != null ? request.getRole() : "EMPLOYEE",
                    request.getFirstName(),
                    request.getLastName(),
                    request.getPhone(),
                    request.getAddress(),
                    hireDateToStore, // This should never be null or empty now
                    request.getDepartmentId(),
                    request.getManagerId(),
                    request.getPosition()
            );

            log.info("User successfully created in Keycloak: {} with hireDate: {}",
                    request.getUsername(), hireDateToStore);

        } catch (Exception e) {
            log.error("Error during user creation in Keycloak: {}", e.getMessage());
            throw new RuntimeException("Failed to create user in Keycloak: " + e.getMessage(), e);
        }
    }


    public LoginResponse login(LoginRequest request) {
        log.info("Processing login request for username: {}", request.getUsername());
        if (request.getUsername() == null || request.getPassword() == null) {
            throw new BadCredentialsException("Username and password are required");
        }

        try {
            String token = keycloakService.getToken(request.getUsername(), request.getPassword());
            log.info("Keycloak authentication successful for user: {}", request.getUsername());
            Map<String, Object> userDetails = keycloakService.getUserDetails(request.getUsername());
            return buildLoginResponse(userDetails, token);
        } catch (Exception e) {
            log.error("Keycloak authentication failed: {}", e.getMessage());
            throw new BadCredentialsException("Invalid credentials");
        }
    }

    private LoginResponse buildLoginResponse(Map<String, Object> userDetails, String token) {
        return LoginResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .forcePasswordReset(Boolean.parseBoolean(userDetails.getOrDefault("forcePasswordReset", "false").toString()))
                .username(userDetails.get("username").toString())
                .role(userDetails.get("role").toString())
                .firstName(userDetails.get("firstName").toString())
                .lastName(userDetails.get("lastName").toString())
                .phone(getStringOrNull(userDetails, "phone"))
                .address(getStringOrNull(userDetails, "address"))
                .hireDate(getStringOrNull(userDetails, "hireDate"))
                .departmentId(getLongOrNull(userDetails, "departmentId"))
                .managerId(getLongOrNull(userDetails, "managerId"))
                .position(getStringOrNull(userDetails, "position"))
                .build();
    }

    public UserDTO getUserById(String userId) {
        log.info("Fetching user with ID from Keycloak: {}", userId);
        Map<String, Object> userDetails = keycloakService.getUserDetailsById(userId);
        return convertToDTO(userDetails);
    }

    private UserDTO convertToDTO(Map<String, Object> userDetails) {
        log.debug("Converting Keycloak user details to DTO: {}", userDetails);
        UserDTO dto = new UserDTO();

        dto.setUserId(getStringOrNull(userDetails, "userId"));
        dto.setUsername(getStringOrNull(userDetails, "username"));
        dto.setEmail(getStringOrNull(userDetails, "email"));
        dto.setFirstName(getStringOrNull(userDetails, "firstName"));
        dto.setLastName(getStringOrNull(userDetails, "lastName"));
        dto.setPhone(getStringOrNull(userDetails, "phone"));
        dto.setAddress(getStringOrNull(userDetails, "address"));

        // FIXED: Better hireDate handling
        String hireDate = getStringOrNull(userDetails, "hireDate");
        dto.setHireDate(hireDate);
        log.debug("Set hireDate in DTO: {} for user: {}", hireDate, dto.getUsername());

        dto.setDepartmentId(getLongOrNull(userDetails, "departmentId"));
        dto.setManagerId(getLongOrNull(userDetails, "managerId"));
        dto.setPosition(getStringOrNull(userDetails, "position"));

        String status = getStringOrNull(userDetails, "status");
        try {
            dto.setStatus(status != null ? UserDTO.UserStatus.valueOf(status.toUpperCase()) : UserDTO.UserStatus.ACTIVE);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid status '{}', defaulting to ACTIVE for user {}", status, dto.getUsername());
            dto.setStatus(UserDTO.UserStatus.ACTIVE);
        }

        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) userDetails.getOrDefault("roles", Collections.emptyList());
        log.debug("Roles for user {}: {}", dto.getUsername(), roles);

        @SuppressWarnings("unchecked")
        List<String> projectTitles = userDetails.containsKey("projectTitles") ?
                (List<String>) userDetails.get("projectTitles") : new ArrayList<>();
        dto.setProjectTitles(projectTitles);

        UserDTO.UserRole mappedRole = null;
        for (String role : roles) {
            try {
                mappedRole = UserDTO.UserRole.valueOf(role.toUpperCase());
                log.info("Assigned role {} to user {}", mappedRole, dto.getUsername());
                break;
            } catch (IllegalArgumentException e) {
                log.debug("Role '{}' not found in UserRole enum, skipping", role);
                continue;
            }
        }

        if (mappedRole == null) {
            log.warn("No valid roles found for user {}, defaulting to EMPLOYEE", dto.getUsername());
            mappedRole = UserDTO.UserRole.EMPLOYEE;
        }
        dto.setRole(mappedRole);

        return dto;
    }

    // UserService.java

    private String getStringOrNull(Map<String, Object> map, String key) {
        Object value = map.get(key);

        // Special handling for hireDate - don't return null for empty strings
        if ("hireDate".equals(key)) {
            if (value == null) {
                return null;
            }
            String stringValue = value.toString().trim();
            // For hireDate, return the value even if it's an empty string (let calling code handle it)
            return stringValue.isEmpty() ? null : stringValue;
        }

        // For other fields, return null if value is null or empty
        if (value == null || value.toString().trim().isEmpty()) {
            return null;
        }
        return value.toString().trim();
    }

    private Long getLongOrNull(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return (value != null && !value.toString().isEmpty()) ? Long.parseLong(value.toString()) : null;
    }

    public void changeUserRole(String userId, String newRole) {
        log.info("Starting role change for userId: {} to role: {}", userId, newRole);

        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                log.error("No authenticated user found");
                throw new RuntimeException("User not authenticated");
            }

            List<String> authorities = auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList());
            log.info("Authorities for user {}: {}", auth.getName(), authorities);

            // Check for SUPER_ADMIN with or without ROLE_ prefix
            boolean isSuperAdmin = authorities.stream()
                    .anyMatch(role -> role.equals("SUPER_ADMIN") || role.equals("ROLE_SUPER_ADMIN"));
            if (!isSuperAdmin) {
                log.error("User {} does not have SUPER_ADMIN role to change roles", auth.getName());
                throw new SecurityException("Only SUPER_ADMIN can change user roles");
            }

            String currentAdminUsername = auth.getName();
            log.info("Current admin: {}", currentAdminUsername);

            Map<String, Object> userDetails = keycloakService.getUserDetailsById(userId);
            if (userDetails == null || userDetails.get("username") == null) {
                log.error("User not found or username missing for userId: {}", userId);
                throw new RuntimeException("User not found in Keycloak");
            }

            String targetUsername = userDetails.get("username").toString();
            log.info("Target username: {}", targetUsername);

            try {
                UserDTO.UserRole.valueOf(newRole.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.error("Invalid role: {}", newRole);
                throw new IllegalArgumentException("Invalid role: " + newRole);
            }

            keycloakService.updateUserRole(targetUsername, newRole);
            log.info("Successfully changed role for user {} to {}", targetUsername, newRole);
        } catch (Exception e) {
            log.error("Failed to change role for userId {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to change user role: " + e.getMessage(), e);
        }
    }

    public Map<String, Object> getUserFullDetails(String username) {
        log.info("Fetching all details for user with username from Keycloak: {}", username);
        return keycloakService.getUserDetails(username);
    }

    public void changePassword(String username, String currentPassword, String newPassword) {
        if (newPassword.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters");
        }

        try {
            keycloakService.updatePassword(username, currentPassword, newPassword);
            log.info("Password updated in Keycloak for user: {}", username);
        } catch (Exception e) {
            log.error("Failed to update Keycloak password: {}", e.getMessage());
            throw new RuntimeException("Failed to update Keycloak password", e);
        }
    }

    public List<UserDTO> getAllUsers() {
        log.info("Fetching all users from Keycloak");
        List<Map<String, Object>> users = keycloakService.getAllUsers();
        return users.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public Map<String, Object> getCurrentUserRoles() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User is not authenticated");
        }

        String username = authentication.getName();
        Map<String, Object> userDetails = keycloakService.getUserDetails(username);

        List<String> keycloakRoles = authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        // Prioritize a meaningful role (e.g., SUPER_ADMIN or ADMIN) over default-roles-demo
        String primaryRole = keycloakRoles.stream()
                .map(role -> role.startsWith("ROLE_") ? role.substring(5) : role) // Remove ROLE_ prefix
                .filter(role -> List.of("SUPER_ADMIN", "ADMIN", "MANAGER", "HR", "EMPLOYEE").contains(role))
                .findFirst()
                .orElse((String) userDetails.get("role")); // Fallback to Keycloak's role

        Map<String, Object> response = new HashMap<>();
        response.put("primaryRole", primaryRole); // Renamed for clarity
        response.put("keycloakRoles", keycloakRoles);
        return response;
    }

    public UserDTO getUserByUsername(String username) {
        log.info("Fetching user by username from Keycloak: {}", username);
        Map<String, Object> userDetails = keycloakService.getUserDetails(username);
        return convertToDTO(userDetails);
    }

    public UserDTO updateProfile(String userId, UserDTO updateRequest) {
        log.info("Updating profile for user with ID: {}", userId);
        try {
            // Validate user exists
            Map<String, Object> userDetails = keycloakService.getUserDetailsById(userId);
            if (userDetails == null || userDetails.isEmpty()) {
                log.warn("User not found with ID: {}", userId);
                throw new RuntimeException("User not found");
            }

            // Delegate the entire DTO to Keycloak
            keycloakService.updateUserAttributes(userId, updateRequest);

            log.info("User profile updated successfully for ID: {}", userId);

            // Retrieve updated info
            Map<String, Object> updatedDetails = keycloakService.getUserDetailsById(userId);
            return convertToDTO(updatedDetails);
        } catch (Exception e) {
            log.error("Error updating user profile: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update user profile", e);
        }
    }

    public Map<String, String> getUserNameAndRoleByUsername(String username) {
        log.info("Fetching name and role for user with username: {}", username);
        Map<String, Object> userDetails = keycloakService.getUserDetails(username);

        Map<String, String> nameAndRole = new HashMap<>();

        // Extract first and last name
        String firstName = getStringOrNull(userDetails, "firstName");
        String lastName = getStringOrNull(userDetails, "lastName");
        String fullName = (firstName != null ? firstName : "") +
                (firstName != null && lastName != null ? " " : "") +
                (lastName != null ? lastName : "");

        // Get the role
        String role = getStringOrNull(userDetails, "role");

        nameAndRole.put("name", fullName.trim());
        nameAndRole.put("role", role);

        log.info("Retrieved name '{}' and role '{}' for username: {}", fullName.trim(), role, username);
        return nameAndRole;
    }

    public Map<String, String> getUserNameAndRole(String userId) {
        log.info("Fetching name and role for user with ID: {}", userId);
        Map<String, Object> userDetails = keycloakService.getUserDetailsById(userId);

        Map<String, String> nameAndRole = new HashMap<>();

        // Extract first and last name
        String firstName = getStringOrNull(userDetails, "firstName");
        String lastName = getStringOrNull(userDetails, "lastName");
        String fullName = (firstName != null ? firstName : "") +
                (firstName != null && lastName != null ? " " : "") +
                (lastName != null ? lastName : "");

        // Get the role
        String role = getStringOrNull(userDetails, "role");

        nameAndRole.put("name", fullName.trim());
        nameAndRole.put("role", role);

        log.info("Retrieved name '{}' and role '{}' for user ID: {}", fullName.trim(), role, userId);
        return nameAndRole;
    }

    public void deleteUser(String username) {
        log.info("Deleting user with username: {}", username);
        try {
            keycloakService.deleteUser(username);
            log.info("Successfully deleted user: {}", username);
        } catch (Exception e) {
            log.error("Failed to delete user {}: {}", username, e.getMessage());
            throw new RuntimeException("Failed to delete user: " + e.getMessage(), e);
        }
    }

    public UserDTO deassignProject(String userId, String projectTitle) {
        log.info("Deassigning project '{}' for user with ID: {}", projectTitle, userId);
        try {
            // Validate inputs
            if (userId == null || projectTitle == null || userId.trim().isEmpty() || projectTitle.trim().isEmpty()) {
                log.warn("Invalid input: userId or projectTitle is null or empty");
                throw new IllegalArgumentException("User ID and project title must not be null or empty");
            }

            // Validate user exists
            Map<String, Object> userDetails = keycloakService.getUserDetailsById(userId);
            if (userDetails == null || userDetails.isEmpty()) {
                log.warn("User not found with ID: {}", userId);
                throw new RuntimeException("User not found in Keycloak");
            }

            // Get current project titles
            List<String> projectTitles = new ArrayList<>();
            if (userDetails.containsKey("projectTitles")) {
                Object projectsObj = userDetails.get("projectTitles");
                if (projectsObj instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<String> existingProjects = (List<String>) projectsObj;
                    projectTitles = new ArrayList<>(existingProjects != null ? existingProjects : Collections.emptyList());
                } else {
                    log.warn("projectTitles is not a List for userId {}, initializing as empty", userId);
                }
            } else {
                log.info("No projectTitles attribute found for userId {}, initializing as empty", userId);
            }

            // Log current projects
            log.debug("Before removal - projectTitles for userId {}: {}", userId, projectTitles);

            // Check if project exists and remove it
            String trimmedProjectTitle = projectTitle.trim();
            if (!projectTitles.contains(trimmedProjectTitle)) {
                log.info("Project '{}' not assigned to userId {}. No changes needed.", trimmedProjectTitle, userId);
                return convertToDTO(userDetails);
            }

            boolean removed = projectTitles.remove(trimmedProjectTitle);
            if (!removed) {
                log.warn("Failed to remove project '{}' from projectTitles for userId {}", trimmedProjectTitle, userId);
            }

            // Log projects after removal
            log.debug("After removal - projectTitles for userId {}: {}", userId, projectTitles);

            // Create a defensive copy with a new ArrayList to ensure Keycloak receives a properly serializable list
            List<String> updatedProjectsList = new ArrayList<>(projectTitles);

            // Update projects in Keycloak
            keycloakService.updateUserProjects(userId, updatedProjectsList);

            // Force cache refresh by re-fetching user
            Map<String, Object> updatedDetails = keycloakService.getUserDetailsById(userId);

            log.info("Successfully deassigned project '{}' for userId {}. Remaining projects: {}",
                    trimmedProjectTitle, userId, updatedProjectsList);

            return convertToDTO(updatedDetails);
        } catch (Exception e) {
            log.error("Error deassigning project '{}' for userId {}: {}", projectTitle, userId, e.getMessage(), e);
            throw new RuntimeException("Failed to deassign project: " + e.getMessage(), e);
        }
    }

    public UserDTO assignProject(String userId, String projectTitle) {
        log.info("Assigning project '{}' for user with ID: {}", projectTitle, userId);
        try {
            // Validate inputs
            if (userId == null || projectTitle == null || userId.trim().isEmpty() || projectTitle.trim().isEmpty()) {
                log.warn("Invalid input: userId or projectTitle is null or empty");
                throw new IllegalArgumentException("User ID and project title must not be null or empty");
            }

            // Validate user exists
            Map<String, Object> userDetails = keycloakService.getUserDetailsById(userId);
            if (userDetails == null || userDetails.isEmpty()) {
                log.warn("User not found with ID: {}", userId);
                throw new RuntimeException("User not found in Keycloak");
            }

            // Get current project titles
            List<String> projectTitles = new ArrayList<>();
            if (userDetails.containsKey("projectTitles")) {
                Object projectsObj = userDetails.get("projectTitles");
                if (projectsObj instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<String> existingProjects = (List<String>) projectsObj;
                    projectTitles = new ArrayList<>(existingProjects != null ? existingProjects : Collections.emptyList());
                } else {
                    log.warn("projectTitles is not a List for userId {}, initializing as empty", userId);
                }
            } else {
                log.info("No projectTitles attribute found for userId {}, initializing as empty", userId);
            }

            log.debug("Before assignment - projectTitles for userId {}: {}", userId, projectTitles);

            // Add project if not already assigned
            String trimmedProjectTitle = projectTitle.trim();
            if (projectTitles.contains(trimmedProjectTitle)) {
                log.info("Project '{}' already assigned to userId {}. No changes needed.", trimmedProjectTitle, userId);
                return convertToDTO(userDetails);
            }
            projectTitles.add(trimmedProjectTitle);

            log.debug("After assignment - projectTitles for userId {}: {}", userId, projectTitles);

            // Update projects in Keycloak
            keycloakService.updateUserProjects(userId, new ArrayList<>(projectTitles));

            // Verify update
            Map<String, Object> updatedDetails = keycloakService.getUserDetailsById(userId);
            List<String> updatedProjects;
            if (updatedDetails.containsKey("projectTitles")) {
                Object updatedProjectsObj = updatedDetails.get("projectTitles");
                if (updatedProjectsObj instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<String> projectsList = (List<String>) updatedProjectsObj;
                    updatedProjects = new ArrayList<>(projectsList != null ? projectsList : Collections.emptyList());
                } else {
                    updatedProjects = Collections.emptyList();
                    log.warn("Updated projectTitles is not a List for userId {}, got: {}", userId, updatedProjectsObj);
                }
            } else {
                updatedProjects = Collections.emptyList();
                log.warn("No projectTitles attribute after update for userId {}. Expected: {}", userId, projectTitles);
            }

            log.debug("Verified projectTitles after update for userId {}: {}", userId, updatedProjects);

            if (!updatedProjects.equals(projectTitles)) {
                log.error("Project titles update failed for userId {}: expected {}, got {}",
                        userId, projectTitles, updatedProjects);
                throw new RuntimeException("Failed to persist project titles update in Keycloak");
            }

            log.info("Successfully assigned project '{}' for userId {}. Projects: {}",
                    trimmedProjectTitle, userId, updatedProjects);

            return convertToDTO(updatedDetails);
        } catch (Exception e) {
            log.error("Error assigning project '{}' for userId {}: {}", projectTitle, userId, e.getMessage(), e);
            throw new RuntimeException("Failed to assign project: " + e.getMessage(), e);
        }
    }


    public void migrateExistingUsersHireDate() {
        log.info("Initiating hireDate migration for existing users");
        try {
            keycloakService.migrateExistingUsersWithHireDate();
            log.info("HireDate migration completed successfully");
        } catch (Exception e) {
            log.error("HireDate migration failed: {}", e.getMessage());
            throw new RuntimeException("Failed to migrate user hireDates", e);
        }
    }
}