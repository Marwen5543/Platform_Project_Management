package company.Leave_Management_service.DTO;

import company.Leave_Management_service.Models.LeaveRequest;
import lombok.Data;

import java.time.LocalDate;
@Data
public class LeaveRequestDTO {
    private Long requestId;
    private Long employeeId;
    private LocalDate startDate;
    private LocalDate endDate;
    private LeaveRequest.LeaveType leaveType;
    private LeaveRequest.LeaveStatus status;
    private String comments;
    private Long approverEmployeeId;
    private LocalDate submissionDate;
    private LocalDate approvalDate;
}