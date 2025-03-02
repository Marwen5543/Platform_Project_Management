package company.user_management_service.Service;

import company.user_management_service.DTO.LoginRequest;
import company.user_management_service.DTO.LoginResponse;
import company.user_management_service.DTO.UserDTO;
import company.user_management_service.Models.User;
import company.user_management_service.Repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
@Service
@Slf4j
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private KeycloakService keycloakService;

    @Value("${keycloak.realm}")
    private String realm;

    public void createUser(String username, String email, String password) {
        try {
            // Use the KeycloakService
            keycloakService.createUser(username, email, password);

            // Add user to local database
            User localUser = new User();
            localUser.setUsername(username);
            localUser.setEmail(email);
            localUser.setPassword(passwordEncoder.encode(password));
            localUser.setRole(User.UserRole.EMPLOYEE.toString());  // Convert enum to string
            localUser.setStatus(User.UserStatus.ACTIVE.toString()); // Convert enum to string

            // Convert the String back to UserRole and UserStatus enum
            localUser.setRole(User.UserRole.EMPLOYEE.toString());  // Convert enum to string
            localUser.setStatus(User.UserStatus.ACTIVE.toString()); // Convert enum to string

            userRepository.save(localUser);
            log.info("User successfully saved in PostgreSQL: {}", username);
        } catch (Exception e) {
            log.error("Failed to create user: {}", e.getMessage());
            // Store user in local DB even if Keycloak fails
            try {
                User localUser = new User();
                localUser.setUsername(username);
                localUser.setEmail(email);
                localUser.setPassword(passwordEncoder.encode(password));
                localUser.setRole(User.UserRole.EMPLOYEE.toString());  // Convert enum to string
                localUser.setStatus(User.UserStatus.ACTIVE.toString()); // Convert enum to string

                // Convert the String back to UserRole and UserStatus enum
                localUser.setRole(localUser.getRole());  // No need to call valueOf again
                localUser.setStatus(localUser.getStatus());  // No need to call valueOf again


                userRepository.save(localUser);
                log.info("User saved in PostgreSQL despite Keycloak failure: {}", username);
            } catch (Exception dbEx) {
                log.error("Failed to save user in PostgreSQL: {}", dbEx.getMessage());
            }
            throw e;
        }
    }

    public LoginResponse login(LoginRequest request) {
        log.info("Processing login request for username: {}", request.getUsername());

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> {
                    log.error("User not found with username: {}", request.getUsername());
                    return new RuntimeException("Invalid username or password");
                });

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.error("Invalid password for username: {}", request.getUsername());
            throw new RuntimeException("Invalid username or password");
        }

        if (!user.getStatus().equals(User.UserStatus.ACTIVE.toString())) {  // Compare as strings
            log.error("User account is not active: {}", request.getUsername());
            throw new RuntimeException("Account is not active");
        }

        try {
            String token = keycloakService.getToken(request.getUsername(), request.getPassword());
            return new LoginResponse(
                    token,
                    "Bearer",
                    user.getUsername(),
                    user.getRole().toString()  // Convert to string before sending
            );
        } catch (Exception e) {
            log.warn("Keycloak authentication failed: {}", e.getMessage());
            log.warn("Using mock token as fallback");
            String mockToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";

            return new LoginResponse(
                    mockToken,
                    "Bearer",
                    user.getUsername(),
                    user.getRole().toString()  // Convert to string before sending
            );
        }
    }

    public UserDTO getUserById(Long userId) {
        log.info("Fetching user with ID: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("User not found with ID: {}", userId);
                    return new RuntimeException("User not found");
                });
        log.debug("Retrieved user: {}", user.getUsername());
        return convertToDTO(user);
    }

    private UserDTO convertToDTO(User user) {
        log.debug("Converting User entity to DTO for user ID: {}", user.getUserId());
        UserDTO dto = new UserDTO();
        dto.setUserId(user.getUserId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setRole(User.UserRole.valueOf(user.getRole()));// Convert to String before setting
        dto.setStatus(User.UserStatus.valueOf(user.getStatus()));  // Convert to String before setting
        return dto;
    }
}
