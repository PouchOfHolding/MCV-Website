package pt.ualg.mcv.gallery;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import pt.ualg.mcv.common.models.Photo;

@Entity
@Table(name = "gallery_photo")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class GalleryPhoto extends Photo {

    @Column(length = 256, nullable = false)
    private String title;

    @Column(length = 1000, nullable = false)
    private String description;
}