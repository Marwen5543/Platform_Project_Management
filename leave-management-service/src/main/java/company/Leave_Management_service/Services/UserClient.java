package company.Leave_Management_service.Services;

import company.Leave_Management_service.Config.FeignConfig;
import company.Leave_Management_service.DTO.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "user-management-service", configuration = FeignConfig.class)
public interface UserClient {
    @GetMapping("/api/users/{userId}")
    UserDTO getUserById(@PathVariable("userId") String userId);

    @GetMapping("/api/users/details/{username}")
    UserDTO getUserByUsername(@PathVariable("username") String username);

    @GetMapping("/api/users")
    List<UserDTO> getAllUsers();
}
