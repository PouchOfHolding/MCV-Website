package pt.ualg.mcv.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import pt.ualg.mcv.auth.dtos.AuthLoginDto;
import pt.ualg.mcv.auth.dtos.AuthResponseDto;
import pt.ualg.mcv.config.security.CustomUserDetails;
import pt.ualg.mcv.config.security.JwtTokenService;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceUnitTest
{

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private JwtTokenService jwtService;

    @InjectMocks
    private AuthService authService;

    private AuthLoginDto loginDto;
    private CustomUserDetails mockUserDetails;
    private UUID expectedUuid;

    @BeforeEach
    void setUp()
    {
        loginDto = new AuthLoginDto();
        loginDto.setEmail("devcoder@gmail.com");
        loginDto.setPassword("securePass123");

        expectedUuid = UUID.randomUUID();
        mockUserDetails = mock(CustomUserDetails.class);
    }

    @Test
    void login_Success_ReturnsAccessAndRefreshTokens()
    {

        when(userDetailsService.loadUserByUsername("devcoder@gmail.com")).thenReturn(mockUserDetails);
        when(mockUserDetails.getUsername()).thenReturn("devcoder@gmail.com");
        when(mockUserDetails.getUuid()).thenReturn(expectedUuid);
        when(jwtService.generateAccessToken("devcoder@gmail.com", expectedUuid))
                .thenReturn("mocked-access-token");
        when(jwtService.generateRefreshToken("devcoder@gmail.com")).thenReturn("mocked-refresh-token");


        AuthResponseDto tokens = authService.login(loginDto);


        assertNotNull(tokens);
        assertEquals("mocked-access-token", tokens.getAccessToken());
        assertEquals("mocked-refresh-token", tokens.getRefreshToken());

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userDetailsService).loadUserByUsername("devcoder@gmail.com");
    }

    @Test
    void login_ThrowsException_WhenBadCredentialsProvided()
    {

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid username or password"));


        assertThrows(BadCredentialsException.class, () -> authService.login(loginDto));


        verifyNoInteractions(userDetailsService, jwtService);
    }

    @Test
    void refresh_Success_GeneratesNewTokenPairs()
    {

        String validRefreshToken = "valid-refresh-token-string";
        when(jwtService.extractUsername(validRefreshToken)).thenReturn("devcoder@gmail.com");
        when(userDetailsService.loadUserByUsername("devcoder@gmail.com")).thenReturn(mockUserDetails);
        when(mockUserDetails.getUsername()).thenReturn("devcoder@gmail.com");
        when(mockUserDetails.getUuid()).thenReturn(expectedUuid);
        when(jwtService.generateAccessToken("devcoder@gmail.com", expectedUuid))
                .thenReturn("new-access-token");
        when(jwtService.generateRefreshToken("devcoder@gmail.com")).thenReturn("new-refresh-token");


        AuthResponseDto tokens = authService.refresh(validRefreshToken);


        assertNotNull(tokens);
        assertEquals("new-access-token", tokens.getAccessToken());
        assertEquals("new-refresh-token", tokens.getRefreshToken());

        verify(jwtService).extractUsername(validRefreshToken);
    }
}
