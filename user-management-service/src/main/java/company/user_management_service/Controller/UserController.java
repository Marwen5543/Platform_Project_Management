package company.user_management_service.Controller;

import company.user_management_service.DTO.CreateUserRequest;
import company.user_management_service.DTO.LoginRequest;
import company.user_management_service.DTO.LoginResponse;
import company.user_management_service.DTO.UserDTO;
import company.user_management_service.Service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;
import org.keycloak.representations.AccessToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/users")
@Slf4j
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        log.info("Admin/SuperAdmin fetching all users");
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/details/{username}")
    public Map<String, Object> getUserDetails(@PathVariable String username) {
        return userService.getUserFullDetails(username);
    }

    @GetMapping("/myRole")
    public ResponseEntity<Map<String, Object>> getCurrentUserRoles() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() ||
                authentication instanceof AnonymousAuthenticationToken) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not authenticated");
        }

        String username = authentication.getName();
        log.info("Fetching roles for authenticated user: {}", username);

        Map<String, Object> roles = userService.getCurrentUserRoles();
        return ResponseEntity.ok(roles);
    }

    @GetMapping("/me")
    public ResponseEntity<UserDTO> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not authenticated");
        }
        log.info("Authentication object: {}", authentication);
        String username = authentication.getName();
        if (username == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Username is null in authentication");
        }
        log.info("Username from authentication: {}", username);
        UserDTO user = userService.getUserByUsername(username);
        return ResponseEntity.ok(user);
    }

    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@RequestBody CreateUserRequest request) {
        log.info("Received registration request: username={}, email={}, role={}, firstName={}, lastName={}",
                request.getUsername(), request.getEmail(), request.getRole(), request.getFirstName(), request.getLastName());
        try {
            userService.createUser(request);
            return ResponseEntity.ok("User created successfully");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        log.info("Received login request for username: {}", request.getUsername());
        LoginResponse response = userService.login(request);
        log.info("Successfully authenticated user: {}", request.getUsername());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserDTO> getUser(@PathVariable String userId) {
        log.info("Received request to fetch user with ID: {}", userId);
        UserDTO user = userService.getUserById(userId);
        log.info("Successfully retrieved user with ID: {}", userId);
        return ResponseEntity.ok(user);
    }

    @PutMapping("/{userId}/role")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<String> changeUserRole(
            @PathVariable String userId,
            @RequestBody Map<String, String> request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        log.info("Authenticated user: {}, authorities: {}", auth.getName(), auth.getAuthorities());

        String newRole = request.get("role");
        if (newRole == null) {
            log.warn("Role not provided in request");
            return ResponseEntity.badRequest().body("Role is required");
        }

        try {
            userService.changeUserRole(userId, newRole);
            return ResponseEntity.ok("Role updated successfully");
        } catch (IllegalArgumentException e) {
            log.warn("Invalid role request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (SecurityException e) {
            log.warn("Security violation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (RuntimeException e) {
            log.error("Role update failed for userId {}: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to update role: " + e.getMessage());
        }
    }

    @PostMapping("/change-password")
    public ResponseEntity<String> changePassword(
            @RequestBody Map<String, String> request,
            Authentication authentication) {

        String currentPassword = request.get("currentPassword");
        String newPassword = request.get("newPassword");

        if (currentPassword == null || newPassword == null) {
            return ResponseEntity.badRequest().body("Both current and new password are required");
        }

        try {
            String username = authentication.getName();
            userService.changePassword(username, currentPassword, newPassword);
            return ResponseEntity.ok("Password changed successfully");
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/profile/{userId}")
    public ResponseEntity<UserDTO> updateProfile(
            @PathVariable String userId,
            @RequestBody UserDTO updateRequest,
            Authentication authentication) {
        log.info("Received request to update profile for userId: {}", userId);

        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("Unauthenticated attempt to update profile for userId: {}", userId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }

        Jwt jwt = (Jwt) authentication.getPrincipal();
        String authenticatedUserId = jwt.getSubject();

        log.info("Authenticated User ID (sub): {}", authenticatedUserId);
        log.info("Requested User ID: {}", userId);

        if (!authenticatedUserId.equals(userId)) {
            log.warn("User {} attempted to update profile of userId: {}", authenticatedUserId, userId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }

        try {
            UserDTO updatedUser = userService.updateProfile(userId, updateRequest);
            log.info("Successfully updated profile for userId: {}", userId);
            return ResponseEntity.ok(updatedUser);
        } catch (RuntimeException e) {
            log.error("Failed to update profile for userId {}: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/username/{username}/name-and-role")
    public ResponseEntity<Map<String, String>> getUserNameAndRoleByUsername(@PathVariable String username) {
        log.info("Received request to fetch name and role for user with username: {}", username);
        try {
            Map<String, String> nameAndRole = userService.getUserNameAndRoleByUsername(username);
            log.info("Successfully retrieved name and role for user with username: {}", username);
            return ResponseEntity.ok(nameAndRole);
        } catch (Exception e) {
            log.error("Failed to fetch name and role for user with username {}: {}", username, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/{userId}/name-and-role")
    public ResponseEntity<Map<String, String>> getUserNameAndRole(@PathVariable String userId) {
        log.info("Received request to fetch name and role for user with ID: {}", userId);
        try {
            Map<String, String> nameAndRole = userService.getUserNameAndRole(userId);
            log.info("Successfully retrieved name and role for user with ID: {}", userId);
            return ResponseEntity.ok(nameAndRole);
        } catch (Exception e) {
            log.error("Failed to fetch name and role for user with ID {}: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/me/name-and-role")
    public ResponseEntity<Map<String, String>> getCurrentUserNameAndRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() ||
                authentication instanceof AnonymousAuthenticationToken) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not authenticated");
        }

        String username = authentication.getName();
        log.info("Fetching name and role for current authenticated user: {}", username);

        try {
            Map<String, String> nameAndRole = userService.getUserNameAndRoleByUsername(username);
            return ResponseEntity.ok(nameAndRole);
        } catch (Exception e) {
            log.error("Failed to fetch name and role for current user {}: {}", username, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<String> deleteUser(@PathVariable String userId) {
        log.info("Received request to delete user with ID: {}", userId);
        try {
            UserDTO user = userService.getUserById(userId);
            userService.deleteUser(user.getUsername());
            log.info("Successfully deleted user with ID: {}", userId);
            return ResponseEntity.ok("User deleted successfully");
        } catch (RuntimeException e) {
            log.error("Failed to delete user with ID {}: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to delete user: " + e.getMessage());
        }
    }

    @DeleteMapping("/{userId}/project/{projectTitle}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<UserDTO> deassignProject(
            @PathVariable String userId,
            @PathVariable String projectTitle) {
        log.info("Received request to deassign project '{}' for user with ID: {}", projectTitle, userId);
        try {
            // Validate inputs
            if (userId == null || projectTitle == null || userId.trim().isEmpty() || projectTitle.trim().isEmpty()) {
                log.warn("Invalid input: userId or projectTitle is null or empty");
                throw new IllegalArgumentException("User ID and project title must not be null or empty");
            }

            // Call UserService to deassign project
            log.debug("Calling UserService.deassignProject for userId: {}, projectTitle: {}", userId, projectTitle);
            UserDTO updatedUser = userService.deassignProject(userId, projectTitle);

            // Log updated project titles
            log.debug("Updated projectTitles for userId {}: {}", userId, updatedUser.getProjectTitles());

            log.info("Successfully deassigned project '{}' for user with ID: {}", projectTitle, userId);
            return ResponseEntity.ok(updatedUser);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid project deassignment request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(null);
        } catch (RuntimeException e) {
            log.error("Failed to deassign project '{}' for userId {}: {}", projectTitle, userId, e.getMessage(), e);
            if (e.getMessage().contains("User not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping("/{userId}/project/{projectTitle}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<UserDTO> assignProject(
            @PathVariable String userId,
            @PathVariable String projectTitle) {
        log.info("Received request to assign project '{}' for user with ID: {}", projectTitle, userId);
        try {
            // Validate inputs
            if (userId == null || projectTitle == null || userId.trim().isEmpty() || projectTitle.trim().isEmpty()) {
                log.warn("Invalid input: userId or projectTitle is null or empty");
                throw new IllegalArgumentException("User ID and project title must not be null or empty");
            }

            // Call UserService to assign project
            log.debug("Calling UserService.assignProject for userId: {}, projectTitle: {}", userId, projectTitle);
            UserDTO updatedUser = userService.assignProject(userId, projectTitle);

            log.debug("Updated projectTitles for userId {}: {}", userId, updatedUser.getProjectTitles());
            log.info("Successfully assigned project '{}' for user with ID: {}", projectTitle, userId);
            return ResponseEntity.ok(updatedUser);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid project assignment request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(null);
        } catch (RuntimeException e) {
            log.error("Failed to assign project '{}' for userId {}: {}", projectTitle, userId, e.getMessage(), e);
            if (e.getMessage().contains("User not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping("/migrate-hiredates")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<String> migrateUserHireDates() {
        try {
            log.info("Migration endpoint called by user: {}", SecurityContextHolder.getContext().getAuthentication().getName());
            userService.migrateExistingUsersHireDate();
            return ResponseEntity.ok("HireDate migration completed successfully");
        } catch (Exception e) {
            log.error("Migration endpoint failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Migration failed: " + e.getMessage());
        }
    }

    @GetMapping("/test-hiredate/{username}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> testUserHireDate(@PathVariable String username) {
        try {
            log.info("Testing hireDate for username: {}", username);

            // Get raw user details from Keycloak via UserService
            Map<String, Object> userDetails = userService.getUserFullDetails(username);

            // Also get the UserDTO for comparison
            UserDTO userDto = userService.getUserByUsername(username);

            Map<String, Object> response = new HashMap<>();
            response.put("username", username);
            response.put("hireDate_from_details", userDetails.get("hireDate"));
            response.put("hireDate_from_dto", userDto.getHireDate());
            response.put("hireDateType_details", userDetails.get("hireDate") != null ?
                    userDetails.get("hireDate").getClass().getSimpleName() : "null");
            response.put("hireDateType_dto", userDto.getHireDate() != null ?
                    userDto.getHireDate().getClass().getSimpleName() : "null");
            response.put("allUserDetails", userDetails);
            response.put("userDTO", userDto);

            log.info("Test result for {}: details_hireDate = {}, dto_hireDate = {}",
                    username, userDetails.get("hireDate"), userDto.getHireDate());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Test failed for username {}: {}", username, e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            errorResponse.put("username", username);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PostMapping("/fix-user-hiredate/{username}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> fixUserHireDate(@PathVariable String username) {
        try {
            log.info("Fixing hireDate for username: {}", username);

            // Get current user details
            UserDTO currentUser = userService.getUserByUsername(username);

            // Check if hireDate is missing or null
            if (currentUser.getHireDate() == null || currentUser.getHireDate().trim().isEmpty()) {
                // Set hireDate to today's date
                String todaysDate = LocalDate.now().toString();

                // Update the user profile with today's date
                UserDTO updateRequest = new UserDTO();
                updateRequest.setHireDate(todaysDate);

                UserDTO updatedUser = userService.updateProfile(currentUser.getUserId(), updateRequest);

                Map<String, Object> response = new HashMap<>();
                response.put("message", "HireDate fixed successfully");
                response.put("username", username);
                response.put("oldHireDate", currentUser.getHireDate());
                response.put("newHireDate", updatedUser.getHireDate());
                response.put("updatedUser", updatedUser);

                log.info("Fixed hireDate for user {}: {} -> {}", username,
                        currentUser.getHireDate(), updatedUser.getHireDate());

                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("message", "User already has a valid hireDate");
                response.put("username", username);
                response.put("currentHireDate", currentUser.getHireDate());
                response.put("user", currentUser);

                return ResponseEntity.ok(response);
            }

        } catch (Exception e) {
            log.error("Failed to fix hireDate for username {}: {}", username, e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            errorResponse.put("username", username);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/debug-all-users-hiredates")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> debugAllUsersHireDates() {
        try {
            log.info("Debugging hireDate for all users");

            List<UserDTO> allUsers = userService.getAllUsers();

            Map<String, Object> response = new HashMap<>();
            List<Map<String, Object>> userSummaries = new ArrayList<>();

            int usersWithHireDate = 0;
            int usersWithoutHireDate = 0;

            for (UserDTO user : allUsers) {
                Map<String, Object> userSummary = new HashMap<>();
                userSummary.put("username", user.getUsername());
                userSummary.put("userId", user.getUserId());
                userSummary.put("hireDate", user.getHireDate());
                userSummary.put("hasValidHireDate", user.getHireDate() != null && !user.getHireDate().trim().isEmpty());

                if (user.getHireDate() != null && !user.getHireDate().trim().isEmpty()) {
                    usersWithHireDate++;
                } else {
                    usersWithoutHireDate++;
                }

                userSummaries.add(userSummary);
            }

            response.put("totalUsers", allUsers.size());
            response.put("usersWithHireDate", usersWithHireDate);
            response.put("usersWithoutHireDate", usersWithoutHireDate);
            response.put("users", userSummaries);

            log.info("Debug complete: {}/{} users have valid hireDates",
                    usersWithHireDate, allUsers.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to debug all users hireDates: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

}