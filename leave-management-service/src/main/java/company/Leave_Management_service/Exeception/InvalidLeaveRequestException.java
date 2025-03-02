package company.Leave_Management_service.Exeception;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

public class InvalidLeaveRequestException extends RuntimeException {
    public InvalidLeaveRequestException(String message) {
        super(message);
    }
}