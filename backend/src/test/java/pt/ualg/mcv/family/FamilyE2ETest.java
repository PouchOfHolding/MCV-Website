package pt.ualg.mcv.family;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import pt.ualg.mcv.config.security.CustomUserDetailsService;
import pt.ualg.mcv.exceptions.ConflictException;
import pt.ualg.mcv.exceptions.NotFoundException;
import pt.ualg.mcv.family.dtos.FamilyAddMemberDto;
import pt.ualg.mcv.family.dtos.FamilyCreatePatchDto;
import pt.ualg.mcv.family.dtos.FamilyMemberRetrieveDto;
import pt.ualg.mcv.family.dtos.FamilyRetrieveDto;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FamilyControllerE2ETest
{

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private FamilyService familyService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private UUID familyUuid;
    private UUID memberUuid;
    private FamilyRetrieveDto sampleFamilyDto;
    private FamilyMemberRetrieveDto sampleMemberDto;

    @BeforeEach
    void setUp()
    {
        familyUuid = UUID.randomUUID();
        memberUuid = UUID.randomUUID();

        sampleFamilyDto = new FamilyRetrieveDto();
        sampleFamilyDto.setUuid(familyUuid);
        sampleFamilyDto.setYear(2026L);

        sampleMemberDto = new FamilyMemberRetrieveDto();
        sampleMemberDto.setUuid(memberUuid);
        sampleMemberDto.setFirstName("John");
        sampleMemberDto.setLastName("Doe");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/families - Should return 201 Created for ADMIN")
    void createFamily_Admin_Success() throws Exception
    {
        FamilyCreatePatchDto createDto = new FamilyCreatePatchDto();
        createDto.setYear(2026L);

        when(familyService.createFamily(any(), any())).thenReturn(sampleFamilyDto);

        mockMvc.perform(post("/api/families").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.uuid").value(familyUuid.toString()))
                .andExpect(jsonPath("$.year").value(2026));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("POST /api/families - Should return 403 Forbidden for non-ADMIN user")
    void createFamily_ForbiddenForUser() throws Exception
    {
        FamilyCreatePatchDto createDto = new FamilyCreatePatchDto();
        createDto.setYear(2026L);

        mockMvc.perform(post("/api/families").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createFamily_Unauthorized() throws Exception
    {
        FamilyCreatePatchDto createDto = new FamilyCreatePatchDto();
        createDto.setYear(2026L);

        mockMvc.perform(post("/api/families").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("PATCH /api/families/{uuid} - Should return 200 OK for ADMIN")
    void patchFamily_Admin_Success() throws Exception
    {
        FamilyCreatePatchDto patchDto = new FamilyCreatePatchDto();
        patchDto.setYear(2027L);

        sampleFamilyDto.setYear(2027L);
        when(familyService.patchFamily(eq(familyUuid), any())).thenReturn(sampleFamilyDto);

        mockMvc.perform(
                patch("/api/families/{uuid}", familyUuid).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patchDto)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.year").value(2027));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("PATCH /api/families/{uuid} - Should return 404 Not Found when missing")
    void patchFamily_NotFound() throws Exception
    {
        FamilyCreatePatchDto patchDto = new FamilyCreatePatchDto();
        when(familyService.patchFamily(eq(familyUuid), any()))
                .thenThrow(new NotFoundException("Family not found"));

        mockMvc.perform(
                patch("/api/families/{uuid}", familyUuid).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patchDto)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/families - Should return paged results publicly")
    void getFamilies_PublicSuccess() throws Exception
    {
        PageImpl<FamilyRetrieveDto> page = new PageImpl<>(List.of(sampleFamilyDto));
        when(familyService.getFamilies(eq(null), eq(null), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/families").param("page", "0").param("size", "10"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].year").value(2026));
    }

    @Test
    @DisplayName("GET /api/families?minYear=2020&maxYear=2026 - Should return families within year range")
    void getFamilies_WithYearRangeFilter_Success() throws Exception
    {
        PageImpl<FamilyRetrieveDto> page = new PageImpl<>(List.of(sampleFamilyDto));
        when(familyService.getFamilies(eq(2020L), eq(2026L), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/families").param("minYear", "2020").param("maxYear", "2026")
                .param("page", "0").param("size", "10")).andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].year").value(2026));
    }

    @Test
    @DisplayName("GET /api/families?minYear=2020 - Should return families starting from minYear")
    void getFamilies_WithMinYearOnly_Success() throws Exception
    {
        PageImpl<FamilyRetrieveDto> page = new PageImpl<>(List.of(sampleFamilyDto));
        when(familyService.getFamilies(eq(2020L), eq(null), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/families").param("minYear", "2020").param("page", "0")
                .param("size", "10")).andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].year").value(2026));
    }

    @Test
    @DisplayName("GET /api/families?maxYear=2026 - Should return families up to maxYear")
    void getFamilies_WithMaxYearOnly_Success() throws Exception
    {
        PageImpl<FamilyRetrieveDto> page = new PageImpl<>(List.of(sampleFamilyDto));
        when(familyService.getFamilies(eq(null), eq(2026L), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/families").param("maxYear", "2026").param("page", "0")
                .param("size", "10")).andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].year").value(2026));
    }

    @Test
    @DisplayName("GET /api/families/{uuid} - Should return 404 Not Found")
    void getFamily_NotFound() throws Exception
    {
        when(familyService.getFamily(familyUuid))
                .thenThrow(new NotFoundException("Family not found"));

        mockMvc.perform(get("/api/families/{uuid}", familyUuid)).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("DELETE /api/families/{uuid} - Should return 204 No Content for ADMIN")
    void deleteFamily_Success() throws Exception
    {
        doNothing().when(familyService).deleteFamily(familyUuid);

        mockMvc.perform(delete("/api/families/{uuid}", familyUuid))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("DELETE /api/families/{uuid} - Should return 404 Not Found")
    void deleteFamily_NotFound() throws Exception
    {
        doThrow(new NotFoundException("Family not found")).when(familyService)
                .deleteFamily(familyUuid);

        mockMvc.perform(delete("/api/families/{uuid}", familyUuid))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/families/{fUuid}/members/{mUuid} - Should associate member and return 201 Created")
    void addMemberToFamily_Success() throws Exception
    {
        FamilyAddMemberDto addDto = new FamilyAddMemberDto();
        addDto.setRole(FamilyMemberRole.DUX);

        when(familyService.addMemberToFamily(eq(familyUuid), eq(memberUuid), any()))
                .thenReturn(sampleMemberDto);

        mockMvc.perform(
                post("/api/families/{familyUuid}/members/{memberUuid}", familyUuid, memberUuid)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addDto)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.firstName").value("John"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/families/{fUuid}/members/{mUuid} - Should return 409 Conflict if link already exists or capacity reached")
    void addMemberToFamily_Conflict() throws Exception
    {
        FamilyAddMemberDto addDto = new FamilyAddMemberDto();
        addDto.setRole(FamilyMemberRole.DUX);

        when(familyService.addMemberToFamily(eq(familyUuid), eq(memberUuid), any()))
                .thenThrow(new ConflictException("Family already has maximum allowed DUX (1)"));

        mockMvc.perform(
                post("/api/families/{familyUuid}/members/{memberUuid}", familyUuid, memberUuid)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addDto)))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("PATCH /api/families/{fUuid}/members/{mUuid} - Should return 200 OK when updating member role")
    void updateMemberRoleInFamily_Success() throws Exception
    {
        FamilyAddMemberDto updateDto = new FamilyAddMemberDto();
        updateDto.setRole(FamilyMemberRole.FACULTIS);

        when(familyService.updateMemberRoleInFamily(eq(familyUuid), eq(memberUuid), any()))
                .thenReturn(sampleMemberDto);

        mockMvc.perform(
                patch("/api/families/{familyUuid}/members/{memberUuid}", familyUuid, memberUuid)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("John"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("PATCH /api/families/{fUuid}/members/{mUuid} - Should return 409 Conflict when new role exceeds capacity")
    void updateMemberRoleInFamily_CapacityExceeded_Conflict() throws Exception
    {
        FamilyAddMemberDto updateDto = new FamilyAddMemberDto();
        updateDto.setRole(FamilyMemberRole.FACULTIS);

        when(familyService.updateMemberRoleInFamily(eq(familyUuid), eq(memberUuid), any()))
                .thenThrow(new ConflictException("Family already has maximum allowed FACULTIS (8)"));

        mockMvc.perform(
                patch("/api/families/{familyUuid}/members/{memberUuid}", familyUuid, memberUuid)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("PATCH /api/families/{fUuid}/members/{mUuid} - Should return 404 Not Found when association missing")
    void updateMemberRoleInFamily_NotFound() throws Exception
    {
        FamilyAddMemberDto updateDto = new FamilyAddMemberDto();
        updateDto.setRole(FamilyMemberRole.ADVISOR);

        when(familyService.updateMemberRoleInFamily(eq(familyUuid), eq(memberUuid), any()))
                .thenThrow(new NotFoundException("Association not found"));

        mockMvc.perform(
                patch("/api/families/{familyUuid}/members/{memberUuid}", familyUuid, memberUuid)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("DELETE /api/families/{fUuid}/members/{mUuid} - Should return 204 No Content on successful removal")
    void removeMemberFromFamily_Success() throws Exception
    {
        doNothing().when(familyService).removeMemberFromFamily(familyUuid, memberUuid);

        mockMvc.perform(
                delete("/api/families/{familyUuid}/members/{memberUuid}", familyUuid, memberUuid))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("DELETE /api/families/{fUuid}/members/{mUuid} - Should return 404 Not Found if link missing")
    void removeMemberFromFamily_NotFound() throws Exception
    {
        doThrow(new NotFoundException("Link not found")).when(familyService)
                .removeMemberFromFamily(familyUuid, memberUuid);

        mockMvc.perform(
                delete("/api/families/{familyUuid}/members/{memberUuid}", familyUuid, memberUuid))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/families/{fUuid}/members - Should return paged family members publicly")
    void listMembersOfFamily_PublicSuccess() throws Exception
    {
        PageImpl<FamilyMemberRetrieveDto> page = new PageImpl<>(List.of(sampleMemberDto));
        when(familyService.listMembersOfFamily(eq(familyUuid), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/families/{familyUuid}/members", familyUuid).param("page", "0")
                .param("size", "10")).andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].firstName").value("John"));
    }

    @Test
    @DisplayName("GET /api/families/{fUuid}/members - Should return 404 Not Found when family missing")
    void listMembersOfFamily_NotFound() throws Exception
    {
        when(familyService.listMembersOfFamily(eq(familyUuid), any(Pageable.class)))
                .thenThrow(new NotFoundException("Family not found"));

        mockMvc.perform(get("/api/families/{familyUuid}/members", familyUuid))
                .andExpect(status().isNotFound());
    }
}