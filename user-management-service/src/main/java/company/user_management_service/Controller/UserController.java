package company.user_management_service.Controller;

import company.user_management_service.DTO.CreateUserRequest;
import company.user_management_service.DTO.LoginRequest;
import company.user_management_service.DTO.LoginResponse;
import company.user_management_service.DTO.UserDTO;
import company.user_management_service.Service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@Slf4j
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@RequestBody CreateUserRequest request) {
        log.info("Received registration request: username={}, email={}, role={}",
                request.getUsername(), request.getEmail(), request.getRole());
        try {
            userService.createUser(request.getUsername(), request.getEmail(), request.getPassword());
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
    public ResponseEntity<UserDTO> getUser(@PathVariable Long userId) {
        log.info("Received request to fetch user with ID: {}", userId);
        UserDTO user = userService.getUserById(userId);
        log.info("Successfully retrieved user with ID: {}", userId);
        return ResponseEntity.ok(user);
    }


}