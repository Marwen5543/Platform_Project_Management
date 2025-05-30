package company.Leave_Management_service.Controller;

import company.Leave_Management_service.Services.LeaveRequestMigrationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/admin")
public class AdminController {
    @Autowired
    private LeaveRequestMigrationService migrationService;

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