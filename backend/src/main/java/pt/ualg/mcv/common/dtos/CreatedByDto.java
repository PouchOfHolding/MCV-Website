package pt.ualg.mcv.common.dtos;

import java.util.UUID;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import pt.ualg.mcv.user.UserRole;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreatedByDto {
    private UUID uuid;
    
    @Email
    private String email;

    @NotBlank
    @Size(min = 1, max = 64)
    private String firstName;

    @NotBlank
    @Size(min = 1, max = 64)
    private String lastName;

    private UserRole role;
}
