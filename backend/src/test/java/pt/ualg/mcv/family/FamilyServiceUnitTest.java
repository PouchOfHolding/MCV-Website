package pt.ualg.mcv.family;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

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

import pt.ualg.mcv.config.security.CustomUserDetails;
import pt.ualg.mcv.exceptions.ConflictException;
import pt.ualg.mcv.exceptions.NotFoundException;
import pt.ualg.mcv.family.dtos.FamilyAddMemberDto;
import pt.ualg.mcv.family.dtos.FamilyCreatePatchDto;
import pt.ualg.mcv.family.dtos.FamilyMemberRetrieveDto;
import pt.ualg.mcv.family.dtos.FamilyRetrieveDto;
import pt.ualg.mcv.user.User;

@ExtendWith(MockitoExtension.class)
class FamilyServiceTest
{

    @Mock
    private FamilyRepository familyRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private FamilyHasMemberRepository familyHasMemberRepository;

    @InjectMocks
    private FamilyService familyService;

    private UUID familyUuid;
    private UUID memberUuid;
    private Family family;
    private Member member;
    private User user;
    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp()
    {
        familyUuid = UUID.randomUUID();
        memberUuid = UUID.randomUUID();

        user = new User();
        user.setUuid(UUID.randomUUID());
        user.setEmail("admin@ualg.pt");

        userDetails = mock(CustomUserDetails.class);

        family = new Family();
        family.setUuid(familyUuid);
        family.setYear(2026L);
        family.setCreatedBy(user);

        member = new Member();
        member.setUuid(memberUuid);
        member.setFirstName("John");
        member.setLastName("Doe");
    }

    @Test
    @DisplayName("createFamily - Should create and return FamilyRetrieveDto successfully")
    void createFamily_Success()
    {
        FamilyCreatePatchDto dto = new FamilyCreatePatchDto();
        dto.setYear(2026L);

        when(userDetails.getUser()).thenReturn(user);
        when(familyRepository.save(any(Family.class))).thenAnswer(invocation -> {
            Family saved = invocation.getArgument(0);
            saved.setUuid(familyUuid);
            return saved;
        });

        FamilyRetrieveDto result = familyService.createFamily(dto, userDetails);

        assertThat(result).isNotNull();
        assertThat(result.getYear()).isEqualTo(2026L);
        verify(familyRepository).save(any(Family.class));
    }

    @Test
    @DisplayName("patchFamily - Should update family year when present")
    void patchFamily_Success()
    {
        FamilyCreatePatchDto patchDto = new FamilyCreatePatchDto();
        patchDto.setYear(2027L);

        when(familyRepository.findById(familyUuid)).thenReturn(Optional.of(family));
        when(familyRepository.save(any(Family.class))).thenReturn(family);

        FamilyRetrieveDto result = familyService.patchFamily(familyUuid, patchDto);

        assertThat(result.getYear()).isEqualTo(2027L);
        verify(familyRepository).save(family);
    }

    @Test
    @DisplayName("patchFamily - Should keep existing family year when patch field is null")
    void patchFamily_NullYear_KeepsExistingValue()
    {
        FamilyCreatePatchDto patchDto = new FamilyCreatePatchDto();

        when(familyRepository.findById(familyUuid)).thenReturn(Optional.of(family));
        when(familyRepository.save(any(Family.class))).thenReturn(family);

        FamilyRetrieveDto result = familyService.patchFamily(familyUuid, patchDto);

        assertThat(result.getYear()).isEqualTo(2026L);
    }

    @Test
    @DisplayName("patchFamily - Should throw NotFoundException when family does not exist")
    void patchFamily_NotFound()
    {
        FamilyCreatePatchDto patchDto = new FamilyCreatePatchDto();
        when(familyRepository.findById(familyUuid)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> familyService.patchFamily(familyUuid, patchDto))
                .isInstanceOf(NotFoundException.class);

        verify(familyRepository, never()).save(any());
    }

    @Test
    @DisplayName("getFamily - Should return FamilyRetrieveDto when family exists")
    void getFamily_Success()
    {
        when(familyRepository.findById(familyUuid)).thenReturn(Optional.of(family));

        FamilyRetrieveDto result = familyService.getFamily(familyUuid);

        assertThat(result.getUuid()).isEqualTo(familyUuid);
        assertThat(result.getYear()).isEqualTo(2026L);
    }

