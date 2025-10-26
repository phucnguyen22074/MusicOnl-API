package com.example.demo.services;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;

import javax.crypto.SecretKey;

@Service
public class JWTServiceImpl implements JWTService {

    private static final Logger log = LoggerFactory.getLogger(JWTServiceImpl.class);

    private final Environment env;

    // Cached config
    private SecretKey key;
    private long expiryMs;
    private String issuer;
    private long clockSkewSeconds;

    public JWTServiceImpl(Environment env) {
        this.env = env;
    }

    @PostConstruct
    void init() {
        // Đọc config 1 lần
        String secretRaw = getRequired("jwt.secret");         // đổi key: secret -> jwt.secret
        this.expiryMs = Long.parseLong(env.getProperty("jwt.expiry", "604800000")); // 7 ngày
        this.issuer = env.getProperty("jwt.issuer", "cinema-app");
        this.clockSkewSeconds = Long.parseLong(env.getProperty("jwt.clockSkewSeconds", "60"));

        // Ưu tiên secret Base64: jwt.secret.base64=true hoặc auto-detect
        boolean base64 = Boolean.parseBoolean(env.getProperty("jwt.secret.base64", "true"));
        if (base64) {
            byte[] decoded = Decoders.BASE64.decode(secretRaw);
            if (decoded.length < 32) {
                throw new IllegalStateException("JWT Base64 secret must be at least 32 bytes after decoding for HS256");
            }
            this.key = Keys.hmacShaKeyFor(decoded);
        } else {
            if (secretRaw.getBytes(StandardCharsets.UTF_8).length < 32) {
                throw new IllegalStateException("JWT secret must be at least 32 bytes for HS256");
            }
            this.key = Keys.hmacShaKeyFor(secretRaw.getBytes(StandardCharsets.UTF_8));
        }

        log.info("JWT initialized: issuer='{}', expiryMs={}, clockSkewSeconds={}", issuer, expiryMs, clockSkewSeconds);
    }

    private String getRequired(String key) {
        String v = env.getProperty(key);
        if (v == null || v.isBlank()) {
            throw new IllegalStateException("Missing required config: " + key);
        }
        return v;
    }

    @Override
    public String generateToken(String username) {
        try {
            Instant now = Instant.now();
            Instant exp = now.plusMillis(expiryMs);

            return Jwts.builder()
                    .header().type("JWT").and()
                    .id(UUID.randomUUID().toString())
                    .subject(username)
                    .issuer(issuer)
                    .issuedAt(Date.from(now))
                    .notBefore(Date.from(now.minusSeconds(1))) // nới nhẹ
                    .expiration(Date.from(exp))
                    .signWith(key, Jwts.SIG.HS256)
                    .compact();
        } catch (Exception e) {
            throw new RuntimeException("Error generating JWT token", e);
        }
    }

    @Override
    public String getUsernameFromJWT(String token) {
        try {
            Claims claims = Jwts.parser()
                    .clockSkewSeconds(clockSkewSeconds)
                    .requireIssuer(issuer) // khớp issuer
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.getSubject();
        } catch (ExpiredJwtException e) {
            throw new RuntimeException("Token has expired", e);
        } catch (UnsupportedJwtException e) {
            throw new RuntimeException("Unsupported JWT token", e);
        } catch (MalformedJwtException e) {
            throw new RuntimeException("Invalid JWT token", e);
        } catch (SignatureException e) {
            throw new RuntimeException("Invalid JWT signature", e);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("JWT claims string is empty", e);
        } catch (Exception e) {
            throw new RuntimeException("Error parsing JWT token", e);
        }
    }

    @Override
    public boolean validToken(String token) {
        try {
            Jwts.parser()
                .clockSkewSeconds(clockSkewSeconds)
                .requireIssuer(issuer)
                .verifyWith(key)
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.debug("JWT expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.debug("JWT unsupported: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.debug("JWT malformed: {}", e.getMessage());
        } catch (SignatureException e) {
            log.debug("JWT bad signature: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.debug("JWT empty/illegal: {}", e.getMessage());
        } catch (Exception e) {
            log.debug("JWT validation error: {}", e.getMessage());
        }
        return false;
    }
}
