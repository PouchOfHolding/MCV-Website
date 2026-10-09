package pt.ualg.mcv.family.dtos;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FamilyMemberRetrieveDto {
    
    private UUID uuid;

    private String firstName;

    private String lastName;

    private Instant createdAt;
    
    private Instant updatedAt;

    private List<FamilyMemberPhotoRetrieveDto> photos = new ArrayList<>();
}
