package company.Leave_Management_service.DTO;

import company.Leave_Management_service.Models.LeaveRequest;
import lombok.Data;

@Data
public class UpdateLeaveStatusDTO {
    private LeaveRequest.LeaveStatus status;
    private Long approverEmployeeId;
}