package pt.ualg.mcv.gallery;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pt.ualg.mcv.config.security.CustomUserDetailsService;
import pt.ualg.mcv.exceptions.NotFoundException;
import pt.ualg.mcv.gallery.dtos.GalleryPhotoCreateDto;
import pt.ualg.mcv.gallery.dtos.GalleryPhotoRetrieveDto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GalleryControllerE2ETest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private GalleryService galleryService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private UUID photoUuid;
    private GalleryPhotoRetrieveDto sampleRetrieveDto;
    private MockMultipartFile filePart;

    @BeforeEach
    void setUp() {
        photoUuid = UUID.randomUUID();

        sampleRetrieveDto = new GalleryPhotoRetrieveDto();
        sampleRetrieveDto.setUuid(photoUuid);
        sampleRetrieveDto.setCreatedAt(Instant.now());
        sampleRetrieveDto.setTitle("Sunset");
        sampleRetrieveDto.setDescription("Beautiful sunset");
        sampleRetrieveDto.setFilePath("http://localhost/uploads/photo.jpg");
        sampleRetrieveDto.setThumbnailPath("http://localhost/uploads/thumb_photo.jpg");

        filePart = new MockMultipartFile(
                "file",
                "sunset.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "fake image content".getBytes()
        );
    }

    private MockMultipartFile createDtoPart(GalleryPhotoCreateDto dto) throws Exception {
        return new MockMultipartFile(
                "dto",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsString(dto).getBytes()
        );
    }

    @Test
    @DisplayName("GET /api/gallery/photos - Should return 200 OK publicly without authentication")
    void getPhotos_PublicAccess_Success() throws Exception {
        PageImpl<GalleryPhotoRetrieveDto> page = new PageImpl<>(List.of(sampleRetrieveDto));
        when(galleryService.getPhotos(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/gallery/photos")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].uuid").value(photoUuid.toString()))
                .andExpect(jsonPath("$.content[0].title").value("Sunset"))
                .andExpect(jsonPath("$.content[0].description").value("Beautiful sunset"))
                .andExpect(jsonPath("$.content[0].filePath").value("http://localhost/uploads/photo.jpg"))
                .andExpect(jsonPath("$.content[0].thumbnailPath").value("http://localhost/uploads/thumb_photo.jpg"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/gallery/photos - Should return 201 Created for ADMIN user creating photo")
    void createPhoto_Admin_Success() throws Exception {
        GalleryPhotoCreateDto createDto = new GalleryPhotoCreateDto();
        createDto.setTitle("Sunset");
        createDto.setDescription("Beautiful sunset");

        MockMultipartFile dtoPart = createDtoPart(createDto);

        when(galleryService.uploadPhoto(any(), any(), any())).thenReturn(sampleRetrieveDto);

        mockMvc.perform(multipart("/api/gallery/photos")
                .file(filePart)
                .file(dtoPart))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.uuid").value(photoUuid.toString()))
                .andExpect(jsonPath("$.title").value("Sunset"))
                .andExpect(jsonPath("$.description").value("Beautiful sunset"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("POST /api/gallery/photos - Should return 403 Forbidden for non-ADMIN user")
    void createPhoto_ForbiddenForUser() throws Exception {
        GalleryPhotoCreateDto createDto = new GalleryPhotoCreateDto();
        createDto.setTitle("Sunset");
        createDto.setDescription("Forbidden photo");

        MockMultipartFile dtoPart = createDtoPart(createDto);

        mockMvc.perform(multipart("/api/gallery/photos")
                .file(filePart)
                .file(dtoPart))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/gallery/photos - Should return 401 Unauthorized for unauthenticated request")
    void createPhoto_Unauthorized() throws Exception {
        GalleryPhotoCreateDto createDto = new GalleryPhotoCreateDto();
        createDto.setTitle("Sunset");
        createDto.setDescription("Unauthenticated");

        MockMultipartFile dtoPart = createDtoPart(createDto);

        mockMvc.perform(multipart("/api/gallery/photos")
                .file(filePart)
                .file(dtoPart))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/gallery/photos - Should return 400 Bad Request when title is blank")
    void createPhoto_MissingTitle_BadRequest() throws Exception {
        GalleryPhotoCreateDto createDto = new GalleryPhotoCreateDto();
        createDto.setTitle("");
        createDto.setDescription("Description with empty title");

        MockMultipartFile dtoPart = createDtoPart(createDto);

        mockMvc.perform(multipart("/api/gallery/photos")
                .file(filePart)
                .file(dtoPart))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/gallery/photos - Should return 400 Bad Request when description is blank")
    void createPhoto_MissingDescription_BadRequest() throws Exception {
        GalleryPhotoCreateDto createDto = new GalleryPhotoCreateDto();
        createDto.setTitle("Title");
        createDto.setDescription("");

        MockMultipartFile dtoPart = createDtoPart(createDto);

        mockMvc.perform(multipart("/api/gallery/photos")
                .file(filePart)
                .file(dtoPart))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("DELETE /api/gallery/photos/{uuid} - Should return 204 No Content for ADMIN user")
    void deletePhoto_Admin_Success() throws Exception {
        doNothing().when(galleryService).deletePhoto(photoUuid);

        mockMvc.perform(delete("/api/gallery/photos/{uuid}", photoUuid))
                .andExpect(status().isNoContent());

        verify(galleryService).deletePhoto(photoUuid);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("DELETE /api/gallery/photos/{uuid} - Should return 404 Not Found when photo does not exist")
    void deletePhoto_NotFound() throws Exception {
        UUID nonExistentUuid = UUID.randomUUID();
        doThrow(new NotFoundException("Photo not found")).when(galleryService).deletePhoto(nonExistentUuid);

        mockMvc.perform(delete("/api/gallery/photos/{uuid}", nonExistentUuid))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("DELETE /api/gallery/photos/{uuid} - Should return 403 Forbidden for non-ADMIN user")
    void deletePhoto_ForbiddenForUser() throws Exception {
        mockMvc.perform(delete("/api/gallery/photos/{uuid}", photoUuid))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /api/gallery/photos/{uuid} - Should return 401 Unauthorized for unauthenticated request")
    void deletePhoto_Unauthorized() throws Exception {
        mockMvc.perform(delete("/api/gallery/photos/{uuid}", photoUuid))
                .andExpect(status().isUnauthorized());
    }
}