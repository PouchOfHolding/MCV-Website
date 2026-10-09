package pt.ualg.mcv.gallery;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import pt.ualg.mcv.common.services.FileStorageService;
import pt.ualg.mcv.common.services.FileUrlService;
import pt.ualg.mcv.config.security.CustomUserDetails;
import pt.ualg.mcv.exceptions.NotFoundException;
import pt.ualg.mcv.gallery.dtos.GalleryPhotoCreateDto;
import pt.ualg.mcv.gallery.dtos.GalleryPhotoRetrieveDto;
import pt.ualg.mcv.user.User;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GalleryServiceTest {

    @Mock
    private GalleryPhotoRepository galleryPhotoRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private FileUrlService fileUrlService;

    @InjectMocks
    private GalleryService galleryService;

    private UUID photoUuid;
    private GalleryPhoto samplePhoto;
    private User sampleUser;
    private CustomUserDetails userDetails;
    private MockMultipartFile sampleFile;

    @BeforeEach
    void setUp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(8080);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        photoUuid = UUID.randomUUID();

        sampleUser = new User();
        sampleUser.setUuid(UUID.randomUUID());
        sampleUser.setEmail("admin@ualg.pt");

        userDetails = mock(CustomUserDetails.class);

        samplePhoto = GalleryPhoto.builder()
                .uuid(photoUuid)
                .title("Sample Title")
                .description("Sample Description")
                .filePath("/app/uploads/sample.jpg")
                .thumbnailPath("/app/uploads/thumb_sample.jpg")
                .mimeType("image/jpeg")
                .createdAt(Instant.now())
                .createdBy(sampleUser)
                .build();

        sampleFile = new MockMultipartFile(
                "file",
                "test-image.jpg",
                "image/jpeg",
                "dummy image content".getBytes()
        );
    }

    @Test
    @DisplayName("getPhotos - Should return paged list of photo retrieve DTOs")
    void getPhotos_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<GalleryPhoto> page = new PageImpl<>(List.of(samplePhoto), pageable, 1);

        when(galleryPhotoRepository.findAll(pageable)).thenReturn(page);
        when(fileUrlService.buildPublicUrl("/app/uploads/sample.jpg")).thenReturn("http://localhost:8080/uploads/sample.jpg");
        when(fileUrlService.buildPublicUrl("/app/uploads/thumb_sample.jpg")).thenReturn("http://localhost:8080/uploads/thumb_sample.jpg");

        Page<GalleryPhotoRetrieveDto> result = galleryService.getPhotos(pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);

        GalleryPhotoRetrieveDto dto = result.getContent().get(0);
        assertThat(dto.getUuid()).isEqualTo(photoUuid);
        assertThat(dto.getTitle()).isEqualTo("Sample Title");
        assertThat(dto.getDescription()).isEqualTo("Sample Description");
        assertThat(dto.getFilePath()).isEqualTo("http://localhost:8080/uploads/sample.jpg");
        assertThat(dto.getThumbnailPath()).isEqualTo("http://localhost:8080/uploads/thumb_sample.jpg");

        verify(galleryPhotoRepository).findAll(pageable);
    }

    @Test
    @DisplayName("getPhotos - Should return empty page when no photos exist")
    void getPhotos_EmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<GalleryPhoto> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(galleryPhotoRepository.findAll(pageable)).thenReturn(emptyPage);

        Page<GalleryPhotoRetrieveDto> result = galleryService.getPhotos(pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        verify(galleryPhotoRepository).findAll(pageable);
    }

    @Test
    @DisplayName("uploadPhoto - Should store file, thumbnail, save entity, and return DTO")
    void uploadPhoto_Success() {
        GalleryPhotoCreateDto dto = new GalleryPhotoCreateDto();
        dto.setTitle("Nature");
        dto.setDescription("A pretty landscape");

        when(userDetails.getUser()).thenReturn(sampleUser);
        when(fileStorageService.storeFile(sampleFile)).thenReturn("/app/uploads/uuid-image.jpg");
        when(fileStorageService.createThumbnail("/app/uploads/uuid-image.jpg")).thenReturn("/app/uploads/thumb_uuid-image.jpg");

        when(galleryPhotoRepository.save(any(GalleryPhoto.class))).thenAnswer(invocation -> {
            GalleryPhoto photoToSave = invocation.getArgument(0);
            photoToSave.setUuid(photoUuid);
            photoToSave.setCreatedAt(Instant.now());
            return photoToSave;
        });

        when(fileUrlService.buildPublicUrl("/app/uploads/uuid-image.jpg")).thenReturn("http://localhost:8080/uploads/uuid-image.jpg");
        when(fileUrlService.buildPublicUrl("/app/uploads/thumb_uuid-image.jpg")).thenReturn("http://localhost:8080/uploads/thumb_uuid-image.jpg");

        GalleryPhotoRetrieveDto result = galleryService.uploadPhoto(sampleFile, dto, userDetails);

        assertThat(result).isNotNull();
        assertThat(result.getUuid()).isEqualTo(photoUuid);
        assertThat(result.getTitle()).isEqualTo("Nature");
        assertThat(result.getDescription()).isEqualTo("A pretty landscape");
        assertThat(result.getFilePath()).isEqualTo("http://localhost:8080/uploads/uuid-image.jpg");
        assertThat(result.getThumbnailPath()).isEqualTo("http://localhost:8080/uploads/thumb_uuid-image.jpg");

        verify(fileStorageService).storeFile(sampleFile);
        verify(fileStorageService).createThumbnail("/app/uploads/uuid-image.jpg");
        verify(galleryPhotoRepository).save(any(GalleryPhoto.class));
    }

    @Test
    @DisplayName("uploadPhoto - Should handle null userDetails without throwing NPE")
    void uploadPhoto_NullUserDetails_Success() {
        GalleryPhotoCreateDto dto = new GalleryPhotoCreateDto();
        dto.setTitle("Anonymous Title");
        dto.setDescription("Anonymous Description");

        when(fileStorageService.storeFile(sampleFile)).thenReturn("/app/uploads/uuid-image.jpg");
        when(fileStorageService.createThumbnail("/app/uploads/uuid-image.jpg")).thenReturn("/app/uploads/thumb_uuid-image.jpg");

        when(galleryPhotoRepository.save(any(GalleryPhoto.class))).thenAnswer(invocation -> {
            GalleryPhoto photoToSave = invocation.getArgument(0);
            photoToSave.setUuid(photoUuid);
            return photoToSave;
        });

        when(fileUrlService.buildPublicUrl("/app/uploads/uuid-image.jpg")).thenReturn("http://localhost:8080/uploads/uuid-image.jpg");
        when(fileUrlService.buildPublicUrl("/app/uploads/thumb_uuid-image.jpg")).thenReturn("http://localhost:8080/uploads/thumb_uuid-image.jpg");

        GalleryPhotoRetrieveDto result = galleryService.uploadPhoto(sampleFile, dto, null);

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Anonymous Title");
        assertThat(result.getDescription()).isEqualTo("Anonymous Description");
        verify(galleryPhotoRepository).save(any(GalleryPhoto.class));
    }

    @Test
    @DisplayName("deletePhoto - Should delete files from disk and repository when photo exists")
    void deletePhoto_Success() {
        when(galleryPhotoRepository.findById(photoUuid)).thenReturn(Optional.of(samplePhoto));

        galleryService.deletePhoto(photoUuid);

        verify(galleryPhotoRepository).findById(photoUuid);
        verify(fileStorageService).deleteFile("/app/uploads/sample.jpg");
        verify(fileStorageService).deleteFile("/app/uploads/thumb_sample.jpg");
        verify(galleryPhotoRepository).delete(samplePhoto);
    }

    @Test
    @DisplayName("deletePhoto - Should throw NotFoundException when photo does not exist")
    void deletePhoto_NotFound_ThrowsException() {
        UUID nonExistentUuid = UUID.randomUUID();
        when(galleryPhotoRepository.findById(nonExistentUuid)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> galleryService.deletePhoto(nonExistentUuid))
                .isInstanceOf(NotFoundException.class);

        verify(galleryPhotoRepository).findById(nonExistentUuid);
        verify(fileStorageService, never()).deleteFile(any());
        verify(galleryPhotoRepository, never()).delete(any());
    }
}