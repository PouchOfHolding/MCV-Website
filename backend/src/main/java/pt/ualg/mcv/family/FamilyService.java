package pt.ualg.mcv.family;

import java.nio.file.Paths;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import lombok.RequiredArgsConstructor;
import pt.ualg.mcv.config.security.CustomUserDetails;
import pt.ualg.mcv.exceptions.ConflictException;
import pt.ualg.mcv.exceptions.NotFoundException;
import pt.ualg.mcv.family.dtos.FamilyAddMemberDto;
import pt.ualg.mcv.family.dtos.FamilyCreatePatchDto;
import pt.ualg.mcv.family.dtos.FamilyMemberPhotoRetrieveDto;
import pt.ualg.mcv.family.dtos.FamilyMemberRetrieveDto;
import pt.ualg.mcv.family.dtos.FamilyRetrieveDto;

@Service
@RequiredArgsConstructor
public class FamilyService
{

    private final FamilyRepository familyRepository;
    private final MemberRepository memberRepository;
    private final FamilyHasMemberRepository familyHasMemberRepository;

    @Transactional
    public FamilyRetrieveDto createFamily(FamilyCreatePatchDto familyMemberCreateDto,
            CustomUserDetails userDetails)
    {
        Family family = new Family();
        family.setYear(familyMemberCreateDto.getYear());
        family.setCreatedBy(userDetails.getUser());

        familyRepository.save(family);

        return familyToFamilyRetrieveDto(family);
    }

    @Transactional
    public FamilyRetrieveDto patchFamily(UUID uuid, FamilyCreatePatchDto familyMemberPatchDto)
    {
        Family family = familyRepository.findById(uuid).orElseThrow(NotFoundException::new);

        if (familyMemberPatchDto.getYear() != null)
        {
            family.setYear(familyMemberPatchDto.getYear());
        }

        familyRepository.save(family);

        return familyToFamilyRetrieveDto(family);
    }

    @Transactional(readOnly = true)
    public FamilyRetrieveDto getFamily(UUID uuid)
    {
        Family family = familyRepository.findById(uuid).orElseThrow(NotFoundException::new);
        return familyToFamilyRetrieveDto(family);
    }

    @Transactional(readOnly = true)
    public Page<FamilyRetrieveDto> getFamilies(Long minYear, Long maxYear, Pageable pageable)
    {
        Page<Family> families;
        if (minYear != null || maxYear != null)
        {
            families = familyRepository.findByYearBetween(minYear, maxYear, pageable);
        }
        else
        {
            families = familyRepository.findAll(pageable);
        }
        return families.map(this::familyToFamilyRetrieveDto);
    }

    @Transactional
    public void deleteFamily(UUID uuid)
    {
        Family family = familyRepository.findById(uuid).orElseThrow(NotFoundException::new);
        familyRepository.delete(family);
    }

    @Transactional
    public FamilyMemberRetrieveDto addMemberToFamily(UUID familyUuid, UUID memberUuid,
            FamilyAddMemberDto familyAddMemberDto)
    {
        Family family = familyRepository.findById(familyUuid).orElseThrow(NotFoundException::new);
        Member member = memberRepository.findById(memberUuid).orElseThrow(NotFoundException::new);

        if (familyHasMemberRepository.existsById(new FamilyHasMemberId(familyUuid, memberUuid)))
        {
            throw new ConflictException("Member already belongs to this family");
        }

        validateRoleCapacity(familyUuid, familyAddMemberDto.getRole(), null);

        FamilyHasMember familyHasMember = new FamilyHasMember();
        familyHasMember.setId(new FamilyHasMemberId(familyUuid, memberUuid));
        familyHasMember.setFamily(family);
        familyHasMember.setMember(member);
        familyHasMember.setRole(familyAddMemberDto.getRole());

        familyHasMemberRepository.save(familyHasMember);

        return memberToFamilyMemberDto(member);
    }

    @Transactional
    public FamilyMemberRetrieveDto updateMemberRoleInFamily(UUID familyUuid, UUID memberUuid,
            FamilyAddMemberDto familyAddMemberDto)
    {
        if (!familyRepository.existsById(familyUuid) || !memberRepository.existsById(memberUuid))
        {
            throw new NotFoundException();
        }

        FamilyHasMember familyHasMember =
                familyHasMemberRepository.findById(new FamilyHasMemberId(familyUuid, memberUuid))
                        .orElseThrow(NotFoundException::new);

        if (familyHasMember.getRole() != familyAddMemberDto.getRole())
        {
            validateRoleCapacity(familyUuid, familyAddMemberDto.getRole(), familyHasMember.getRole());
            familyHasMember.setRole(familyAddMemberDto.getRole());
            familyHasMemberRepository.save(familyHasMember);
        }

        return memberToFamilyMemberDto(familyHasMember.getMember());
    }

