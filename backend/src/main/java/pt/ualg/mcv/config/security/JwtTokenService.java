package pt.ualg.mcv.config.security;


import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class JwtTokenService
{
    @Value("${mcv.api.jwtSecret}")
    private String secret;

    private SecretKey secretKey;

    @Value("${mcv.api.jwtAccessExpirationMs}")
    private long accessTokenExpirationInMs;

    @Value("${mcv.api.jwtRefreshExpirationMs}")
    private long refreshTokenExpirationInMs;

    @jakarta.annotation.PostConstruct
    public void init()
    {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateAccessToken(String username, UUID userUuid)
    {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(accessTokenExpirationInMs, ChronoUnit.MILLIS);

        return Jwts.builder().subject(username).claim("userId", userUuid.toString())
                .issuedAt(Date.from(now)).expiration(Date.from(expiresAt)).signWith(secretKey)
                .compact();
    }

    public String generateRefreshToken(String username)
    {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(refreshTokenExpirationInMs, ChronoUnit.MILLIS);

        return Jwts.builder().subject(username).issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt)).signWith(secretKey).compact();
    }

    public String extractUsername(String token)
    {
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload()
                .getSubject();
    }

    public boolean isTokenValid(String token)
    {
        try
        {

            Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token);
            return true;
        }
        catch (Exception e)
        {

            return false;
        }
    }


}
