package com.docint.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;

@Component
@Slf4j
public class JwtTokenProvider {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.access-expiry-ms:900000}")        // 15 minutes
    private long accessExpiryMs;

    @Value("${app.jwt.refresh-expiry-ms:604800000}")    // 7 days
    private long refreshExpiryMs;

    private SecretKey key;

    @PostConstruct
    public void init() {
        if (jwtSecret.length() < 32) {
            throw new IllegalStateException("JWT secret must be at least 32 characters");
        }
        byte[] keyBytes = Decoders.BASE64.decode(
                java.util.Base64.getEncoder().encodeToString(jwtSecret.getBytes()));
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(String email, int tokenVersion) {
        return build(email, accessExpiryMs, "access", tokenVersion);
    }

    public String generateRefreshToken(String email, int tokenVersion) {
        return build(email, refreshExpiryMs, "refresh", tokenVersion);
    }

    private String build(String subject, long ttl, String type, int tokenVersion) {
        Date now = new Date();
        return Jwts.builder()
                .subject(subject)
                .claims(Map.of("type", type, "ver", tokenVersion))
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ttl))
                .signWith(key)
                .compact();
    }

    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    public int extractVersion(String token) {
        Object v = parseClaims(token).get("ver");
        return v == null ? 0 : ((Number) v).intValue();
    }

    public String extractType(String token) {
        Object t = parseClaims(token).get("type");
        return t == null ? "access" : t.toString();
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.debug("JWT expired: {}", e.getMessage());
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT: {}", e.getMessage());
        }
        return false;
    }

    private Claims parseClaims(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}