    @Test
    @DisplayName("getFamily - Should throw NotFoundException when family missing")
    void getFamily_NotFound()
    {
        when(familyRepository.findById(familyUuid)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> familyService.getFamily(familyUuid))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("getFamilies - Should return paged list of families")
    void getFamilies_Success()
    {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Family> page = new PageImpl<>(List.of(family), pageable, 1);

        when(familyRepository.findAll(pageable)).thenReturn(page);

        Page<FamilyRetrieveDto> result = familyService.getFamilies(null, null, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getYear()).isEqualTo(2026L);
    }

    @Test
    @DisplayName("getFamilies - Should filter by both minYear and maxYear when both are provided")
    void getFamilies_WithMinAndMaxYear_Success()
    {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Family> familyPage = new PageImpl<>(List.of(family), pageable, 1);

        when(familyRepository.findByYearBetween(2020L, 2026L, pageable)).thenReturn(familyPage);

        Page<FamilyRetrieveDto> result = familyService.getFamilies(2020L, 2026L, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(familyRepository).findByYearBetween(2020L, 2026L, pageable);
        verify(familyRepository, never()).findAll(pageable);
    }

    @Test
    @DisplayName("getFamilies - Should filter by minYear only when maxYear is null")
    void getFamilies_WithMinYearOnly_Success()
    {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Family> familyPage = new PageImpl<>(List.of(family), pageable, 1);

        when(familyRepository.findByYearBetween(2020L, null, pageable)).thenReturn(familyPage);

        Page<FamilyRetrieveDto> result = familyService.getFamilies(2020L, null, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(familyRepository).findByYearBetween(2020L, null, pageable);
    }

    @Test
    @DisplayName("getFamilies - Should filter by maxYear only when minYear is null")
    void getFamilies_WithMaxYearOnly_Success()
    {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Family> familyPage = new PageImpl<>(List.of(family), pageable, 1);

        when(familyRepository.findByYearBetween(null, 2026L, pageable)).thenReturn(familyPage);

        Page<FamilyRetrieveDto> result = familyService.getFamilies(null, 2026L, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(familyRepository).findByYearBetween(null, 2026L, pageable);
    }

    @Test
    @DisplayName("deleteFamily - Should delete family successfully")
    void deleteFamily_Success()
    {
        when(familyRepository.findById(familyUuid)).thenReturn(Optional.of(family));

        familyService.deleteFamily(familyUuid);

        verify(familyRepository).delete(family);
    }

    @Test
    @DisplayName("deleteFamily - Should throw NotFoundException when deleting non-existent family")
    void deleteFamily_NotFound()
    {
        when(familyRepository.findById(familyUuid)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> familyService.deleteFamily(familyUuid))
                .isInstanceOf(NotFoundException.class);

        verify(familyRepository, never()).delete(any());
    }

    @Test
    @DisplayName("addMemberToFamily - Should associate member to family successfully")
    void addMemberToFamily_Success()
    {
        FamilyAddMemberDto dto = new FamilyAddMemberDto();
        dto.setRole(FamilyMemberRole.DUX);

        FamilyHasMemberId id = new FamilyHasMemberId(familyUuid, memberUuid);

        when(familyRepository.findById(familyUuid)).thenReturn(Optional.of(family));
        when(memberRepository.findById(memberUuid)).thenReturn(Optional.of(member));
        when(familyHasMemberRepository.existsById(id)).thenReturn(false);
        when(familyHasMemberRepository.countByFamilyUuidAndRole(familyUuid, FamilyMemberRole.DUX)).thenReturn(0L);

        FamilyMemberRetrieveDto result =
                familyService.addMemberToFamily(familyUuid, memberUuid, dto);

        assertThat(result).isNotNull();
        assertThat(result.getFirstName()).isEqualTo("John");
        verify(familyHasMemberRepository).save(any(FamilyHasMember.class));
    }

    @Test
    @DisplayName("addMemberToFamily - Should throw ConflictException if DUX limit (1) is exceeded")
    void addMemberToFamily_DuxCapacityExceeded_ThrowsConflict()
    {
        FamilyAddMemberDto dto = new FamilyAddMemberDto();
        dto.setRole(FamilyMemberRole.DUX);

        FamilyHasMemberId id = new FamilyHasMemberId(familyUuid, memberUuid);

        when(familyRepository.findById(familyUuid)).thenReturn(Optional.of(family));
        when(memberRepository.findById(memberUuid)).thenReturn(Optional.of(member));
        when(familyHasMemberRepository.existsById(id)).thenReturn(false);
        when(familyHasMemberRepository.countByFamilyUuidAndRole(familyUuid, FamilyMemberRole.DUX)).thenReturn(1L);

        assertThatThrownBy(() -> familyService.addMemberToFamily(familyUuid, memberUuid, dto))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("maximum allowed DUX (1)");

        verify(familyHasMemberRepository, never()).save(any());
    }

    @Test
    @DisplayName("addMemberToFamily - Should throw ConflictException if FACULTIS limit (8) is exceeded")
    void addMemberToFamily_FacultisCapacityExceeded_ThrowsConflict()
    {
        FamilyAddMemberDto dto = new FamilyAddMemberDto();
        dto.setRole(FamilyMemberRole.FACULTIS);

        FamilyHasMemberId id = new FamilyHasMemberId(familyUuid, memberUuid);

        when(familyRepository.findById(familyUuid)).thenReturn(Optional.of(family));
        when(memberRepository.findById(memberUuid)).thenReturn(Optional.of(member));
        when(familyHasMemberRepository.existsById(id)).thenReturn(false);
        when(familyHasMemberRepository.countByFamilyUuidAndRole(familyUuid, FamilyMemberRole.FACULTIS)).thenReturn(8L);

        assertThatThrownBy(() -> familyService.addMemberToFamily(familyUuid, memberUuid, dto))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("maximum allowed FACULTIS (8)");

        verify(familyHasMemberRepository, never()).save(any());
    }

    @Test
    @DisplayName("addMemberToFamily - Should allow unlimited ADVISOR roles")
    void addMemberToFamily_AdvisorRole_Unlimited()
    {
        FamilyAddMemberDto dto = new FamilyAddMemberDto();
        dto.setRole(FamilyMemberRole.ADVISOR);

        FamilyHasMemberId id = new FamilyHasMemberId(familyUuid, memberUuid);

        when(familyRepository.findById(familyUuid)).thenReturn(Optional.of(family));
        when(memberRepository.findById(memberUuid)).thenReturn(Optional.of(member));
        when(familyHasMemberRepository.existsById(id)).thenReturn(false);

        FamilyMemberRetrieveDto result = familyService.addMemberToFamily(familyUuid, memberUuid, dto);

        assertThat(result).isNotNull();
        verify(familyHasMemberRepository).save(any(FamilyHasMember.class));
    }

    @Test
    @DisplayName("addMemberToFamily - Should throw ConflictException if association already exists")
    void addMemberToFamily_AlreadyExists_ThrowsConflict()
    {
        FamilyAddMemberDto dto = new FamilyAddMemberDto();
        FamilyHasMemberId id = new FamilyHasMemberId(familyUuid, memberUuid);

        when(familyRepository.findById(familyUuid)).thenReturn(Optional.of(family));
        when(memberRepository.findById(memberUuid)).thenReturn(Optional.of(member));
        when(familyHasMemberRepository.existsById(id)).thenReturn(true);

        assertThatThrownBy(() -> familyService.addMemberToFamily(familyUuid, memberUuid, dto))
                .isInstanceOf(ConflictException.class);

        verify(familyHasMemberRepository, never()).save(any());
    }

    @Test
    @DisplayName("addMemberToFamily - Should throw NotFoundException if family does not exist")
    void addMemberToFamily_FamilyNotFound()
    {
        FamilyAddMemberDto dto = new FamilyAddMemberDto();
        when(familyRepository.findById(familyUuid)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> familyService.addMemberToFamily(familyUuid, memberUuid, dto))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("addMemberToFamily - Should throw NotFoundException if member does not exist")
    void addMemberToFamily_MemberNotFound()
    {
        FamilyAddMemberDto dto = new FamilyAddMemberDto();
        when(familyRepository.findById(familyUuid)).thenReturn(Optional.of(family));
        when(memberRepository.findById(memberUuid)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> familyService.addMemberToFamily(familyUuid, memberUuid, dto))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("updateMemberRoleInFamily - Should update role successfully when capacity allows")
    void updateMemberRoleInFamily_Success()
    {
        FamilyAddMemberDto dto = new FamilyAddMemberDto();
        dto.setRole(FamilyMemberRole.FACULTIS);

        FamilyHasMemberId id = new FamilyHasMemberId(familyUuid, memberUuid);
        FamilyHasMember fhm = new FamilyHasMember();
        fhm.setId(id);
        fhm.setFamily(family);
        fhm.setMember(member);
        fhm.setRole(FamilyMemberRole.ADVISOR);

        when(familyRepository.existsById(familyUuid)).thenReturn(true);
        when(memberRepository.existsById(memberUuid)).thenReturn(true);
        when(familyHasMemberRepository.findById(id)).thenReturn(Optional.of(fhm));
        when(familyHasMemberRepository.countByFamilyUuidAndRole(familyUuid, FamilyMemberRole.FACULTIS)).thenReturn(5L);

        FamilyMemberRetrieveDto result = familyService.updateMemberRoleInFamily(familyUuid, memberUuid, dto);

        assertThat(result).isNotNull();
        assertThat(fhm.getRole()).isEqualTo(FamilyMemberRole.FACULTIS);
        verify(familyHasMemberRepository).save(fhm);
    }

    @Test
    @DisplayName("updateMemberRoleInFamily - Should throw ConflictException when target role capacity is reached")
    void updateMemberRoleInFamily_CapacityExceeded_ThrowsConflict()
    {
        FamilyAddMemberDto dto = new FamilyAddMemberDto();
        dto.setRole(FamilyMemberRole.DUX);

        FamilyHasMemberId id = new FamilyHasMemberId(familyUuid, memberUuid);
        FamilyHasMember fhm = new FamilyHasMember();
        fhm.setId(id);
        fhm.setFamily(family);
        fhm.setMember(member);
        fhm.setRole(FamilyMemberRole.ADVISOR);

        when(familyRepository.existsById(familyUuid)).thenReturn(true);
        when(memberRepository.existsById(memberUuid)).thenReturn(true);
        when(familyHasMemberRepository.findById(id)).thenReturn(Optional.of(fhm));
        when(familyHasMemberRepository.countByFamilyUuidAndRole(familyUuid, FamilyMemberRole.DUX)).thenReturn(1L);

        assertThatThrownBy(() -> familyService.updateMemberRoleInFamily(familyUuid, memberUuid, dto))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("maximum allowed DUX (1)");
    }

    @Test
    @DisplayName("updateMemberRoleInFamily - Should throw NotFoundException when association missing")
    void updateMemberRoleInFamily_NotFound()
    {
        FamilyAddMemberDto dto = new FamilyAddMemberDto();
        dto.setRole(FamilyMemberRole.FACULTIS);
        FamilyHasMemberId id = new FamilyHasMemberId(familyUuid, memberUuid);

        when(familyRepository.existsById(familyUuid)).thenReturn(true);
        when(memberRepository.existsById(memberUuid)).thenReturn(true);
        when(familyHasMemberRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> familyService.updateMemberRoleInFamily(familyUuid, memberUuid, dto))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("removeMemberFromFamily - Should remove member from family successfully")
    void removeMemberFromFamily_Success()
    {
        FamilyHasMemberId id = new FamilyHasMemberId(familyUuid, memberUuid);
        FamilyHasMember fhm = new FamilyHasMember();

        when(familyRepository.existsById(familyUuid)).thenReturn(true);
        when(memberRepository.existsById(memberUuid)).thenReturn(true);
        when(familyHasMemberRepository.findById(id)).thenReturn(Optional.of(fhm));

        familyService.removeMemberFromFamily(familyUuid, memberUuid);

        verify(familyHasMemberRepository).delete(fhm);
    }

    @Test
    @DisplayName("removeMemberFromFamily - Should throw NotFoundException if family or member IDs do not exist")
    void removeMemberFromFamily_EntityMissing()
    {
        when(familyRepository.existsById(familyUuid)).thenReturn(false);

        assertThatThrownBy(() -> familyService.removeMemberFromFamily(familyUuid, memberUuid))
                .isInstanceOf(NotFoundException.class);

        verify(familyHasMemberRepository, never()).delete(any());
    }

    @Test
    @DisplayName("listMembersOfFamily - Should return paged list of family members")
    void listMembersOfFamily_Success()
    {
        Pageable pageable = PageRequest.of(0, 10);
        FamilyHasMember fhm = new FamilyHasMember();
        fhm.setMember(member);

        Page<FamilyHasMember> fhmPage = new PageImpl<>(List.of(fhm), pageable, 1);

        when(familyRepository.existsById(familyUuid)).thenReturn(true);
        when(familyHasMemberRepository.findByFamilyUuid(familyUuid, pageable)).thenReturn(fhmPage);

        Page<FamilyMemberRetrieveDto> result =
                familyService.listMembersOfFamily(familyUuid, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getFirstName()).isEqualTo("John");
    }

    @Test
    @DisplayName("listMembersOfFamily - Should throw NotFoundException when family does not exist")
    void listMembersOfFamily_FamilyNotFound()
    {
        Pageable pageable = PageRequest.of(0, 10);
        when(familyRepository.existsById(familyUuid)).thenReturn(false);

        assertThatThrownBy(() -> familyService.listMembersOfFamily(familyUuid, pageable))
                .isInstanceOf(NotFoundException.class);
    }
}