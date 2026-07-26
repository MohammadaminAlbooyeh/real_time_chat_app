package com.chatapp.websocket.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
public class JwtService {

    private final SecretKey key;

    public JwtService(@Value("${app.jwt.secret-key}") String secretKey) {
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Mirrors the Python service's verify_token: returns null for any invalid,
     * expired, or non-"access" token instead of throwing.
     */
    public Claims verifyToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            if (!"access".equals(claims.get("type", String.class))) {
                return null;
            }
            return claims;
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }
}
