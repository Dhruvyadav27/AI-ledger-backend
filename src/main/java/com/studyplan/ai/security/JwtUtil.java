package com.studyplan.ai.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * Handles everything JWT-related: creating a signed token at login/signup,
 * and later (in JwtAuthFilter) reading the userId back out of an incoming
 * token to authenticate each request.
 *
 * We store the userId as the token's "subject" - that's the one piece of
 * info every protected endpoint needs to scope data by user.
 */
@Component
public class JwtUtil {

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms}") long expirationMs
    ) {
        // HS256 needs a key of at least 256 bits (32 bytes). Our configured
        // secret string is 64 ASCII chars = 64 bytes, so this is safe.
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes());
        this.expirationMs = expirationMs;
    }

    /** Creates a signed JWT with the user's id as subject. */
    public String generateToken(String userId, String email) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(userId)
                .claim("email", email)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    /** Extracts the userId (subject) from a valid token. Throws if invalid/expired. */
    public String extractUserId(String token) {
        return parseClaims(token).getSubject();
    }

    /** Returns true if the token is well-formed, signed correctly, and not expired. */
    public boolean isTokenValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
