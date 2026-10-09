package pt.ualg.mcv.family;

import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
import org.springdoc.core.converters.models.PageableAsQueryParam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import pt.ualg.mcv.config.security.CustomUserDetails;
import pt.ualg.mcv.family.dtos.FamilyMemberCreateDto;
import pt.ualg.mcv.family.dtos.FamilyMemberPatchDto;
import pt.ualg.mcv.family.dtos.FamilyMemberPhotoRetrieveDto;
import pt.ualg.mcv.family.dtos.FamilyMemberRetrieveDto;

import org.springframework.data.domain.Sort;

@Tag(name = "Members",
        description = "Endpoints for managing family members and their photo attachments")
@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController
{

    private final MemberService memberService;

    @Operation(summary = "Create a new member",
            description = "Creates a new family member. Requires ADMIN privileges.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value =
    {@ApiResponse(responseCode = "201", description = "Member created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid payload or validation error"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role")})
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public FamilyMemberRetrieveDto createMember(
            @Valid @RequestBody FamilyMemberCreateDto familyMemberCreateDto,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails)
    {
        return memberService.createMember(familyMemberCreateDto, userDetails);
    }

    @Operation(summary = "Patch member details",
            description = "Updates specific fields of an existing member by UUID. Requires ADMIN privileges.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value =
    {@ApiResponse(responseCode = "200", description = "Member updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid payload or validation error"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role"),
            @ApiResponse(responseCode = "404", description = "Member not found")})
    @PatchMapping("/{uuid}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.OK)
    public FamilyMemberRetrieveDto patchMember(
            @Parameter(description = "UUID of the member", required = true) @PathVariable UUID uuid,
            @Valid @RequestBody FamilyMemberPatchDto familyMemberPatchDto)
    {
        return memberService.patchMember(uuid, familyMemberPatchDto);
    }

    @Operation(summary = "Get a member by UUID",
            description = "Retrieves details for a specific member.")
    @ApiResponses(value =
    {@ApiResponse(responseCode = "200", description = "Member details retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Member not found")})
    @GetMapping("/{uuid}")
    @ResponseStatus(HttpStatus.OK)
    public FamilyMemberRetrieveDto getMember(
            @Parameter(description = "UUID of the member", required = true) @PathVariable UUID uuid)
    {
        return memberService.getMember(uuid);
    }

    @Operation(summary = "Get paginated list of members",
            description = "Retrieves a paginated list of all family members with optional name filtering.")
    @ApiResponses(value =
    {@ApiResponse(responseCode = "200", description = "Members retrieved successfully")})
    @PageableAsQueryParam
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public Page<FamilyMemberRetrieveDto> getMembers(
            @Parameter(description = "Filter members by full name, first name, or last name",
                    example = "John Doe") @RequestParam(value = "name",
                            required = false) String name,
            @ParameterObject @PageableDefault(sort = "createdAt",
                    direction = Sort.Direction.DESC) Pageable pageable)
    {
        return memberService.getMembers(name, pageable);
    }

    @Operation(summary = "Delete a member",
            description = "Deletes a member by their UUID. Requires ADMIN privileges.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value =
    {@ApiResponse(responseCode = "204", description = "Member deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role"),
            @ApiResponse(responseCode = "404", description = "Member not found")})
    @DeleteMapping("/{uuid}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMember(@Parameter(description = "UUID of the member to delete",
            required = true) @PathVariable UUID uuid)
    {
        memberService.deleteMember(uuid);
    }

    @Operation(summary = "Upload photo for a member",
            description = "Uploads an image file to attach as a photo for the specified member. Requires ADMIN privileges.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value =
    {@ApiResponse(responseCode = "201", description = "Photo uploaded and attached successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid file or invalid request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role"),
            @ApiResponse(responseCode = "404", description = "Member not found")})
    @PostMapping(value = "/{memberUuid}/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public FamilyMemberPhotoRetrieveDto addPhoto(
            @Parameter(description = "UUID of the target member",
                    required = true) @PathVariable UUID memberUuid,
            @Parameter(description = "Photo file to upload", required = true,
                    content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            schema = @Schema(type = "string",
                                    format = "binary"))) @RequestParam("file") MultipartFile file,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails)
    {
        return memberService.addPhoto(memberUuid, file, userDetails);
    }

    @Operation(summary = "Remove a photo from a member",
            description = "Deletes a specific photo belonging to a member. Requires ADMIN privileges.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value =
    {@ApiResponse(responseCode = "204", description = "Photo removed successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role"),
            @ApiResponse(responseCode = "404", description = "Member or Photo not found")})
    @DeleteMapping("/{memberUuid}/photos/{photoUuid}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removePhoto(
            @Parameter(description = "UUID of the member",
                    required = true) @PathVariable UUID memberUuid,
            @Parameter(description = "UUID of the photo to remove",
                    required = true) @PathVariable UUID photoUuid)
    {
        memberService.removePhoto(memberUuid, photoUuid);
    }
}