    @Transactional
    public void removeMemberFromFamily(UUID familyUuid, UUID memberUuid)
    {
        if (!familyRepository.existsById(familyUuid) || !memberRepository.existsById(memberUuid))
        {
            throw new NotFoundException();
        }

        FamilyHasMember familyHasMember =
                familyHasMemberRepository.findById(new FamilyHasMemberId(familyUuid, memberUuid))
                        .orElseThrow(NotFoundException::new);

        familyHasMemberRepository.delete(familyHasMember);
    }

    @Transactional(readOnly = true)
    public Page<FamilyMemberRetrieveDto> listMembersOfFamily(UUID familyUuid, Pageable pageable)
    {
        if (!familyRepository.existsById(familyUuid))
        {
            throw new NotFoundException();
        }

        Page<FamilyHasMember> page =
                familyHasMemberRepository.findByFamilyUuid(familyUuid, pageable);
        return page.map(fhm -> memberToFamilyMemberDto(fhm.getMember()));
    }

    private void validateRoleCapacity(UUID familyUuid, FamilyMemberRole targetRole, FamilyMemberRole currentRole)
    {
        if (targetRole == FamilyMemberRole.DUX)
        {
            long duxCount = familyHasMemberRepository.countByFamilyUuidAndRole(familyUuid, FamilyMemberRole.DUX);
            if (duxCount >= 1 && currentRole != FamilyMemberRole.DUX)
            {
                throw new ConflictException("Family already has maximum allowed DUX (1)");
            }
        }
        else if (targetRole == FamilyMemberRole.FACULTIS)
        {
            long facultisCount = familyHasMemberRepository.countByFamilyUuidAndRole(familyUuid, FamilyMemberRole.FACULTIS);
            if (facultisCount >= 8 && currentRole != FamilyMemberRole.FACULTIS)
            {
                throw new ConflictException("Family already has maximum allowed FACULTIS (8)");
            }
        }
    }

    private FamilyRetrieveDto familyToFamilyRetrieveDto(Family family)
    {
        FamilyRetrieveDto dto = new FamilyRetrieveDto();
        dto.setCreatedAt(family.getCreatedAt());
        dto.setUpdatedAt(family.getUpdatedAt());
        dto.setUuid(family.getUuid());
        dto.setYear(family.getYear());
        return dto;
    }

    private FamilyMemberRetrieveDto memberToFamilyMemberDto(Member member)
    {
        FamilyMemberRetrieveDto dto = new FamilyMemberRetrieveDto();
        dto.setCreatedAt(member.getCreatedAt());
        dto.setFirstName(member.getFirstName());
        dto.setLastName(member.getLastName());
        dto.setUpdatedAt(member.getUpdatedAt());
        dto.setUuid(member.getUuid());

        if (member.getPhotos() != null)
        {
            dto.setPhotos(member.getPhotos().stream()
                    .map(this::memberPhotoToFamilyMemberPhotoRetrieveDto).toList());
        }

        return dto;
    }

    private FamilyMemberPhotoRetrieveDto memberPhotoToFamilyMemberPhotoRetrieveDto(
            MemberPhoto photo)
    {
        FamilyMemberPhotoRetrieveDto dto = new FamilyMemberPhotoRetrieveDto();
        dto.setCreatedAt(photo.getCreatedAt());
        dto.setMimeType(photo.getMimeType());
        dto.setUuid(photo.getUuid());

        boolean hasRequestContext = org.springframework.web.context.request.RequestContextHolder
                .getRequestAttributes() != null;

        if (photo.getFilePath() != null)
        {
            String fileName = Paths.get(photo.getFilePath()).getFileName().toString();
            String fileUrl = hasRequestContext
                    ? ServletUriComponentsBuilder.fromCurrentContextPath().path("/uploads/")
                            .path(fileName).toUriString()
                    : "/uploads/" + fileName;
            dto.setFilePath(fileUrl);
        }

        if (photo.getThumbnailPath() != null)
        {
            String thumbName = Paths.get(photo.getThumbnailPath()).getFileName().toString();
            String thumbnailUrl = hasRequestContext
                    ? ServletUriComponentsBuilder.fromCurrentContextPath().path("/uploads/")
                            .path(thumbName).toUriString()
                    : "/uploads/" + thumbName;
            dto.setThumbnailPath(thumbnailUrl);
        }

        return dto;
    }
}