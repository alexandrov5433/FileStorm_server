package server.filestorm.util;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import server.filestorm.config.ServerConfigurationProperties;
import server.filestorm.exception.ConfigurationException;

@Service
public class JwtUtil {
    
    private String SECRET;

    public JwtUtil(ServerConfigurationProperties confProps) {
        if (confProps.getJwtSecret().trim().length() == 0) {
            throw new ConfigurationException("The jwt_secret must be valid.");
        }
        this.SECRET = confProps.getJwtSecret();
    }

 
    public String genareteToken(String username, Map<String, Object> claims) {
        return createToken(claims, username);
    }

    @Async
    public CompletableFuture<Boolean> validateToken(String token, String username) {
        final String usernameToken = extractUsername(token);
        final boolean isExpired = extractExparation(token).before(new Date());
        return CompletableFuture.completedFuture(usernameToken.equals(username) && !isExpired);
    }

    private String createToken(Map<String, Object> claims, String username) {
        return Jwts.builder()
            .claims(claims)
            .issuer("FileStorm")
            .subject(username)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + Long.valueOf("31557600000"))) // 1 year
            .signWith(getSignKey())
            .compact();
    }

    private SecretKey getSignKey() {
        // SecretKey key = Jwts.SIG.HS256.key().build();
        // System.out.println(Encoders.BASE64.encode(key.getEncoded()));
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(this.SECRET));
        return key;
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExparation(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public Claims extractAllClaims(String token) throws JwtException {
        try {
            return (Claims) Jwts.parser()
                .verifyWith(getSignKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        } catch (JwtException e) {
            return null;
        }
    }
}
