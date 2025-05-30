package company.Leave_Management_service.DTO;

import company.Leave_Management_service.Models.LeaveType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
@Data
public class LeaveRequestDto {
    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    @NotNull(message = "Type is required")
    private LeaveType type;

    @NotBlank(message = "Reason is required")
    private String reason;
}