package pt.ualg.mcv.family;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

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

import com.fasterxml.jackson.databind.ObjectMapper;

import pt.ualg.mcv.config.security.CustomUserDetailsService;
import pt.ualg.mcv.exceptions.NotFoundException;
import pt.ualg.mcv.family.dtos.FamilyMemberCreateDto;
import pt.ualg.mcv.family.dtos.FamilyMemberPatchDto;
import pt.ualg.mcv.family.dtos.FamilyMemberPhotoRetrieveDto;
import pt.ualg.mcv.family.dtos.FamilyMemberRetrieveDto;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FamilyMemberE2ETest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private MemberService memberService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private UUID memberUuid;
    private UUID photoUuid;
    private FamilyMemberRetrieveDto sampleRetrieveDto;

    @BeforeEach
    void setUp() {
        memberUuid = UUID.randomUUID();
        photoUuid = UUID.randomUUID();

        sampleRetrieveDto = new FamilyMemberRetrieveDto();
        sampleRetrieveDto.setUuid(memberUuid);
        sampleRetrieveDto.setFirstName("John");
        sampleRetrieveDto.setLastName("Doe");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/members - Should return 201 Created for valid ADMIN request")
    void createMember_Admin_Success() throws Exception {
        FamilyMemberCreateDto createDto = new FamilyMemberCreateDto();
        createDto.setFirstName("John");
        createDto.setLastName("Doe");

        when(memberService.createMember(any(), any())).thenReturn(sampleRetrieveDto);

        mockMvc.perform(post("/api/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.uuid").value(memberUuid.toString()))
                .andExpect(jsonPath("$.firstName").value("John"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("POST /api/members - Should return 403 Forbidden for non-ADMIN user")
    void createMember_ForbiddenForNonAdmin() throws Exception {
        FamilyMemberCreateDto createDto = new FamilyMemberCreateDto();
        createDto.setFirstName("John");
        createDto.setLastName("Doe");

        mockMvc.perform(post("/api/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/members - Should return 401 Unauthorized for unauthenticated request")
    void createMember_Unauthorized() throws Exception {
        FamilyMemberCreateDto createDto = new FamilyMemberCreateDto();
        createDto.setFirstName("John");
        createDto.setLastName("Doe");

        mockMvc.perform(post("/api/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/members - Should return 400 Bad Request when mandatory fields are blank")
    void createMember_ValidationError() throws Exception {
        FamilyMemberCreateDto createDto = new FamilyMemberCreateDto();
        createDto.setFirstName("");

        mockMvc.perform(post("/api/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("PATCH /api/members/{uuid} - Should return 200 OK for valid patch")
    void patchMember_Success() throws Exception {
        FamilyMemberPatchDto patchDto = new FamilyMemberPatchDto();
        patchDto.setFirstName("Jane");

        when(memberService.patchMember(eq(memberUuid), any())).thenReturn(sampleRetrieveDto);

        mockMvc.perform(patch("/api/members/{uuid}", memberUuid)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(patchDto)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("PATCH /api/members/{uuid} - Should return 403 Forbidden for non-ADMIN user")
    void patchMember_Forbidden() throws Exception {
        FamilyMemberPatchDto patchDto = new FamilyMemberPatchDto();
        patchDto.setFirstName("Jane");

        mockMvc.perform(patch("/api/members/{uuid}", memberUuid)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(patchDto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("PATCH /api/members/{uuid} - Should return 404 Not Found when member missing")
    void patchMember_NotFound() throws Exception {
        FamilyMemberPatchDto patchDto = new FamilyMemberPatchDto();

        when(memberService.patchMember(eq(memberUuid), any())).thenThrow(new NotFoundException("Member not found"));

        mockMvc.perform(patch("/api/members/{uuid}", memberUuid)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(patchDto)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/members/{uuid} - Should be publicly accessible and return 200 OK")
    void getMember_PublicAccess_Success() throws Exception {
        when(memberService.getMember(memberUuid)).thenReturn(sampleRetrieveDto);

        mockMvc.perform(get("/api/members/{uuid}", memberUuid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("John"));
    }

    @Test
    @DisplayName("GET /api/members/{uuid} - Should return 404 Not Found when member does not exist")
    void getMember_NotFound() throws Exception {
        when(memberService.getMember(memberUuid)).thenThrow(new NotFoundException("Member not found"));

        mockMvc.perform(get("/api/members/{uuid}", memberUuid))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/members - Should return paged list publicly")
    void getMembers_PublicAccess_Success() throws Exception {
        PageImpl<FamilyMemberRetrieveDto> page = new PageImpl<>(List.of(sampleRetrieveDto));
        when(memberService.getMembers(eq("John"), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/members")
                .param("name", "John")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].firstName").value("John"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("DELETE /api/members/{uuid} - Should return 204 No Content for ADMIN")
    void deleteMember_Success() throws Exception {
        doNothing().when(memberService).deleteMember(memberUuid);

        mockMvc.perform(delete("/api/members/{uuid}", memberUuid))
                .andExpect(status().isNoContent());

        verify(memberService).deleteMember(memberUuid);
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("DELETE /api/members/{uuid} - Should return 403 Forbidden for non-ADMIN user")
    void deleteMember_Forbidden() throws Exception {
        mockMvc.perform(delete("/api/members/{uuid}", memberUuid))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("DELETE /api/members/{uuid} - Should return 404 Not Found if missing")
    void deleteMember_NotFound() throws Exception {
        doThrow(new NotFoundException("Member not found")).when(memberService).deleteMember(memberUuid);

        mockMvc.perform(delete("/api/members/{uuid}", memberUuid))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/members/{memberUuid}/photos - Should upload multipart photo and return 201 Created")
    void addPhoto_Success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.jpg", MediaType.IMAGE_JPEG_VALUE, "image bytes".getBytes());

        FamilyMemberPhotoRetrieveDto photoDto = new FamilyMemberPhotoRetrieveDto();
        photoDto.setUuid(photoUuid);
        photoDto.setFilePath("http://localhost/uploads/avatar.jpg");

        when(memberService.addPhoto(eq(memberUuid), any(), any())).thenReturn(photoDto);

        mockMvc.perform(multipart("/api/members/{memberUuid}/photos", memberUuid)
                .file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.uuid").value(photoUuid.toString()))
                .andExpect(jsonPath("$.filePath").value("http://localhost/uploads/avatar.jpg"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("POST /api/members/{memberUuid}/photos - Should return 403 Forbidden for non-ADMIN user")
    void addPhoto_Forbidden() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.jpg", MediaType.IMAGE_JPEG_VALUE, "image bytes".getBytes());

        mockMvc.perform(multipart("/api/members/{memberUuid}/photos", memberUuid)
                .file(file))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("DELETE /api/members/{memberUuid}/photos/{photoUuid} - Should return 204 No Content on deletion")
    void removePhoto_Success() throws Exception {
        doNothing().when(memberService).removePhoto(memberUuid, photoUuid);

        mockMvc.perform(delete("/api/members/{memberUuid}/photos/{photoUuid}", memberUuid, photoUuid))
                .andExpect(status().isNoContent());

        verify(memberService).removePhoto(memberUuid, photoUuid);
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("DELETE /api/members/{memberUuid}/photos/{photoUuid} - Should return 403 Forbidden for non-ADMIN user")
    void removePhoto_Forbidden() throws Exception {
        mockMvc.perform(delete("/api/members/{memberUuid}/photos/{photoUuid}", memberUuid, photoUuid))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("DELETE /api/members/{memberUuid}/photos/{photoUuid} - Should return 404 if photo/member link does not exist")
    void removePhoto_NotFound() throws Exception {
        doThrow(new NotFoundException("Photo not found")).when(memberService).removePhoto(memberUuid, photoUuid);

        mockMvc.perform(delete("/api/members/{memberUuid}/photos/{photoUuid}", memberUuid, photoUuid))
                .andExpect(status().isNotFound());
    }
}