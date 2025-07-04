package org.example.leavemanagement;

import company.Leave_Management_service.Controller.LeaveController;
import company.Leave_Management_service.DTO.LeaveRequestDto;
import company.Leave_Management_service.Models.LeaveRequest;
import company.Leave_Management_service.Models.LeaveStatus;
import company.Leave_Management_service.Services.LeaveService;
import company.Leave_Management_service.Services.LeaveRequestMigrationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.web.client.RestClientException;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeaveControllerTest {

    @Mock
    private LeaveService leaveService;

    @Mock
    private LeaveRequestMigrationService migrationService;

    @InjectMocks
    private LeaveController leaveController;

    // Tests for POST /api/leaves/request
    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void requestLeave_Success() {
        LeaveRequestDto dto = new LeaveRequestDto();
        LeaveRequest createdRequest = new LeaveRequest();
        when(leaveService.createLeaveRequest(dto)).thenReturn(createdRequest);

        ResponseEntity<?> response = leaveController.requestLeave(dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(createdRequest, response.getBody());
        verify(leaveService).createLeaveRequest(dto);
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void requestLeave_InvalidInput() {
        LeaveRequestDto dto = new LeaveRequestDto();
        when(leaveService.createLeaveRequest(dto)).thenThrow(new RuntimeException("Invalid input"));

        ResponseEntity<?> response = leaveController.requestLeave(dto);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Invalid input", response.getBody());
        verify(leaveService).createLeaveRequest(dto);
    }



    // Tests for GET /api/leaves/history
    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void getLeaveHistory_Success() {
        List<LeaveRequest> history = Arrays.asList(new LeaveRequest(), new LeaveRequest());
        when(leaveService.getLeaveHistory()).thenReturn(history);

        ResponseEntity<?> response = leaveController.getLeaveHistory();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(history, response.getBody());
        verify(leaveService).getLeaveHistory();
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void getLeaveHistory_ServiceError() {
        when(leaveService.getLeaveHistory()).thenThrow(new RuntimeException("Service error"));

        ResponseEntity<?> response = leaveController.getLeaveHistory();

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Service error", response.getBody());
        verify(leaveService).getLeaveHistory();
    }

    // Tests for GET /api/leaves/team
    @Test
    @WithMockUser(roles = "MANAGER")
    void getTeamLeaves_Success() {
        List<LeaveRequest> teamLeaves = Arrays.asList(new LeaveRequest(), new LeaveRequest());
        when(leaveService.getTeamLeaves()).thenReturn(teamLeaves);

        ResponseEntity<?> response = leaveController.getTeamLeaves();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(teamLeaves, response.getBody());
        verify(leaveService).getTeamLeaves();
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void getTeamLeaves_UserServiceUnavailable() {
        when(leaveService.getTeamLeaves()).thenThrow(new RestClientException("User service down"));

        ResponseEntity<?> response = leaveController.getTeamLeaves();

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals("Failed to connect to user service", response.getBody());
        verify(leaveService).getTeamLeaves();
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void getTeamLeaves_ServiceError() {
        when(leaveService.getTeamLeaves()).thenThrow(new RuntimeException("Service error"));

        ResponseEntity<?> response = leaveController.getTeamLeaves();

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Service error", response.getBody());
        verify(leaveService).getTeamLeaves();
    }


    // Tests for PUT /api/leaves/{id}/status
    @Test
    @WithMockUser(roles = "MANAGER")
    void updateLeaveStatus_Success() {
        UUID leaveId = UUID.randomUUID();
        Map<String, String> statusMap = Map.of("status", "APPROVED");
        LeaveRequest updatedLeave = new LeaveRequest();
        when(leaveService.updateLeaveStatus(leaveId, LeaveStatus.APPROVED)).thenReturn(updatedLeave);

        ResponseEntity<?> response = leaveController.updateLeaveStatus(leaveId, statusMap);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(updatedLeave, response.getBody());
        verify(leaveService).updateLeaveStatus(leaveId, LeaveStatus.APPROVED);
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void updateLeaveStatus_InvalidStatus() {
        UUID leaveId = UUID.randomUUID();
        Map<String, String> statusMap = Map.of("status", "INVALID_STATUS");

        ResponseEntity<?> response = leaveController.updateLeaveStatus(leaveId, statusMap);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("Invalid status value"));
        verify(leaveService, never()).updateLeaveStatus(any(), any());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void updateLeaveStatus_LeaveNotFound() {
        UUID leaveId = UUID.randomUUID();
        Map<String, String> statusMap = Map.of("status", "APPROVED");
        when(leaveService.updateLeaveStatus(leaveId, LeaveStatus.APPROVED))
                .thenThrow(new RuntimeException("Leave not found"));

        ResponseEntity<?> response = leaveController.updateLeaveStatus(leaveId, statusMap);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Leave not found", response.getBody());
        verify(leaveService).updateLeaveStatus(leaveId, LeaveStatus.APPROVED);
    }


    // Tests for POST /api/leaves/migrate-usernames
    @Test
    @WithMockUser(roles = "ADMIN")
    void migrateUsernames_Success() {
        doNothing().when(migrationService).populateUsernames();

        ResponseEntity<String> response = leaveController.migrateUsernames();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Username migration completed successfully", response.getBody());
        verify(migrationService).populateUsernames();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void migrateUsernames_Error() {
        doThrow(new RuntimeException("Migration error")).when(migrationService).populateUsernames();

        ResponseEntity<String> response = leaveController.migrateUsernames();

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().contains("Failed to complete username migration"));
        verify(migrationService).populateUsernames();
    }
}
