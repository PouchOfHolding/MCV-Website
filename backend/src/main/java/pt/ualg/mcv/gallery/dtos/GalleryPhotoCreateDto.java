package pt.ualg.mcv.gallery.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GalleryPhotoCreateDto {
    
    @NotBlank
    @Size(min = 1, max = 256)
    private String title;

    @NotBlank
    @Size(min = 1, max = 1000)
    private String description;
}
