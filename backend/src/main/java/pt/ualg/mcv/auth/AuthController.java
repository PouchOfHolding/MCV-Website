package pt.ualg.mcv.auth;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import pt.ualg.mcv.auth.dtos.AuthLoginDto;
import pt.ualg.mcv.auth.dtos.AuthResponseDto;
import pt.ualg.mcv.exceptions.UnauthorizedException;

@Tag(name = "Authentication",
        description = "Endpoints for user login, token refresh, and logout operations")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController
{

    private final AuthService authService;
    private final AuthUtils authUtils;

    @Operation(summary = "User Login",
            description = "Authenticates user credentials and sets HttpOnly cookies for access and refresh tokens.")
    @ApiResponses(value =
    {@ApiResponse(responseCode = "200",
            description = "Successfully authenticated. Access and refresh cookies returned in headers.",
            headers =
            {@Header(name = HttpHeaders.SET_COOKIE,
                    description = "Sets HttpOnly access_token and refresh_token cookies",
                    schema = @Schema(type = "string"))}),
            @ApiResponse(responseCode = "400",
                    description = "Invalid request payload or missing required fields",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "Invalid credentials",
                    content = @Content)})
    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public void login(@Valid @RequestBody AuthLoginDto loginDTO, HttpServletResponse response)
    {
        AuthResponseDto tokens = authService.login(loginDTO);

        ResponseCookie accessTokenCookie =
                authUtils.setResponseCookieForAccess(tokens.getAccessToken());

        ResponseCookie refreshTokenCookie =
                authUtils.setResponseCookieForRefresh(tokens.getRefreshToken());

        response.addHeader(HttpHeaders.SET_COOKIE, accessTokenCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());
    }

    @Operation(summary = "Refresh JWT Tokens",
            description = "Uses the refresh token provided in the cookie to issue a new pair of access and refresh tokens.")
    @ApiResponses(value =
    {@ApiResponse(responseCode = "200",
            description = "Tokens successfully refreshed. Updated cookies set in headers.",
            headers =
            {@Header(name = HttpHeaders.SET_COOKIE,
                    description = "Sets updated access_token and refresh_token cookies",
                    schema = @Schema(type = "string"))}),
            @ApiResponse(responseCode = "401",
                    description = "Refresh token is missing, expired, or invalid",
                    content = @Content)})
    @PostMapping("/refresh")
    @ResponseStatus(HttpStatus.OK)
    public void refresh(@Parameter(description = "Refresh token extracted from the HttpOnly cookie",
            required = false) @CookieValue(name = "refresh_token",
                    required = false) String refreshToken,
            HttpServletResponse response)
    {
        if (refreshToken == null)
        {
            throw new UnauthorizedException("Refresh token is missing.");
        }

        AuthResponseDto newTokens = authService.refresh(refreshToken);

        ResponseCookie newAccessTokenCookie =
                authUtils.setResponseCookieForAccess(newTokens.getAccessToken());

        ResponseCookie newRefreshTokenCookie =
                authUtils.setResponseCookieForRefresh(newTokens.getRefreshToken());

        response.addHeader(HttpHeaders.SET_COOKIE, newAccessTokenCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, newRefreshTokenCookie.toString());
    }

    @Operation(summary = "User Logout",
            description = "Revokes access and refresh tokens by clearing the stored HttpOnly cookies.")
    @ApiResponses(value =
    {@ApiResponse(responseCode = "204",
            description = "Successfully logged out. Token cookies cleared.", headers =
            {@Header(name = HttpHeaders.SET_COOKIE,
                    description = "Clears access_token and refresh_token cookies",
                    schema = @Schema(type = "string"))})})
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletResponse response)
    {
        ResponseCookie cleanAccessCookie = authUtils.revokeResponseCookieForAccess();
        ResponseCookie cleanRefreshCookie = authUtils.revokeResponseCookieForRefresh();

        response.addHeader(HttpHeaders.SET_COOKIE, cleanAccessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, cleanRefreshCookie.toString());
    }
}
