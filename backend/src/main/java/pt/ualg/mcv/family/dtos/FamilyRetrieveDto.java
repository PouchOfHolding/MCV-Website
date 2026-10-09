package pt.ualg.mcv.family.dtos;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FamilyRetrieveDto {
    private UUID uuid;
    private Long year;
    private Instant createdAt;
    private Instant updatedAt;
}
