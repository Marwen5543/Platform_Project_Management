package company.Leave_Management_service.Services;

import company.Leave_Management_service.DTO.LeaveRequestDto;
import company.Leave_Management_service.DTO.UserDTO;
import company.Leave_Management_service.Models.LeaveRequest;
import company.Leave_Management_service.Models.LeaveStatus;
import company.Leave_Management_service.Models.LeaveType;
import company.Leave_Management_service.Repository.LeaveRequestRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class LeaveService {
    @Autowired
    private LeaveRequestRepository repository;

    @Autowired
    private UserClient userClient;

    @Autowired
    private RoleHierarchy roleHierarchy;

    public LeaveRequest createLeaveRequest(LeaveRequestDto dto) {
        String userId = getCurrentUserId();
        log.info("Creating leave request for user: {}", userId);

        UserDTO user = fetchUserDetails(userId);
        if (user == null || user.getUsername() == null) {
            log.error("Failed to fetch valid user details for ID: {}", userId);
            throw new RuntimeException("User not found or username missing in user-management-service");
        }

        LeaveRequest leave = new LeaveRequest();
        leave.setEmployeeId(userId);
        leave.setUsername(user.getUsername());
        leave.setStartDate(dto.getStartDate());
        leave.setEndDate(dto.getEndDate());
        leave.setType(dto.getType());
        leave.setStatus(LeaveStatus.PENDING);
        leave.setReason(dto.getReason());
        leave.setCreatedAt(LocalDateTime.now());
        return repository.save(leave);
    }

    public List<LeaveRequest> getLeaveHistory() {
        String userId = getCurrentUserId();
        log.info("Fetching leave history for user: {}", userId);

        List<LeaveRequest> requests;
        if (hasRole("ROLE_HR")) {
            log.debug("User has HR role, returning all leave requests");
            requests = repository.findAll();
        } else {
            log.debug("User does not have HR role, returning own leave requests");
            requests = repository.findByEmployeeId(userId);
        }

        log.info("Processing {} leave requests", requests.size());
        populateUsernames(requests);
        log.debug("Final leave requests: {}", requests);
        return requests;
    }

    public List<LeaveRequest> getTeamLeaves() {
        String userId = getCurrentUserId();
        log.info("Fetching team leaves for manager: {}", userId);

        UserDTO manager = fetchUserDetails(userId);
        if (manager == null || manager.getDepartmentId() == null) {
            log.error("Manager user or department not found for ID: {}", userId);
            return Collections.emptyList();
        }

        Long departmentId = manager.getDepartmentId();
        List<UserDTO> departmentUsers;
        try {
            departmentUsers = userClient.getAllUsers().stream()
                    .filter(user -> departmentId.equals(user.getDepartmentId()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error fetching department users for department ID: {}", departmentId, e);
            return repository.findByEmployeeId(userId);
        }

        List<String> employeeIds = departmentUsers.stream()
                .map(UserDTO::getUserId)
                .collect(Collectors.toList());

        if (employeeIds.isEmpty()) {
            log.warn("No employees found in department: {}", departmentId);
            return Collections.emptyList();
        }

        List<LeaveRequest> requests = repository.findByEmployeeIdIn(employeeIds);
        populateUsernames(requests);
        return requests;
    }

    public LeaveRequest updateLeaveStatus(UUID id, LeaveStatus status) {
        LeaveRequest leave = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Leave not found"));
        leave.setStatus(status);
        leave.setUpdatedAt(LocalDateTime.now());
        return repository.save(leave);
    }

    private String getCurrentUserId() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private boolean hasRole(String role) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        var authorities = roleHierarchy.getReachableGrantedAuthorities(authentication.getAuthorities());
        return authorities.stream().anyMatch(a -> a.getAuthority().equals(role));
    }

    private void populateUsernames(List<LeaveRequest> requests) {
        if (requests.isEmpty()) {
            log.debug("No leave requests to process for username population");
            return;
        }

        // Collect unique employee IDs
        Set<String> employeeIds = requests.stream()
                .filter(request -> request.getUsername() == null || request.getUsername().startsWith("Utilisateur"))
                .map(LeaveRequest::getEmployeeId)
                .collect(Collectors.toSet());

        if (employeeIds.isEmpty()) {
            log.debug("All leave requests have valid usernames");
            return;
        }

        log.info("Fetching usernames for {} employee IDs", employeeIds.size());
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
                            (existing, replacement) -> existing // Keep first username in case of duplicates
                    ));

            // Update usernames
            for (LeaveRequest request : requests) {
                if (request.getUsername() == null || request.getUsername().startsWith("Utilisateur")) {
                    String username = usernameMap.get(request.getEmployeeId());
                    if (username != null) {
                        request.setUsername(username);
                        log.info("Populated username for leave request ID: {} to {}", request.getId(), username);
                    } else {
                        request.setUsername("Unknown User");
                        log.warn("No username found for employeeId: {}", request.getEmployeeId());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error fetching usernames from user-management-service: {}", e.getMessage(), e);
            for (LeaveRequest request : requests) {
                if (request.getUsername() == null || request.getUsername().startsWith("Utilisateur")) {
                    request.setUsername("Unknown User");
                    log.warn("Set username to 'Unknown User' for leave request ID: {} due to error", request.getId());
                }
            }
        }
    }

    private UserDTO fetchUserDetails(String userId) {
        try {
            log.info("Fetching user details for ID: {}", userId);
            UserDTO user = userClient.getUserById(userId);
            if (user == null || user.getUsername() == null) {
                log.warn("UserClient returned null or no username for ID: {}", userId);
            }
            return user;
        } catch (Exception e) {
            log.error("Error fetching user details for ID: {}", userId, e);
            return null;
        }
    }
}