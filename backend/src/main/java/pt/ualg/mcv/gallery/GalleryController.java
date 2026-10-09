package pt.ualg.mcv.gallery;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Encoding;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import pt.ualg.mcv.config.security.CustomUserDetails;
import pt.ualg.mcv.gallery.dtos.GalleryPhotoCreateDto;
import pt.ualg.mcv.gallery.dtos.GalleryPhotoRetrieveDto;

@Tag(name = "Gallery", description = "Endpoints for managing and viewing gallery photos")
@RestController
@RequestMapping("/api/gallery/photos")
@RequiredArgsConstructor
public class GalleryController
{

    private final GalleryService galleryService;

    @Operation(summary = "Get paged list of gallery photos",
            description = "Retrieves a paginated list of all gallery photos. Default ordering shows the most recent photos first.")
    @ApiResponses(value =
    {@ApiResponse(responseCode = "200", description = "Photos retrieved successfully")})
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public Page<GalleryPhotoRetrieveDto> getPhotos(
            @ParameterObject 
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable)
    {
        return galleryService.getPhotos(pageable);
    }

    @Operation(summary = "Upload a new gallery photo",
            description = "Uploads an image file with title and description payload. Requires ADMIN privileges.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value =
    {@ApiResponse(responseCode = "201", description = "Photo uploaded successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid payload, missing file, or validation error"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role")})
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @RequestBody(
        content = @Content(
            encoding = @Encoding(name = "dto", contentType = MediaType.APPLICATION_JSON_VALUE)
        )
    )
    public GalleryPhotoRetrieveDto uploadPhoto(
            @Parameter(description = "The image file to upload", required = true)
            @RequestPart("file") MultipartFile file,

            @Parameter(description = "JSON metadata containing photo title and description", required = true)
            @Valid @RequestPart("dto") GalleryPhotoCreateDto dto,

            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails)
    {
        return galleryService.uploadPhoto(file, dto, userDetails);
    }

    @Operation(summary = "Delete a gallery photo",
            description = "Deletes a gallery photo record and its associated physical files by UUID. Requires ADMIN privileges.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Photo deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role"),
            @ApiResponse(responseCode = "404", description = "Photo not found")
    })
    @DeleteMapping("/{uuid}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void deletePhoto(
            @Parameter(description = "UUID of the photo to delete", required = true)
            @PathVariable("uuid") UUID uuid)
    {
        galleryService.deletePhoto(uuid);
    }
}