package pt.ualg.mcv.family;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
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
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import pt.ualg.mcv.common.services.FileStorageService;
import pt.ualg.mcv.common.services.FileUrlService;
import pt.ualg.mcv.config.security.CustomUserDetails;
import pt.ualg.mcv.exceptions.NotFoundException;
import pt.ualg.mcv.family.dtos.FamilyMemberCreateDto;
import pt.ualg.mcv.family.dtos.FamilyMemberPatchDto;
import pt.ualg.mcv.family.dtos.FamilyMemberPhotoRetrieveDto;
import pt.ualg.mcv.family.dtos.FamilyMemberRetrieveDto;
import pt.ualg.mcv.user.User;

@ExtendWith(MockitoExtension.class)
class FamilyMemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private MemberPhotoRepository memberPhotoRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private FileUrlService fileUrlService;

    @InjectMocks
    private MemberService memberService;

    private User sampleUser;
    private CustomUserDetails customUserDetails;
    private Member sampleMember;
    private UUID memberUuid;
    private UUID photoUuid;

    @BeforeEach
    void setUp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(8080);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        memberUuid = UUID.randomUUID();
        photoUuid = UUID.randomUUID();

        sampleUser = new User();
        sampleUser.setUuid(UUID.randomUUID());
        customUserDetails = new CustomUserDetails(sampleUser);

        sampleMember = new Member();
        sampleMember.setUuid(memberUuid);
        sampleMember.setFirstName("John");
        sampleMember.setLastName("Doe");
        sampleMember.setCreatedBy(sampleUser);
        sampleMember.setPhotos(new ArrayList<>());
    }

    @Test
    @DisplayName("createMember - Should create and return FamilyMemberRetrieveDto successfully")
    void createMember_Success() {
        FamilyMemberCreateDto dto = new FamilyMemberCreateDto();
        dto.setFirstName("John");
        dto.setLastName("Doe");

        when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> {
            Member savedMember = invocation.getArgument(0);
            savedMember.setUuid(memberUuid);
            return savedMember;
        });

        FamilyMemberRetrieveDto result = memberService.createMember(dto, customUserDetails);

        assertThat(result).isNotNull();
        assertThat(result.getFirstName()).isEqualTo("John");
        assertThat(result.getLastName()).isEqualTo("Doe");
        verify(memberRepository, times(1)).save(any(Member.class));
    }

    @Test
    @DisplayName("patchMember - Should update only non-null fields successfully")
    void patchMember_Success() {
        FamilyMemberPatchDto patchDto = new FamilyMemberPatchDto();
        patchDto.setFirstName("Jane");

        when(memberRepository.findById(memberUuid)).thenReturn(Optional.of(sampleMember));
        when(memberRepository.save(any(Member.class))).thenReturn(sampleMember);

        FamilyMemberRetrieveDto result = memberService.patchMember(memberUuid, patchDto);

        assertThat(result.getFirstName()).isEqualTo("Jane");
        assertThat(result.getLastName()).isEqualTo("Doe");
        verify(memberRepository, times(1)).save(sampleMember);
    }

    @Test
    @DisplayName("patchMember - Should throw NotFoundException when member does not exist")
    void patchMember_NotFound() {
        FamilyMemberPatchDto patchDto = new FamilyMemberPatchDto();
        when(memberRepository.findById(memberUuid)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.patchMember(memberUuid, patchDto))
                .isInstanceOf(NotFoundException.class);

        verify(memberRepository, never()).save(any());
    }

    @Test
    @DisplayName("getMember - Should return member DTO when member exists")
    void getMember_Success() {
        when(memberRepository.findById(memberUuid)).thenReturn(Optional.of(sampleMember));

        FamilyMemberRetrieveDto result = memberService.getMember(memberUuid);

        assertThat(result).isNotNull();
        assertThat(result.getUuid()).isEqualTo(memberUuid);
    }

    @Test
    @DisplayName("getMember - Should throw NotFoundException when member does not exist")
    void getMember_NotFound() {
        when(memberRepository.findById(memberUuid)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.getMember(memberUuid))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("getMembers - Should return paged members")
    void getMembers_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Member> memberPage = new PageImpl<>(List.of(sampleMember));

        when(memberRepository.findAll(pageable)).thenReturn(memberPage);

        Page<FamilyMemberRetrieveDto> result = memberService.getMembers(null, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getFirstName()).isEqualTo("John");
    }

    @Test
    @DisplayName("deleteMember - Should delete member and associated photo files from disk")
    void deleteMember_WithPhotos_Success() {
        MemberPhoto photo = MemberPhoto.builder()
                .uuid(photoUuid)
                .filePath("/uploads/image.png")
                .thumbnailPath("/uploads/thumb_image.png")
                .build();
        sampleMember.setPhotos(List.of(photo));

        when(memberRepository.findById(memberUuid)).thenReturn(Optional.of(sampleMember));

        memberService.deleteMember(memberUuid);

        verify(fileStorageService, times(1)).deleteFile("/uploads/image.png");
        verify(fileStorageService, times(1)).deleteFile("/uploads/thumb_image.png");
        verify(memberRepository, times(1)).delete(sampleMember);
    }

    @Test
    @DisplayName("deleteMember - Should throw NotFoundException when member to delete does not exist")
    void deleteMember_NotFound() {
        when(memberRepository.findById(memberUuid)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.deleteMember(memberUuid))
                .isInstanceOf(NotFoundException.class);

        verify(memberRepository, never()).delete(any());
    }

    @Test
    @DisplayName("addPhoto - Should store file, generate thumbnail, save DB record, and return DTO")
    void addPhoto_Success() {
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "image content".getBytes());

        when(memberRepository.findById(memberUuid)).thenReturn(Optional.of(sampleMember));
        when(fileStorageService.storeFile(file)).thenReturn("/uploads/test.jpg");
        when(fileStorageService.createThumbnail("/uploads/test.jpg")).thenReturn("/uploads/thumb_test.jpg");

        MemberPhoto savedPhoto = MemberPhoto.builder()
                .uuid(photoUuid)
                .originalName("test.jpg")
                .filePath("/uploads/test.jpg")
                .thumbnailPath("/uploads/thumb_test.jpg")
                .mimeType("image/jpeg")
                .build();

        when(memberPhotoRepository.save(any(MemberPhoto.class))).thenReturn(savedPhoto);

        when(fileUrlService.buildPublicUrl("/uploads/test.jpg")).thenReturn("http://localhost:8080/uploads/test.jpg");
        when(fileUrlService.buildPublicUrl("/uploads/thumb_test.jpg")).thenReturn("http://localhost:8080/uploads/thumb_test.jpg");

        FamilyMemberPhotoRetrieveDto dto = memberService.addPhoto(memberUuid, file, customUserDetails);

        assertThat(dto).isNotNull();
        assertThat(dto.getFilePath()).isEqualTo("http://localhost:8080/uploads/test.jpg");
        assertThat(dto.getThumbnailPath()).isEqualTo("http://localhost:8080/uploads/thumb_test.jpg");
        verify(memberPhotoRepository, times(1)).save(any(MemberPhoto.class));
    }

    @Test
    @DisplayName("addPhoto - Should cleanup stored files if DB save fails")
    void addPhoto_DatabaseFailure_CleansUpFiles() {
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "content".getBytes());

        when(memberRepository.findById(memberUuid)).thenReturn(Optional.of(sampleMember));
        when(fileStorageService.storeFile(file)).thenReturn("/uploads/test.jpg");
        when(fileStorageService.createThumbnail("/uploads/test.jpg")).thenReturn("/uploads/thumb_test.jpg");
        when(memberPhotoRepository.save(any(MemberPhoto.class))).thenThrow(new RuntimeException("Database error"));

        assertThatThrownBy(() -> memberService.addPhoto(memberUuid, file, customUserDetails))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Database error");

        verify(fileStorageService, times(1)).deleteFile("/uploads/test.jpg");
        verify(fileStorageService, times(1)).deleteFile("/uploads/thumb_test.jpg");
    }

    @Test
    @DisplayName("removePhoto - Should delete file, thumbnail, and DB record")
    void removePhoto_Success() {
        MemberPhoto photo = MemberPhoto.builder()
                .uuid(photoUuid)
                .filePath("/uploads/photo.jpg")
                .thumbnailPath("/uploads/thumb_photo.jpg")
                .build();

        when(memberRepository.existsById(memberUuid)).thenReturn(true);
        when(memberPhotoRepository.findByUuidAndMemberUuid(photoUuid, memberUuid)).thenReturn(Optional.of(photo));

        memberService.removePhoto(memberUuid, photoUuid);

        verify(fileStorageService, times(1)).deleteFile("/uploads/photo.jpg");
        verify(fileStorageService, times(1)).deleteFile("/uploads/thumb_photo.jpg");
        verify(memberPhotoRepository, times(1)).delete(photo);
    }

    @Test
    @DisplayName("removePhoto - Should throw NotFoundException when member does not exist")
    void removePhoto_MemberNotFound() {
        when(memberRepository.existsById(memberUuid)).thenReturn(false);

        assertThatThrownBy(() -> memberService.removePhoto(memberUuid, photoUuid))
                .isInstanceOf(NotFoundException.class);

        verify(fileStorageService, never()).deleteFile(any());
        verify(memberPhotoRepository, never()).delete(any());
    }

    @Test
    @DisplayName("removePhoto - Should throw NotFoundException when photo does not belong to member")
    void removePhoto_PhotoNotFound() {
        when(memberRepository.existsById(memberUuid)).thenReturn(true);
        when(memberPhotoRepository.findByUuidAndMemberUuid(photoUuid, memberUuid)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.removePhoto(memberUuid, photoUuid))
                .isInstanceOf(NotFoundException.class);

        verify(fileStorageService, never()).deleteFile(any());
        verify(memberPhotoRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Should return filtered members when name parameter is provided")
    void getMembers_WithName_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Member> memberPage = new PageImpl<>(List.of(sampleMember), pageable, 1);

        when(memberRepository.searchByName("John", pageable)).thenReturn(memberPage);

        Page<FamilyMemberRetrieveDto> result = memberService.getMembers("John", pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getFirstName()).isEqualTo("John");
        verify(memberRepository).searchByName("John", pageable);
        verify(memberRepository, never()).findAll(pageable);
    }

    @Test
    @DisplayName("Should return all members when name parameter is null or blank")
    void getMembers_WithoutName_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Member> memberPage = new PageImpl<>(List.of(sampleMember), pageable, 1);

        when(memberRepository.findAll(pageable)).thenReturn(memberPage);

        Page<FamilyMemberRetrieveDto> result = memberService.getMembers(null, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(memberRepository).findAll(pageable);
        verify(memberRepository, never()).searchByName(any(), any());
    }
}