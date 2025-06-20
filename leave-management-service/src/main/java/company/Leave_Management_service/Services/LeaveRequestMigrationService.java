package company.Leave_Management_service.Services;

import company.Leave_Management_service.DTO.UserDTO;
import company.Leave_Management_service.Models.LeaveRequest;
import company.Leave_Management_service.Repository.LeaveRequestRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class LeaveRequestMigrationService {
    @Autowired
    private LeaveRequestRepository repository;

    @Autowired
    private UserClient userClient;

    public void populateUsernames() {
        List<LeaveRequest> requests = repository.findAll();
        log.info("Migrating usernames for {} leave requests", requests.size());

        // Collect unique employee IDs
        Set<String> employeeIds = requests.stream()
                .filter(request -> request.getUsername() == null || request.getUsername().startsWith("Utilisateur"))
                .map(LeaveRequest::getEmployeeId)
                .collect(Collectors.toSet());

        if (employeeIds.isEmpty()) {
            log.info("No leave requests require username migration");
            return;
        }

        try {
            // Fetch all users in one request
            List<UserDTO> users = userClient.getAllUsers();
            log.debug("Fetched {} users from user-management-service", users.size());

            // Create a map of employeeId to username
            Map<String, String> usernameMap = users.stream()
                    .filter(user -> user.getUserId() != null && user.getUsername() != null)
                    .collect(Collectors.toMap(
                            UserDTO::getUserId,
                            UserDTO::getUsername,
                            (existing, replacement) -> existing
                    ));

            // Update and save requests
            for (LeaveRequest request : requests) {
                if (request.getUsername() == null || request.getUsername().startsWith("Utilisateur")) {
                    String username = usernameMap.get(request.getEmployeeId());
                    if (username != null) {
                        request.setUsername(username);
                        repository.save(request);
                        log.info("Updated username for leave request ID: {} to {}", request.getId(), username);
                    } else {
                        log.warn("No username found for employeeId: {}", request.getEmployeeId());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error during username migration: {}", e.getMessage(), e);
        }
    }
}