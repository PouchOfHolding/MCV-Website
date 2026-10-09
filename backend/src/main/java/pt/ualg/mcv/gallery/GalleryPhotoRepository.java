package pt.ualg.mcv.gallery;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GalleryPhotoRepository extends JpaRepository<GalleryPhoto, UUID> {
    
}
