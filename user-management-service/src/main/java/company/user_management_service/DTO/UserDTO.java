package company.user_management_service.DTO;

import company.user_management_service.Models.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {

    private Long userId;
    private String username;
    private String email;
    private User.UserRole role;
    private User.UserStatus status;
}
