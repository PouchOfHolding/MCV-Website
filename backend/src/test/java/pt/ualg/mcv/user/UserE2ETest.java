package pt.ualg.mcv.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pt.ualg.mcv.config.security.CustomUserDetailsService;
import pt.ualg.mcv.exceptions.ConflictException;
import pt.ualg.mcv.exceptions.NotFoundException;
import pt.ualg.mcv.user.dtos.UserRegisterDto;
import pt.ualg.mcv.user.dtos.UserRetrieveDto;
import pt.ualg.mcv.user.dtos.UserUpdateDto;
import pt.ualg.mcv.user.dtos.UserUpdateRoleDto;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerE2ETest
{

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;


    @Test
    void register_Returns201Created_WhenPayloadIsValid() throws Exception
    {
        UserRegisterDto registrationPayload = new UserRegisterDto();
        registrationPayload.setEmail("valid@ualg.pt");
        registrationPayload.setFirstName("Valid");
        registrationPayload.setLastName("User");
        registrationPayload.setPassword("securePassword123");

        mockMvc.perform(post("/api/users/register").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registrationPayload)))
                .andExpect(status().isCreated());
    }

    @Test
    void register_Returns400BadRequest_WhenPayloadIsInvalid() throws Exception
    {
        UserRegisterDto invalidPayload = new UserRegisterDto();
        invalidPayload.setEmail("invalid-email-format");
        invalidPayload.setFirstName("");
        invalidPayload.setLastName("");
        invalidPayload.setPassword("123");

        mockMvc.perform(post("/api/users/register").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidPayload)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_Returns409Conflict_WhenEmailAlreadyInUse() throws Exception
    {
        UserRegisterDto duplicatePayload = new UserRegisterDto();
        duplicatePayload.setEmail("existing@ualg.pt");
        duplicatePayload.setFirstName("John");
        duplicatePayload.setLastName("Doe");
        duplicatePayload.setPassword("securePassword123");

        doThrow(new ConflictException("Email already in use.")).when(userService)
                .register(any(UserRegisterDto.class));

        mockMvc.perform(post("/api/users/register").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(duplicatePayload)))
                .andExpect(status().isConflict());
    }


    @Test
    @WithMockUser(username = "adminuser", roles = {"ADMIN"})
    void getUser_Returns200AndUser_WhenUserIsAdminAndRecordExists() throws Exception
    {
        UUID targetUuid = UUID.randomUUID();

        UserRetrieveDto mockUser = new UserRetrieveDto();
        mockUser.setEmail("john.doe@ualg.pt");
        mockUser.setFirstName("John");
        mockUser.setLastName("Doe");
        mockUser.setCreatedAt(LocalDateTime.now().toInstant(ZoneOffset.UTC));

        when(userService.getUser(targetUuid)).thenReturn(mockUser);

        mockMvc.perform(get("/api/users/{uuid}", targetUuid)).andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("john.doe@ualg.pt"))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"));
    }

    @Test
    @WithMockUser(username = "regularuser", roles = {"USER"})
    void getUser_Returns403Forbidden_WhenUserIsNotAdmin() throws Exception
    {
        UUID targetUuid = UUID.randomUUID();

        mockMvc.perform(get("/api/users/{uuid}", targetUuid))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "adminuser", roles = {"ADMIN"})
    void getUser_Returns404NotFound_WhenUserDoesNotExist() throws Exception
    {
        UUID nonExistentUuid = UUID.randomUUID();

        when(userService.getUser(nonExistentUuid))
                .thenThrow(new NotFoundException("User not found."));

        mockMvc.perform(get("/api/users/{uuid}", nonExistentUuid)).andExpect(status().isNotFound());
    }

    @Test
    void getUser_Returns401Unauthorized_WhenUnauthenticated() throws Exception
    {
        UUID targetUuid = UUID.randomUUID();

        mockMvc.perform(get("/api/users/{uuid}", targetUuid)).andExpect(status().isUnauthorized());
    }


    @Test
    @WithMockUser(username = "authenticated@ualg.pt", roles = {"USER"})
    void getMe_Returns200AndProfile_WhenAuthenticated() throws Exception
    {
        UserRetrieveDto mockProfile = new UserRetrieveDto();
        mockProfile.setEmail("authenticated@ualg.pt");
        mockProfile.setFirstName("Alice");
        mockProfile.setLastName("Smith");

        when(userService.getMe(any())).thenReturn(mockProfile);

        mockMvc.perform(get("/api/users/me")).andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("authenticated@ualg.pt"))
                .andExpect(jsonPath("$.firstName").value("Alice"));
    }

    @Test
    void getMe_Returns401Unauthorized_WhenUnauthenticated() throws Exception
    {
        mockMvc.perform(get("/api/users/me")).andExpect(status().isUnauthorized());
    }


    @Test
    @WithMockUser(username = "adminuser", roles = {"ADMIN"})
    void getUsers_ReturnsPagedData_WhenUserIsAdmin() throws Exception
    {
        UserRetrieveDto item = new UserRetrieveDto();
        item.setEmail("student@ualg.pt");
        item.setFirstName("Bob");
        item.setLastName("Marley");

        PageImpl<UserRetrieveDto> pagedResult =
                new PageImpl<>(List.of(item), PageRequest.of(0, 20), 1);

        when(userService.getUsers(any(), any(Pageable.class), any())).thenReturn(pagedResult);

        mockMvc.perform(get("/api/users").param("page", "0").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].email").value("student@ualg.pt"))
                .andExpect(jsonPath("$.content[0].firstName").value("Bob"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @WithMockUser(username = "adminuser", roles = {"ADMIN"})
    void getUsers_ReturnsFilteredData_WhenNameQueryProvided() throws Exception
    {
        UserRetrieveDto item = new UserRetrieveDto();
        item.setEmail("john.doe@ualg.pt");
        item.setFirstName("John");
        item.setLastName("Doe");

        PageImpl<UserRetrieveDto> pagedResult =
                new PageImpl<>(List.of(item), PageRequest.of(0, 20), 1);

        when(userService.getUsers(eq("John Doe"), any(Pageable.class), any())).thenReturn(pagedResult);

        mockMvc.perform(get("/api/users")
                .param("name", "John Doe")
                .param("page", "0")
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].firstName").value("John"))
                .andExpect(jsonPath("$.content[0].lastName").value("Doe"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @WithMockUser(username = "regularuser", roles = {"USER"})
    void getUsers_Returns403Forbidden_WhenUserIsNotAdmin() throws Exception
    {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getUsers_Returns401Unauthorized_WhenUnauthenticated() throws Exception
    {
        mockMvc.perform(get("/api/users")).andExpect(status().isUnauthorized());
    }


    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void updateUser_Returns200AndUpdatedUser_WhenPayloadIsValidAndAuthenticated() throws Exception
    {
        UUID targetUuid = UUID.randomUUID();

        UserUpdateDto updatePayload = new UserUpdateDto();
        updatePayload.setFirstName("UpdatedFirstName");
        updatePayload.setLastName("UpdatedLastName");

        UserRetrieveDto updatedResponse = new UserRetrieveDto();
        updatedResponse.setUuid(targetUuid);
        updatedResponse.setEmail("john.doe@ualg.pt");
        updatedResponse.setFirstName("UpdatedFirstName");
        updatedResponse.setLastName("UpdatedLastName");

        when(userService.updateUser(any(UserUpdateDto.class), any()))
                .thenReturn(updatedResponse);

        mockMvc.perform(
                patch("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatePayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid").value(targetUuid.toString()))
                .andExpect(jsonPath("$.firstName").value("UpdatedFirstName"))
                .andExpect(jsonPath("$.lastName").value("UpdatedLastName"));
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void updateUser_Returns400BadRequest_WhenPayloadIsInvalid() throws Exception
    {
        UserUpdateDto invalidPayload = new UserUpdateDto();
        invalidPayload.setFirstName("A".repeat(100));

        mockMvc.perform(
                patch("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidPayload)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateUser_Returns401Unauthorized_WhenUnauthenticated() throws Exception
    {
        UserUpdateDto updatePayload = new UserUpdateDto();
        updatePayload.setFirstName("Jane");

        mockMvc.perform(
                patch("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatePayload)))
                .andExpect(status().isUnauthorized());
    }


    @Test
    @WithMockUser(username = "adminuser", roles = {"ADMIN"})
    void updateUserRole_Returns200AndUpdatedUser_WhenUserIsAdminAndPayloadIsValid() throws Exception
    {
        UUID targetUuid = UUID.randomUUID();
        UserUpdateRoleDto rolePayload = new UserUpdateRoleDto();
        rolePayload.setRole(UserRole.ADMIN);

        UserRetrieveDto updatedUser = new UserRetrieveDto();
        updatedUser.setUuid(targetUuid);
        updatedUser.setEmail("promoted.user@ualg.pt");
        updatedUser.setRole(UserRole.ADMIN);

        when(userService.updateUserRole(eq(targetUuid), any(UserUpdateRoleDto.class), any()))
                .thenReturn(updatedUser);

        mockMvc.perform(patch("/api/users/{uuid}/role", targetUuid)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(rolePayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid").value(targetUuid.toString()))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    @WithMockUser(username = "regularuser", roles = {"USER"})
    void updateUserRole_Returns403Forbidden_WhenUserIsNotAdmin() throws Exception
    {
        UUID targetUuid = UUID.randomUUID();
        UserUpdateRoleDto rolePayload = new UserUpdateRoleDto();
        rolePayload.setRole(UserRole.ADMIN);

        mockMvc.perform(patch("/api/users/{uuid}/role", targetUuid)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(rolePayload)))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateUserRole_Returns401Unauthorized_WhenUnauthenticated() throws Exception
    {
        UUID targetUuid = UUID.randomUUID();
        UserUpdateRoleDto rolePayload = new UserUpdateRoleDto();
        rolePayload.setRole(UserRole.ADMIN);

        mockMvc.perform(patch("/api/users/{uuid}/role", targetUuid)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(rolePayload)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "adminuser", roles = {"ADMIN"})
    void updateUserRole_Returns404NotFound_WhenUserDoesNotExist() throws Exception
    {
        UUID targetUuid = UUID.randomUUID();
        UserUpdateRoleDto rolePayload = new UserUpdateRoleDto();
        rolePayload.setRole(UserRole.ADMIN);

        when(userService.updateUserRole(eq(targetUuid), any(UserUpdateRoleDto.class), any()))
                .thenThrow(new NotFoundException("User not found."));

        mockMvc.perform(patch("/api/users/{uuid}/role", targetUuid)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(rolePayload)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "adminuser", roles = {"ADMIN"})
    void updateUserRole_Returns409Conflict_WhenAdminModifiesOwnRole() throws Exception
    {
        UUID adminUuid = UUID.randomUUID();
        UserUpdateRoleDto rolePayload = new UserUpdateRoleDto();
        rolePayload.setRole(UserRole.MEMBER);

        when(userService.updateUserRole(eq(adminUuid), any(UserUpdateRoleDto.class), any()))
                .thenThrow(new ConflictException("Admins cannot modify their own role."));

        mockMvc.perform(patch("/api/users/{uuid}/role", adminUuid)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(rolePayload)))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(username = "adminuser", roles = {"ADMIN"})
    void updateUserRole_Returns400BadRequest_WhenRoleIsNull() throws Exception
    {
        UUID targetUuid = UUID.randomUUID();
        UserUpdateRoleDto invalidPayload = new UserUpdateRoleDto();

        mockMvc.perform(patch("/api/users/{uuid}/role", targetUuid)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidPayload)))
                .andExpect(status().isBadRequest());
    }
}