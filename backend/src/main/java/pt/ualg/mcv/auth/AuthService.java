package pt.ualg.mcv.auth;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import pt.ualg.mcv.auth.dtos.AuthLoginDto;
import pt.ualg.mcv.auth.dtos.AuthResponseDto;
import pt.ualg.mcv.config.security.CustomUserDetails;
import pt.ualg.mcv.config.security.JwtTokenService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService
{
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtTokenService jwtService;

    public AuthResponseDto login(AuthLoginDto userLoginDTO)
    {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                userLoginDTO.getEmail(), userLoginDTO.getPassword()));


        CustomUserDetails userDetails = (CustomUserDetails) userDetailsService
                .loadUserByUsername(userLoginDTO.getEmail());

        
        // in this case username corresponds to the email
        String username = userDetails.getUsername();
        UUID userUuid = userDetails.getUuid();


        String accessToken = jwtService.generateAccessToken(username, userUuid);
        String refreshToken = jwtService.generateRefreshToken(username);
        
        AuthResponseDto authResponseDto = new AuthResponseDto();
        authResponseDto.setAccessToken(accessToken);
        authResponseDto.setRefreshToken(refreshToken);
        
        return authResponseDto;
    }

    public AuthResponseDto refresh(String refreshToken)
    {
        String username = jwtService.extractUsername(refreshToken);
        CustomUserDetails userDetails =
                (CustomUserDetails) userDetailsService.loadUserByUsername(username);


        String newAccessToken =
                jwtService.generateAccessToken(userDetails.getUsername(), userDetails.getUuid());

        String newRefreshToken = jwtService.generateRefreshToken(userDetails.getUsername());

        AuthResponseDto authResponseDto = new AuthResponseDto();
        authResponseDto.setAccessToken(newAccessToken);
        authResponseDto.setRefreshToken(newRefreshToken);
        return authResponseDto;
    }
}
