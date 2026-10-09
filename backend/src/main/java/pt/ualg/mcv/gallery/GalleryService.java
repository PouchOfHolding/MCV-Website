package pt.ualg.mcv.gallery;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import pt.ualg.mcv.common.services.FileStorageService;
import pt.ualg.mcv.common.services.FileUrlService;
import pt.ualg.mcv.config.security.CustomUserDetails;
import pt.ualg.mcv.exceptions.NotFoundException;
import pt.ualg.mcv.gallery.dtos.GalleryPhotoCreateDto;
import pt.ualg.mcv.gallery.dtos.GalleryPhotoRetrieveDto;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GalleryService
{

    private final GalleryPhotoRepository galleryPhotoRepository;
    private final FileStorageService fileStorageService;
    private final FileUrlService fileUrlService;

    @Transactional(readOnly = true)
    public Page<GalleryPhotoRetrieveDto> getPhotos(Pageable pageable)
    {
        return galleryPhotoRepository.findAll(pageable).map(this::mapToRetrieveDto);
    }

    @Transactional
    public GalleryPhotoRetrieveDto uploadPhoto(MultipartFile file, GalleryPhotoCreateDto dto,
            CustomUserDetails userDetails)
    {
        String filePath = fileStorageService.storeFile(file);
        String thumbnailPath = fileStorageService.createThumbnail(filePath);

        GalleryPhoto photo = GalleryPhoto.builder()
                .filePath(filePath)
                .thumbnailPath(thumbnailPath)
                .originalName(file.getOriginalFilename())
                .fileSizeBytes(file.getSize())
                .mimeType(file.getContentType())
                .title(dto.getTitle())
                .description(dto.getDescription())
                .createdBy(userDetails != null ? userDetails.getUser() : null)
                .build();

        GalleryPhoto savedPhoto = galleryPhotoRepository.save(photo);
        return mapToRetrieveDto(savedPhoto);
    }

    @Transactional
    public void deletePhoto(UUID uuid)
    {
        GalleryPhoto photo = galleryPhotoRepository.findById(uuid)
                .orElseThrow(NotFoundException::new);

        fileStorageService.deleteFile(photo.getFilePath());
        fileStorageService.deleteFile(photo.getThumbnailPath());

        galleryPhotoRepository.delete(photo);
    }

    private GalleryPhotoRetrieveDto mapToRetrieveDto(GalleryPhoto photo) {
        GalleryPhotoRetrieveDto dto = new GalleryPhotoRetrieveDto();
        dto.setUuid(photo.getUuid());
        
        dto.setFilePath(fileUrlService.buildPublicUrl(photo.getFilePath()));
        dto.setThumbnailPath(fileUrlService.buildPublicUrl(photo.getThumbnailPath()));
        
        dto.setMimeType(photo.getMimeType());
        dto.setCreatedAt(photo.getCreatedAt());
        dto.setTitle(photo.getTitle());
        dto.setDescription(photo.getDescription());
        return dto;
    }
}