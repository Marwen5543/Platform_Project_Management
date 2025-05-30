package company.Leave_Management_service.Controller;

import company.Leave_Management_service.DTO.LeaveRequestDto;
import company.Leave_Management_service.Models.LeaveRequest;
import company.Leave_Management_service.Models.LeaveStatus;
import company.Leave_Management_service.Services.LeaveService;
import company.Leave_Management_service.Services.LeaveRequestMigrationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/leaves")
public class LeaveController {
    @Autowired
    private LeaveService leaveService;

    @Autowired
    private LeaveRequestMigrationService migrationService;

    @PostMapping("/request")
    @PreAuthorize("hasAuthority('ROLE_EMPLOYEE')")
    public ResponseEntity<?> requestLeave(@RequestBody LeaveRequestDto dto) {
        try {
            LeaveRequest leaveRequest = leaveService.createLeaveRequest(dto);
            return ResponseEntity.ok(leaveRequest);
        } catch (RuntimeException e) {
            log.error("Error creating leave request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error creating leave request: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to create leave request");
        }
    }

    @GetMapping("/history")
    @PreAuthorize("hasAnyAuthority('ROLE_EMPLOYEE', 'ROLE_MANAGER', 'ROLE_HR')")
    public ResponseEntity<?> getLeaveHistory() {
        try {
            List<LeaveRequest> leaveHistory = leaveService.getLeaveHistory();
            return ResponseEntity.ok(leaveHistory);
        } catch (RuntimeException e) {
            log.error("Error fetching leave history: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error fetching leave history: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to fetch leave history");
        }
    }

    @GetMapping("/team")
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_HR')")
    public ResponseEntity<?> getTeamLeaves() {
        try {
            List<LeaveRequest> teamLeaves = leaveService.getTeamLeaves();
            return ResponseEntity.ok(teamLeaves);
        } catch (RestClientException e) {
            log.error("Error connecting to user service: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Failed to connect to user service");
        } catch (RuntimeException e) {
            log.error("Error fetching team leaves: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error fetching team leaves: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to fetch team leaves");
        }
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_HR')")
    public ResponseEntity<?> updateLeaveStatus(@PathVariable UUID id, @RequestBody Map<String, String> statusMap) {
        try {
            LeaveStatus status = LeaveStatus.valueOf(statusMap.get("status"));
            LeaveRequest updatedLeave = leaveService.updateLeaveStatus(id, status);
            return ResponseEntity.ok(updatedLeave);
        } catch (IllegalArgumentException e) {
            log.error("Invalid status value: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    "Invalid status value. Valid values are: " +
                            String.join(", ", Arrays.stream(LeaveStatus.values())
                                    .map(Enum::name)
                                    .toList()));
        } catch (RuntimeException e) {
            log.error("Error updating leave status: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error updating leave status: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to update leave status");
        }
    }

    @PostMapping("/migrate-usernames")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<String> migrateUsernames() {
        try {
            migrationService.populateUsernames();
            log.info("Username migration completed successfully");
            return ResponseEntity.ok("Username migration completed successfully");
        } catch (Exception e) {
            log.error("Error during username migration: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to complete username migration: " + e.getMessage());
        }
    }
}