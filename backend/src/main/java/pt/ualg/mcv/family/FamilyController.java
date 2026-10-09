package pt.ualg.mcv.family;

import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
import org.springdoc.core.converters.models.PageableAsQueryParam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import pt.ualg.mcv.config.security.CustomUserDetails;
import pt.ualg.mcv.family.dtos.FamilyAddMemberDto;
import pt.ualg.mcv.family.dtos.FamilyCreatePatchDto;
import pt.ualg.mcv.family.dtos.FamilyRetrieveDto;
import pt.ualg.mcv.family.dtos.FamilyMemberRetrieveDto;
import org.springframework.data.domain.Sort;

@Tag(name = "Families", description = "Endpoints for managing families and family associations")
@RestController
@RequestMapping("/api/families")
@RequiredArgsConstructor
public class FamilyController
{

        private final FamilyService familyService;

        @Operation(summary = "Create a new family",
                        description = "Creates a new family entry. Requires ADMIN privileges.",
                        security = @SecurityRequirement(name = "bearerAuth"))
        @ApiResponses(value =
        {@ApiResponse(responseCode = "201", description = "Family created successfully"),
                        @ApiResponse(responseCode = "400",
                                        description = "Invalid payload or validation error"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "403",
                                        description = "Forbidden - Requires ADMIN role")})
        @PostMapping
        @ResponseStatus(HttpStatus.CREATED)
        @PreAuthorize("hasRole('ADMIN')")
        public FamilyRetrieveDto createFamily(
                        @Valid @RequestBody FamilyCreatePatchDto familyCreatePatchDto,
                        @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails)
        {
                return familyService.createFamily(familyCreatePatchDto, userDetails);
        }

        @Operation(summary = "Patch family information",
                        description = "Updates specific fields of an existing family by UUID. Requires ADMIN privileges.",
                        security = @SecurityRequirement(name = "bearerAuth"))
        @ApiResponses(value =
        {@ApiResponse(responseCode = "200", description = "Family updated successfully"),
                        @ApiResponse(responseCode = "400",
                                        description = "Invalid payload or validation error"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "403",
                                        description = "Forbidden - Requires ADMIN role"),
                        @ApiResponse(responseCode = "404", description = "Family not found")})
        @PatchMapping("/{uuid}")
        @ResponseStatus(HttpStatus.OK)
        @PreAuthorize("hasRole('ADMIN')")
        public FamilyRetrieveDto patchFamily(
                        @Parameter(description = "UUID of the family",
                                        required = true) @PathVariable UUID uuid,
                        @Valid @RequestBody FamilyCreatePatchDto familyCreatePatchDto)
        {
                return familyService.patchFamily(uuid, familyCreatePatchDto);
        }

        @Operation(summary = "Get a family by UUID",
                        description = "Retrieves family details for a specific family UUID.")
        @ApiResponses(value =
        {@ApiResponse(responseCode = "200", description = "Family details retrieved successfully"),
                        @ApiResponse(responseCode = "404", description = "Family not found")})
        @GetMapping("/{uuid}")
        @ResponseStatus(HttpStatus.OK)
        public FamilyRetrieveDto getFamily(@Parameter(description = "UUID of the family",
                        required = true) @PathVariable UUID uuid)
        {
                return familyService.getFamily(uuid);
        }

        @Operation(summary = "Get paged list of families",
                        description = "Retrieves a paginated list of all families with optional year range filtering.")
        @ApiResponses(value =
        {@ApiResponse(responseCode = "200", description = "Families retrieved successfully")})
        @PageableAsQueryParam
        @GetMapping
        @ResponseStatus(HttpStatus.OK)
        public Page<FamilyRetrieveDto> getFamilies(
                        @Parameter(description = "Minimum year boundary (inclusive)",
                                        example = "2020") @RequestParam(value = "minYear",
                                                        required = false) Long minYear,
                        @Parameter(description = "Maximum year boundary (inclusive)",
                                        example = "2026") @RequestParam(value = "maxYear",
                                                        required = false) Long maxYear,
                        @ParameterObject @PageableDefault(sort = "year",
                                        direction = Sort.Direction.DESC) Pageable pageable)
        {
                return familyService.getFamilies(minYear, maxYear, pageable);
        }

        @Operation(summary = "Delete a family",
                        description = "Deletes a family by its UUID. Requires ADMIN privileges.",
                        security = @SecurityRequirement(name = "bearerAuth"))
        @ApiResponses(value =
        {@ApiResponse(responseCode = "204", description = "Family deleted successfully"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "403",
                                        description = "Forbidden - Requires ADMIN role"),
                        @ApiResponse(responseCode = "404", description = "Family not found")})
        @DeleteMapping("/{uuid}")
        @ResponseStatus(HttpStatus.NO_CONTENT)
        @PreAuthorize("hasRole('ADMIN')")
        public void deleteFamily(@Parameter(description = "UUID of the family to delete",
                        required = true) @PathVariable UUID uuid)
        {
                familyService.deleteFamily(uuid);
        }

