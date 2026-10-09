package pt.ualg.mcv.family.dtos;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import pt.ualg.mcv.family.FamilyMemberRole;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FamilyAddMemberDto {
    @NotNull(message = "Role is required")
    private FamilyMemberRole role;
}
