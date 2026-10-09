package pt.ualg.mcv.family;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import lombok.RequiredArgsConstructor;
import pt.ualg.mcv.common.services.FileStorageService;
import pt.ualg.mcv.common.services.FileUrlService;
import pt.ualg.mcv.config.security.CustomUserDetails;
import pt.ualg.mcv.exceptions.NotFoundException;
import pt.ualg.mcv.family.dtos.FamilyMemberCreateDto;
import pt.ualg.mcv.family.dtos.FamilyMemberPatchDto;
import pt.ualg.mcv.family.dtos.FamilyMemberPhotoRetrieveDto;
import pt.ualg.mcv.family.dtos.FamilyMemberRetrieveDto;

@Service
@RequiredArgsConstructor
public class MemberService
{

    private final MemberRepository memberRepository;
    private final MemberPhotoRepository memberPhotoRepository;
    private final FileStorageService fileStorageService;
    private final FileUrlService fileUrlService;

    @Transactional
    public FamilyMemberRetrieveDto createMember(FamilyMemberCreateDto familyMemberCreateDto,
            CustomUserDetails userDetails)
    {

        Member member = new Member();
        member.setFirstName(familyMemberCreateDto.getFirstName());
        member.setLastName(familyMemberCreateDto.getLastName());
        member.setCreatedBy(userDetails.getUser());

        memberRepository.save(member);

        return memberToFamilyMemberRetrieveDto(member);
    }

    @Transactional
    public FamilyMemberRetrieveDto patchMember(UUID uuid, FamilyMemberPatchDto familyMemberPatchDto)
    {
        Member member = memberRepository.findById(uuid).orElseThrow(NotFoundException::new);

        if (familyMemberPatchDto.getFirstName() != null)
        {
            member.setFirstName(familyMemberPatchDto.getFirstName());
        }

        if (familyMemberPatchDto.getLastName() != null)
        {
            member.setLastName(familyMemberPatchDto.getLastName());
        }

        memberRepository.save(member);

        return memberToFamilyMemberRetrieveDto(member);
    }

    @Transactional(readOnly = true)
    public FamilyMemberRetrieveDto getMember(UUID uuid)
    {
        Member member = memberRepository.findById(uuid).orElseThrow(NotFoundException::new);
        return memberToFamilyMemberRetrieveDto(member);
    }

    @Transactional(readOnly = true)
    public Page<FamilyMemberRetrieveDto> getMembers(String name, Pageable pageable)
    {
        Page<Member> page;
        if (name != null && !name.isBlank())
        {
            page = memberRepository.searchByName(name.trim(), pageable);
        }
        else
        {
            page = memberRepository.findAll(pageable);
        }

        return page.map(this::memberToFamilyMemberRetrieveDto);
    }

    @Transactional
    public void deleteMember(UUID uuid)
    {
        Member member = memberRepository.findById(uuid).orElseThrow(NotFoundException::new);

        if (member.getPhotos() != null)
        {
            for (MemberPhoto photo : member.getPhotos())
            {
                fileStorageService.deleteFile(photo.getFilePath());
                fileStorageService.deleteFile(photo.getThumbnailPath());
            }
        }

        memberRepository.delete(member);
    }

    @Transactional
    public FamilyMemberPhotoRetrieveDto addPhoto(UUID memberUuid, MultipartFile file,
            CustomUserDetails userDetails)
    {
        Member member = memberRepository.findById(memberUuid).orElseThrow(NotFoundException::new);


        String savedFilePath = fileStorageService.storeFile(file);

        String thumbnailPath = fileStorageService.createThumbnail(savedFilePath);

        try
        {
            MemberPhoto photo = MemberPhoto.builder().originalName(file.getOriginalFilename())
                    .filePath(savedFilePath).thumbnailPath(thumbnailPath)
                    .fileSizeBytes(file.getSize()).mimeType(file.getContentType()).member(member).createdBy(userDetails.getUser())
                    .build();

            MemberPhoto savedPhoto = memberPhotoRepository.save(photo);

            if (member.getPhotos() != null)
            {
                member.getPhotos().add(savedPhoto);
            }

            return memberPhotoToFamilyMemberPhotoRetrieveDto(savedPhoto);

        }
        catch (Exception ex)
        {

            fileStorageService.deleteFile(savedFilePath);
            fileStorageService.deleteFile(thumbnailPath);
            throw ex;
        }
    }

    @Transactional
    public void removePhoto(UUID memberUuid, UUID photoUuid)
    {
        if (!memberRepository.existsById(memberUuid))
        {
            throw new NotFoundException();
        }

        MemberPhoto photo = memberPhotoRepository.findByUuidAndMemberUuid(photoUuid, memberUuid)
                .orElseThrow(NotFoundException::new);

        fileStorageService.deleteFile(photo.getFilePath());
        fileStorageService.deleteFile(photo.getThumbnailPath());

        memberPhotoRepository.delete(photo);
    }

    private FamilyMemberRetrieveDto memberToFamilyMemberRetrieveDto(Member member)
    {
        FamilyMemberRetrieveDto familyMemberRetrieveDto = new FamilyMemberRetrieveDto();
        familyMemberRetrieveDto.setCreatedAt(member.getCreatedAt());
        familyMemberRetrieveDto.setFirstName(member.getFirstName());
        familyMemberRetrieveDto.setLastName(member.getLastName());
        familyMemberRetrieveDto.setUpdatedAt(member.getUpdatedAt());
        familyMemberRetrieveDto.setUuid(member.getUuid());


        if (member.getPhotos() != null)
        {
            familyMemberRetrieveDto.setPhotos(member.getPhotos().stream()
                    .map(this::memberPhotoToFamilyMemberPhotoRetrieveDto).toList());
        }

        return familyMemberRetrieveDto;
    }

    private FamilyMemberPhotoRetrieveDto memberPhotoToFamilyMemberPhotoRetrieveDto(MemberPhoto photo) {
        FamilyMemberPhotoRetrieveDto dto = new FamilyMemberPhotoRetrieveDto();
        dto.setCreatedAt(photo.getCreatedAt());
        dto.setMimeType(photo.getMimeType());
        dto.setUuid(photo.getUuid());
        dto.setFilePath(fileUrlService.buildPublicUrl(photo.getFilePath()));
        dto.setThumbnailPath(fileUrlService.buildPublicUrl(photo.getThumbnailPath()));

        return dto;
    }
}