        @Operation(summary = "Add a member to a family",
                        description = "Associates an existing member with a family and assigns them a role. Max 1 DUX and 8 FACULTIS allowed. Requires ADMIN privileges.",
                        security = @SecurityRequirement(name = "bearerAuth"))
        @ApiResponses(value =
        {@ApiResponse(responseCode = "201", description = "Member added to family successfully"),
                        @ApiResponse(responseCode = "400",
                                        description = "Invalid payload or validation error"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "403",
                                        description = "Forbidden - Requires ADMIN role"),
                        @ApiResponse(responseCode = "404",
                                        description = "Family or Member not found"),
                        @ApiResponse(responseCode = "409",
                                        description = "Member already belongs to this family or role capacity exceeded")})
        @PostMapping("/{familyUuid}/members/{memberUuid}")
        @ResponseStatus(HttpStatus.CREATED)
        @PreAuthorize("hasRole('ADMIN')")
        public FamilyMemberRetrieveDto addMemberToFamily(
                        @Parameter(description = "UUID of the target family",
                                        required = true) @PathVariable UUID familyUuid,
                        @Parameter(description = "UUID of the member to add",
                                        required = true) @PathVariable UUID memberUuid,
                        @Valid @RequestBody FamilyAddMemberDto familyAddMemberDto)
        {
                return familyService.addMemberToFamily(familyUuid, memberUuid, familyAddMemberDto);
        }

        @Operation(summary = "Update a member's role in a family",
                        description = "Updates the role of a member who is already part of the family. Subject to role capacity limits. Requires ADMIN privileges.",
                        security = @SecurityRequirement(name = "bearerAuth"))
        @ApiResponses(value =
        {@ApiResponse(responseCode = "200", description = "Member role updated successfully"),
                        @ApiResponse(responseCode = "400",
                                        description = "Invalid payload or validation error"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "403",
                                        description = "Forbidden - Requires ADMIN role"),
                        @ApiResponse(responseCode = "404",
                                        description = "Family, Member, or Association not found"),
                        @ApiResponse(responseCode = "409", description = "Role capacity exceeded")})
        @PatchMapping("/{familyUuid}/members/{memberUuid}")
        @ResponseStatus(HttpStatus.OK)
        @PreAuthorize("hasRole('ADMIN')")
        public FamilyMemberRetrieveDto updateMemberRoleInFamily(
                        @Parameter(description = "UUID of the target family",
                                        required = true) @PathVariable UUID familyUuid,
                        @Parameter(description = "UUID of the member to update",
                                        required = true) @PathVariable UUID memberUuid,
                        @Valid @RequestBody FamilyAddMemberDto familyAddMemberDto)
        {
                return familyService.updateMemberRoleInFamily(familyUuid, memberUuid,
                                familyAddMemberDto);
        }

        @Operation(summary = "Remove a member from a family",
                        description = "Removes the association between a member and a family. Requires ADMIN privileges.",
                        security = @SecurityRequirement(name = "bearerAuth"))
        @ApiResponses(value =
        {@ApiResponse(responseCode = "204",
                        description = "Member removed from family successfully"),
                        @ApiResponse(responseCode = "401", description = "Unauthorized"),
                        @ApiResponse(responseCode = "403",
                                        description = "Forbidden - Requires ADMIN role"),
                        @ApiResponse(responseCode = "404",
                                        description = "Family, Member, or Association not found")})
        @DeleteMapping("/{familyUuid}/members/{memberUuid}")
        @ResponseStatus(HttpStatus.NO_CONTENT)
        @PreAuthorize("hasRole('ADMIN')")
        public void removeMemberFromFamily(
                        @Parameter(description = "UUID of the family",
                                        required = true) @PathVariable UUID familyUuid,
                        @Parameter(description = "UUID of the member to remove",
                                        required = true) @PathVariable UUID memberUuid)
        {
                familyService.removeMemberFromFamily(familyUuid, memberUuid);
        }

        @Operation(summary = "List members belonging to a family",
                        description = "Retrieves a paginated list of all members associated with the specified family.")
        @ApiResponses(value =
        {@ApiResponse(responseCode = "200", description = "Members list retrieved successfully"),
                        @ApiResponse(responseCode = "404", description = "Family not found")})
        @GetMapping("/{familyUuid}/members")
        @ResponseStatus(HttpStatus.OK)
        public Page<FamilyMemberRetrieveDto> listMembersOfFamily(
                        @Parameter(description = "UUID of the family",
                                        required = true) @PathVariable UUID familyUuid,
                        @ParameterObject Pageable pageable)
        {
                return familyService.listMembersOfFamily(familyUuid, pageable);
        }
}
