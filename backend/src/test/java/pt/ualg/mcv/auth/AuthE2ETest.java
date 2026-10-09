package pt.ualg.mcv.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pt.ualg.mcv.auth.dtos.AuthLoginDto;
import pt.ualg.mcv.auth.dtos.AuthResponseDto;
import pt.ualg.mcv.config.security.CustomUserDetailsService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerE2ETest
{

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void login_Returns200AndAppendsSetCookieHeaders() throws Exception
    {
        AuthLoginDto credentials = new AuthLoginDto();
        credentials.setEmail("devcoder@gmail.com");
        credentials.setPassword("securePass123");


        AuthResponseDto authResponse = new AuthResponseDto();
        authResponse.setAccessToken("access-jwt-token-value");
        authResponse.setRefreshToken("refresh-jwt-token-value");


        when(authService.login(any(AuthLoginDto.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(credentials))).andExpect(status().isOk())
                .andExpect(header().exists("Set-Cookie"));
    }

    @Test
    void refresh_Returns200WithUpdatedCookies_WhenCookieIsPresent() throws Exception
    {

        AuthResponseDto authResponse = new AuthResponseDto();
        authResponse.setAccessToken("fresh-access-token-string");
        authResponse.setRefreshToken("fresh-refresh-token-string");

        when(authService.refresh("existing-refresh-token")).thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/refresh")
                .cookie(new Cookie("refresh_token", "existing-refresh-token")))
                .andExpect(status().isOk()).andExpect(header().exists("Set-Cookie"));
    }

    @Test
    void refresh_Returns401Unauthorized_WhenCookieIsMissing() throws Exception
    {
        mockMvc.perform(post("/api/auth/refresh")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "testuser")
    void logout_Returns204NoContentAndClearsCookies() throws Exception
    {
        mockMvc.perform(post("/api/auth/logout")).andExpect(status().isNoContent())
                .andExpect(header().exists("Set-Cookie"));
    }
    
}
