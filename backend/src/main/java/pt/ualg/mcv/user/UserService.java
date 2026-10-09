package pt.ualg.mcv.user;

import java.util.UUID;
import pt.ualg.mcv.exceptions.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import pt.ualg.mcv.config.security.CustomUserDetails;
import pt.ualg.mcv.exceptions.ConflictException;
import pt.ualg.mcv.user.dtos.UserRegisterDto;
import pt.ualg.mcv.user.dtos.UserRetrieveDto;
import pt.ualg.mcv.user.dtos.UserUpdateDto;
import pt.ualg.mcv.user.dtos.UserUpdateRoleDto;

@Service
@RequiredArgsConstructor
public class UserService
{

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void register(UserRegisterDto userRegisterDto)
    {

        if (userRepository.existsByEmail(userRegisterDto.getEmail()))
        {
            throw new ConflictException("Email already in use.");
        }

        String encodedPassword = passwordEncoder.encode(userRegisterDto.getPassword());

        User user = new User();
        user.setEmail(userRegisterDto.getEmail());
        user.setFirstName(userRegisterDto.getFirstName());
        user.setLastName(userRegisterDto.getLastName());
        user.setPassword(encodedPassword);

        userRepository.save(user);
    }


    public UserRetrieveDto getUser(UUID uuid)
    {
        User user = userRepository.findById(uuid)
                .orElseThrow(() -> new NotFoundException("User not found."));

        return mapToUserRetrieveDto(user);
    }


    public Page<UserRetrieveDto> getUsers(String query, Pageable pageable,
            CustomUserDetails userDetails)
    {
        Page<User> usersPage;

        if (query != null && !query.trim().isEmpty())
        {
            usersPage = userRepository.searchByFullNameAndUuidNot(query.trim(),
                    userDetails.getUuid(), pageable);
        }
        else
        {
            usersPage = userRepository.findByUuidNot(userDetails.getUuid(), pageable);
        }

        return usersPage.map(this::mapToUserRetrieveDto);
    }

    public UserRetrieveDto getMe(@AuthenticationPrincipal CustomUserDetails userDetails)
    {
        User user = userDetails.getUser();
        return mapToUserRetrieveDto(user);
    }

    @Transactional
    public UserRetrieveDto updateUser(UserUpdateDto userUpdateDto, CustomUserDetails userDetails)
    {
        User user = userDetails.getUser();

        if (userUpdateDto.getFirstName() != null)
        {
            user.setFirstName(userUpdateDto.getFirstName());
        }

        if (userUpdateDto.getLastName() != null)
        {
            user.setLastName(userUpdateDto.getLastName());
        }

        if (userUpdateDto.getPassword() != null)
        {
            String encodedPassword = passwordEncoder.encode(userUpdateDto.getPassword());
            user.setPassword(encodedPassword);
        }

        userRepository.save(user);

        return mapToUserRetrieveDto(user);
    }

    @Transactional
public UserRetrieveDto updateUserRole(UUID uuid, UserUpdateRoleDto userUpdateRoleDto, CustomUserDetails userDetails)
{
    if (uuid.equals(userDetails.getUuid()))
    {
        throw new ConflictException("Admins cannot modify their own role.");
    }

    User user = userRepository.findById(uuid)
            .orElseThrow(() -> new NotFoundException("User not found."));

    user.setRole(userUpdateRoleDto.getRole());
    userRepository.save(user);

    return mapToUserRetrieveDto(user);
}

    private UserRetrieveDto mapToUserRetrieveDto(User user)
    {
        UserRetrieveDto dto = new UserRetrieveDto();
        dto.setUuid(user.getUuid());
        dto.setEmail(user.getEmail());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setRole(user.getRole());
        dto.setCreatedAt(user.getCreatedAt());
        return dto;
    }
}