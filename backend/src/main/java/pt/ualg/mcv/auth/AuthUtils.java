package pt.ualg.mcv.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class AuthUtils
{
    private static long accessTokenExpirationInMs;

    private static long refreshTokenExpirationInMs;

    public AuthUtils(
            @Value("${mcv.api.jwtAccessExpirationMs}") long accessExpiry,
            @Value("${mcv.api.jwtRefreshExpirationMs}") long refreshExpiry
    ) {
        AuthUtils.accessTokenExpirationInMs = accessExpiry;
        AuthUtils.refreshTokenExpirationInMs = refreshExpiry;
    }

    public ResponseCookie setResponseCookieForAccess(String accessToken)
    {
        return ResponseCookie.from("access_token", accessToken).httpOnly(true).secure(true)
                .path("/").maxAge(accessTokenExpirationInMs / 1000).sameSite("Lax").build();
    }

    public ResponseCookie setResponseCookieForRefresh(String refreshToken)
    {
        return ResponseCookie.from("refresh_token", refreshToken).httpOnly(true).secure(true)
                .path("/api/auth/refresh").maxAge(refreshTokenExpirationInMs / 1000).sameSite("Lax").build();
    }

    public ResponseCookie revokeResponseCookieForAccess()
    {
        return ResponseCookie.from("access_token", "").httpOnly(true).secure(true).path("/")
                .maxAge(0).build();
    }

    public ResponseCookie revokeResponseCookieForRefresh()
    {
        return ResponseCookie.from("refresh_token", "").httpOnly(true).secure(true).path("/api/auth/refresh")
                .maxAge(0).build();

    }
}