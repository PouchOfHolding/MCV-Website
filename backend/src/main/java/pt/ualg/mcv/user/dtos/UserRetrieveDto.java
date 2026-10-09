package pt.ualg.mcv.user.dtos;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import pt.ualg.mcv.user.UserRole;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserRetrieveDto {
    private UUID uuid;
    private String email;
    private String firstName;
    private String lastName;
    private UserRole role;
    private Instant createdAt;
}
