package company.user_management_service.Service;

import company.user_management_service.DTO.LoginRequest;
import company.user_management_service.DTO.LoginResponse;
import company.user_management_service.DTO.UserDTO;
import company.user_management_service.Models.User;
import company.user_management_service.Repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
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

    public void createUser(String username, String email, String password) {
        log.info("Creating user: username={}, email={}", username, email);

        // Check if username already exists
        if (userRepository.findByUsername(username).isPresent()) {
            log.warn("Username {} already exists", username);
            throw new IllegalArgumentException("Username '" + username + "' is already taken");
        }

        try {
            // Register in Keycloak
            keycloakService.createUser(username, email, password);

            // Save locally
            User localUser = new User();
            localUser.setUsername(username);
            localUser.setEmail(email);
            localUser.setPassword(passwordEncoder.encode(password));
            localUser.setRole(User.UserRole.EMPLOYEE.toString());
            localUser.setStatus(User.UserStatus.ACTIVE.toString());
            userRepository.save(localUser);
            log.info("User successfully saved in PostgreSQL: {}", username);
        } catch (Exception e) {
            log.error("Failed to create user in Keycloak: {}", e.getMessage());
            // Save locally even if Keycloak fails (only if not already saved)
            if (userRepository.findByUsername(username).isEmpty()) {
                User localUser = new User();
                localUser.setUsername(username);
                localUser.setEmail(email);
                localUser.setPassword(passwordEncoder.encode(password));
                localUser.setRole(User.UserRole.EMPLOYEE.toString());
                localUser.setStatus(User.UserStatus.ACTIVE.toString());
                userRepository.save(localUser);
                log.info("User saved in PostgreSQL despite Keycloak failure: {}", username);
            }
            throw new RuntimeException("Failed to register user in Keycloak", e);
        }
    }

    public LoginResponse login(LoginRequest request) {
        log.info("Processing login request for username: {}", request.getUsername());

        // Check local database
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> {
                    log.error("User not found with username: {}", request.getUsername());
                    return new UsernameNotFoundException("User not found: " + request.getUsername());
                });

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.error("Invalid password for username: {}", request.getUsername());
            throw new BadCredentialsException("Invalid password");
        }

        if (!user.getStatus().equals(User.UserStatus.ACTIVE.toString())) {
            log.error("User account is not active: {}", request.getUsername());
            throw new RuntimeException("Account is not active");
        }

        try {
            String token = keycloakService.getToken(request.getUsername(), request.getPassword());
            log.info("Successfully authenticated with Keycloak for user: {}", request.getUsername());
            return new LoginResponse(token, "Bearer", user.getUsername(), user.getRole());
        } catch (Exception e) {
            log.warn("Keycloak authentication failed: {}", e.getMessage());
            log.warn("Using mock token as fallback for user: {}", request.getUsername());
            String mockToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
            return new LoginResponse(mockToken, "Bearer", user.getUsername(), user.getRole());
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
