package company.Leave_Management_service.Controller;


import company.Leave_Management_service.DTO.CreateLeaveRequestDTO;
import company.Leave_Management_service.DTO.LeaveRequestDTO;
import company.Leave_Management_service.DTO.UpdateLeaveStatusDTO;
import company.Leave_Management_service.Services.LeaveRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leave-requests")
@RequiredArgsConstructor
public class LeaveRequestController {
    private final LeaveRequestService leaveRequestService;

    @PostMapping
    public ResponseEntity<LeaveRequestDTO> createLeaveRequest(@RequestBody CreateLeaveRequestDTO dto) {
        LeaveRequestDTO created = leaveRequestService.createLeaveRequest(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{requestId}")
    public ResponseEntity<LeaveRequestDTO> getLeaveRequestById(@PathVariable Long requestId) {
        return ResponseEntity.ok(leaveRequestService.getLeaveRequestById(requestId));
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<LeaveRequestDTO>> getLeaveRequestsByEmployeeId(@PathVariable Long employeeId) {
        return ResponseEntity.ok(leaveRequestService.getLeaveRequestsByEmployeeId(employeeId));
    }

    @GetMapping
    public ResponseEntity<List<LeaveRequestDTO>> getAllLeaveRequests() {
        return ResponseEntity.ok(leaveRequestService.getAllLeaveRequests());
    }

    @PutMapping("/{requestId}/status")
    public ResponseEntity<LeaveRequestDTO> updateLeaveRequestStatus(
            @PathVariable Long requestId,
            @RequestBody UpdateLeaveStatusDTO dto) {
        return ResponseEntity.ok(leaveRequestService.updateLeaveRequestStatus(requestId, dto));
    }
}