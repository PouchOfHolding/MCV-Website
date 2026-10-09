package pt.ualg.mcv.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import pt.ualg.mcv.config.security.CustomUserDetails;
import pt.ualg.mcv.exceptions.ConflictException;
import pt.ualg.mcv.exceptions.NotFoundException;
import pt.ualg.mcv.user.dtos.UserRegisterDto;
import pt.ualg.mcv.user.dtos.UserRetrieveDto;
import pt.ualg.mcv.user.dtos.UserUpdateDto;
import pt.ualg.mcv.user.dtos.UserUpdateRoleDto;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceUnitTest
{

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private CustomUserDetails customUserDetails;

    @InjectMocks
    private UserService userService;

    private UUID userUuid;
    private User baseUser;

    @BeforeEach
    void setUp()
    {
        userUuid = UUID.randomUUID();
        baseUser = new User();
        baseUser.setUuid(userUuid);
        baseUser.setEmail("test@ualg.pt");
        baseUser.setFirstName("Test");
        baseUser.setLastName("User");
        baseUser.setPassword("hashed_password");
        baseUser.setRole(UserRole.MEMBER);
    }


    @Test
    void register_Success()
    {
        UserRegisterDto dto = new UserRegisterDto();
        dto.setEmail("test@ualg.pt");
        dto.setFirstName("Test");
        dto.setLastName("User");
        dto.setPassword("password123");

        when(userRepository.existsByEmail(dto.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(dto.getPassword())).thenReturn("hashed_password");

        assertDoesNotThrow(() -> userService.register(dto));
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void register_ThrowsConflict_WhenEmailExists()
    {
        UserRegisterDto dto = new UserRegisterDto();
        dto.setEmail("test@ualg.pt");
        dto.setFirstName("Test");
        dto.setLastName("User");
        dto.setPassword("password123");

        when(userRepository.existsByEmail(dto.getEmail())).thenReturn(true);

        assertThrows(ConflictException.class, () -> userService.register(dto));
        verify(userRepository, never()).save(any(User.class));
    }


    @Test
    void getUser_Success()
    {
        when(userRepository.findById(userUuid)).thenReturn(Optional.of(baseUser));

        UserRetrieveDto result = userService.getUser(userUuid);

        assertNotNull(result);
        assertEquals("test@ualg.pt", result.getEmail());
        assertEquals("Test", result.getFirstName());
        assertEquals("User", result.getLastName());
    }

    @Test
    void getUser_ThrowsNotFound_WhenIdDoesNotExist()
    {
        when(userRepository.findById(userUuid)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> userService.getUser(userUuid));
    }


    @Test
    void getUsers_ReturnsPagedData_WhenQueryIsNull()
    {
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> userPage = new PageImpl<>(Collections.singletonList(baseUser));
        UUID loggedInUserUuid = UUID.randomUUID();

        when(customUserDetails.getUuid()).thenReturn(loggedInUserUuid);
        when(userRepository.findByUuidNot(eq(loggedInUserUuid), eq(pageable))).thenReturn(userPage);

        Page<UserRetrieveDto> result = userService.getUsers(null, pageable, customUserDetails);

        assertNotNull(result);
        assertFalse(result.getContent().isEmpty());
        assertEquals(1, result.getTotalElements());
        assertEquals("test@ualg.pt", result.getContent().get(0).getEmail());

        verify(userRepository, times(1)).findByUuidNot(loggedInUserUuid, pageable);
        verify(userRepository, never()).searchByFullNameAndUuidNot(any(), any(), any());
    }

    @Test
    void getUsers_ReturnsPagedData_WhenQueryIsProvided()
    {
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> userPage = new PageImpl<>(Collections.singletonList(baseUser));
        UUID loggedInUserUuid = UUID.randomUUID();

        when(customUserDetails.getUuid()).thenReturn(loggedInUserUuid);
        when(userRepository.searchByFullNameAndUuidNot(eq("Test User"), eq(loggedInUserUuid), eq(pageable)))
                .thenReturn(userPage);

        Page<UserRetrieveDto> result = userService.getUsers("Test User", pageable, customUserDetails);

        assertNotNull(result);
        assertFalse(result.getContent().isEmpty());
        assertEquals(1, result.getTotalElements());
        assertEquals("test@ualg.pt", result.getContent().get(0).getEmail());

        verify(userRepository, times(1)).searchByFullNameAndUuidNot("Test User", loggedInUserUuid, pageable);
        verify(userRepository, never()).findByUuidNot(any(), any());
    }


    @Test
    void updateUser_Success_AllFieldsProvided()
    {
        UserUpdateDto updateDto = new UserUpdateDto();
        updateDto.setFirstName("UpdatedFirstName");
        updateDto.setLastName("UpdatedLastName");
        updateDto.setPassword("newPassword123");

        when(customUserDetails.getUser()).thenReturn(baseUser);
        when(passwordEncoder.encode("newPassword123")).thenReturn("new_hashed_password");
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserRetrieveDto result = userService.updateUser(updateDto, customUserDetails);

        assertNotNull(result);
        assertEquals("UpdatedFirstName", result.getFirstName());
        assertEquals("UpdatedLastName", result.getLastName());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(1)).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertEquals("UpdatedFirstName", savedUser.getFirstName());
        assertEquals("UpdatedLastName", savedUser.getLastName());
        assertEquals("new_hashed_password", savedUser.getPassword());
    }

    @Test
    void updateUser_Success_PartialUpdateOnlyFirstName()
    {
        UserUpdateDto updateDto = new UserUpdateDto();
        updateDto.setFirstName("UpdatedFirstName");

        when(customUserDetails.getUser()).thenReturn(baseUser);
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserRetrieveDto result = userService.updateUser(updateDto, customUserDetails);

        assertNotNull(result);
        assertEquals("UpdatedFirstName", result.getFirstName());
        assertEquals("User", result.getLastName());

        verify(passwordEncoder, never()).encode(anyString());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(1)).save(userCaptor.capture());
        assertEquals("hashed_password", userCaptor.getValue().getPassword());
    }

    @Test
    void updateUser_Success_EmptyDtoDoesNotModifyUser()
    {
        UserUpdateDto updateDto = new UserUpdateDto();

        when(customUserDetails.getUser()).thenReturn(baseUser);
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserRetrieveDto result = userService.updateUser(updateDto, customUserDetails);

        assertNotNull(result);
        assertEquals("Test", result.getFirstName());
        assertEquals("User", result.getLastName());
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, times(1)).save(any(User.class));
    }


    @Test
    void updateUserRole_Success()
    {
        UserUpdateRoleDto roleDto = new UserUpdateRoleDto();
        roleDto.setRole(UserRole.ADMIN);

        UUID authenticatedAdminUuid = UUID.randomUUID();
        when(customUserDetails.getUuid()).thenReturn(authenticatedAdminUuid);
        when(userRepository.findById(userUuid)).thenReturn(Optional.of(baseUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserRetrieveDto result = userService.updateUserRole(userUuid, roleDto, customUserDetails);

        assertNotNull(result);
        assertEquals(UserRole.ADMIN, result.getRole());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(1)).save(userCaptor.capture());
        assertEquals(UserRole.ADMIN, userCaptor.getValue().getRole());
    }

    @Test
    void updateUserRole_ThrowsConflict_WhenAdminAttemptsToModifyOwnRole()
    {
        UserUpdateRoleDto roleDto = new UserUpdateRoleDto();
        roleDto.setRole(UserRole.MEMBER);

        when(customUserDetails.getUuid()).thenReturn(userUuid);

        assertThrows(ConflictException.class,
                () -> userService.updateUserRole(userUuid, roleDto, customUserDetails));
        verify(userRepository, never()).findById(any());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateUserRole_ThrowsNotFound_WhenUserDoesNotExist()
    {
        UserUpdateRoleDto roleDto = new UserUpdateRoleDto();
        roleDto.setRole(UserRole.ADMIN);

        UUID authenticatedAdminUuid = UUID.randomUUID();
        when(customUserDetails.getUuid()).thenReturn(authenticatedAdminUuid);
        when(userRepository.findById(userUuid)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> userService.updateUserRole(userUuid, roleDto, customUserDetails));
        verify(userRepository, never()).save(any(User.class));
    }
}