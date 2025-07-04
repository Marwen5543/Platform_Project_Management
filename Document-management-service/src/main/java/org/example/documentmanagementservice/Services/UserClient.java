package org.example.documentmanagementservice.Services;

import org.example.documentmanagementservice.config.FeignConfig;
import org.example.documentmanagementservice.DTO.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-management-service", configuration = FeignConfig.class)
public interface UserClient {
    @GetMapping("/api/users/{userId}")
    UserDTO getUserById(@PathVariable("userId") String userId);

    @GetMapping("/api/users/details/{username}")
    UserDTO getUserByUsername(@PathVariable("username") String username);
}