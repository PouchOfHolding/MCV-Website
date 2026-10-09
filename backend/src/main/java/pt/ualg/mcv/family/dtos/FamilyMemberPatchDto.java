package pt.ualg.mcv.family.dtos;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FamilyMemberPatchDto {
    
    @Size(min = 1, max = 64)
    private String firstName;

    @Size(min = 1, max = 64)
    private String lastName;

    public void setFirstName(String firstName)
    {
        this.firstName = firstName != null ? firstName.trim() : null;
    }

    public void setLastName(String lastName)
    {
        this.lastName = lastName != null ? lastName.trim() : null;
    }
}
