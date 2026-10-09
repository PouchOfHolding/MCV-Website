package pt.ualg.mcv.gallery.dtos;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GalleryPhotoRetrieveDto {
    
    private UUID uuid;

    private String filePath;

    private String thumbnailPath;

    private String mimeType;

    private Instant createdAt;

    private String title;
    
    private String description;
}
