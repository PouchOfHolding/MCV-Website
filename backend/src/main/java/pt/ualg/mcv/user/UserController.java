package pt.ualg.mcv.user;

import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springdoc.core.converters.models.PageableAsQueryParam;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
import pt.ualg.mcv.user.dtos.UserRegisterDto;
import pt.ualg.mcv.user.dtos.UserRetrieveDto;
import pt.ualg.mcv.user.dtos.UserUpdateDto;
import pt.ualg.mcv.user.dtos.UserUpdateRoleDto;

@Tag(name = "User Management",
                description = "Endpoints for managing users, registration, and authentication context")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController
{

        private final UserService userService;

        @Operation(summary = "Register a new user",
                        description = "Creates a new public user account with provided registration details.")
        @ApiResponses(value =
        {@ApiResponse(responseCode = "201", description = "User successfully registered"),
                        @ApiResponse(responseCode = "400",
                                        description = "Invalid request payload or validation failed"),
                        @ApiResponse(responseCode = "409", description = "Email already in use")})
        @PostMapping("/register")
        @ResponseStatus(HttpStatus.CREATED)
        public void register(@Valid @RequestBody UserRegisterDto userRegisterDto)
        {
                userService.register(userRegisterDto);
        }

        @Operation(summary = "Get user by UUID",
                        description = "Retrieves user details corresponding to the specified UUID. Requires ADMIN privileges.",
                        security = @SecurityRequirement(name = "bearerAuth"))
        @ApiResponses(value =
        {@ApiResponse(responseCode = "200", description = "User retrieved successfully"),
                        @ApiResponse(responseCode = "401",
                                        description = "Unauthorized - Missing or invalid JWT token"),
                        @ApiResponse(responseCode = "403",
                                        description = "Forbidden - Requires ADMIN role"),
                        @ApiResponse(responseCode = "404", description = "User not found")})
        @GetMapping("/{uuid}")
        @PreAuthorize("hasRole('ADMIN')")
        @ResponseStatus(HttpStatus.OK)
        public UserRetrieveDto getUser(@Parameter(description = "UUID of the user to fetch",
                        required = true) @PathVariable UUID uuid)
        {
                return userService.getUser(uuid);
        }

        @Operation(summary = "Get paginated users",
                        description = "Retrieves a paginated list of users with optional full name or partial name filtering. Requires ADMIN privileges.",
                        security = @SecurityRequirement(name = "bearerAuth"))
        @ApiResponses(value =
        {@ApiResponse(responseCode = "200", description = "Paginated list retrieved successfully"),
                        @ApiResponse(responseCode = "401",
                                        description = "Unauthorized - Missing or invalid JWT token"),
                        @ApiResponse(responseCode = "403",
                                        description = "Forbidden - Requires ADMIN role")})
        @PageableAsQueryParam
        @GetMapping
        @PreAuthorize("hasRole('ADMIN')")
        @ResponseStatus(HttpStatus.OK)
        public Page<UserRetrieveDto> getUsers(
                        @Parameter(description = "Filter by full name, first name, or last name",
                                        example = "John Doe") @RequestParam(value = "name",
                                                        required = false) String name,
                        @Parameter(hidden = true) @PageableDefault(sort = "createdAt",
                                        direction = Sort.Direction.DESC) Pageable pageable,
                        @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails)
        {
                return userService.getUsers(name, pageable, userDetails);
        }

        @Operation(summary = "Get current authenticated user",
                        description = "Retrieves profile details for the currently logged-in user.",
                        security = @SecurityRequirement(name = "bearerAuth"))
        @ApiResponses(value =
        {@ApiResponse(responseCode = "200", description = "Current user retrieved successfully"),
                        @ApiResponse(responseCode = "401",
                                        description = "Unauthorized - Missing or invalid JWT token")})
        @GetMapping("/me")
        @ResponseStatus(HttpStatus.OK)
        public UserRetrieveDto getMe(@Parameter(
                        hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails)
        {
                return userService.getMe(userDetails);
        }

        @Operation(summary = "Update current user details",
                        description = "Partially updates profile details for the currently logged-in user.",
                        security = @SecurityRequirement(name = "bearerAuth"))
        @ApiResponses(value =
        {@ApiResponse(responseCode = "200", description = "User updated successfully"),
                        @ApiResponse(responseCode = "400",
                                        description = "Invalid request payload or validation failed"),
                        @ApiResponse(responseCode = "401",
                                        description = "Unauthorized - Missing or invalid JWT token")})
        @PatchMapping
        @ResponseStatus(HttpStatus.OK)
        public UserRetrieveDto updateUser(@Valid @RequestBody UserUpdateDto userUpdateDto,
                        @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails)
        {
                return userService.updateUser(userUpdateDto, userDetails);
        }

        @Operation(summary = "Update user role",
                        description = "Updates the role of a user specified by UUID. Requires ADMIN privileges.",
                        security = @SecurityRequirement(name = "bearerAuth"))
        @ApiResponses(value =
        {@ApiResponse(responseCode = "200", description = "User role updated successfully"),
                        @ApiResponse(responseCode = "400",
                                        description = "Invalid request payload or validation failed"),
                        @ApiResponse(responseCode = "401",
                                        description = "Unauthorized - Missing or invalid JWT token"),
                        @ApiResponse(responseCode = "403",
                                        description = "Forbidden - Requires ADMIN role"),
                        @ApiResponse(responseCode = "404", description = "User not found")})
        @PatchMapping("/{uuid}/role")
        @PreAuthorize("hasRole('ADMIN')")
        @ResponseStatus(HttpStatus.OK)
        public UserRetrieveDto updateUserRole(
                        @Parameter(description = "UUID of the user whose role is to be updated",
                                        required = true) @PathVariable UUID uuid,
                        @Valid @RequestBody UserUpdateRoleDto userUpdateRoleDto,
                        @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails)
        {
                return userService.updateUserRole(uuid, userUpdateRoleDto, userDetails);
        }
}
