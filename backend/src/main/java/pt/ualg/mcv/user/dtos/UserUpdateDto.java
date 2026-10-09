package pt.ualg.mcv.user.dtos;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserUpdateDto
{

    @Size(min = 1, max = 64)
    private String firstName;

    @Size(min = 1, max = 64)
    private String lastName;

    @Size(min = 1, max = 130)
    private String password;

    public void setFirstName(String firstName)
    {
        this.firstName = firstName != null ? firstName.trim() : null;
    }

    public void setLastName(String lastName)
    {
        this.lastName = lastName != null ? lastName.trim() : null;
    }

    public void setPassword(String password)
    {
        this.password = password != null ? password.trim() : null;
    }
}
