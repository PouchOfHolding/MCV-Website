package pt.ualg.mcv.family.dtos;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FamilyMemberPhotoRetrieveDto {
    
    private UUID uuid;

    private String filePath;

    private String thumbnailPath;

    private String mimeType;

    private Instant createdAt;
}
