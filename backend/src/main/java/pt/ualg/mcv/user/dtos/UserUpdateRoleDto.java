package pt.ualg.mcv.user.dtos;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import pt.ualg.mcv.user.UserRole;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserUpdateRoleDto
{
    @NotNull(message = "Role is required")
    private UserRole role;
}