package com.sajilni.service;

import com.sajilni.config.JwtConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.time.Duration;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Date;
import java.util.Map;

@Service
public class JwtService {
    private static final Logger log = LoggerFactory.getLogger(JwtService.class);
    private final JwtConfig jwtConfig;
    private SecretKey key;

    public JwtService(JwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
    }

    @PostConstruct
    private void init() {
        byte[] keyBytes = jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8);
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(String email, Long userId, Map<String, Object> claims) {
        Instant now = Instant.now();
        Instant expiry = now.plus(jwtConfig.getExpirationMs());
        Map<String, Object> payload = new HashMap<>();
                if (claims != null) payload.putAll(claims);
                payload.put("userId", userId);
                payload.put("tokenType", "access"); // distinguish from refresh

                        return Jwts.builder()
                                .issuer(jwtConfig.getIssuer())
                                .subject(email)
                                .claims(payload)
                                .issuedAt(Date.from(now))
                                .expiration(Date.from(expiry))
                                .signWith(key)
                                .compact();
    }

    /** Create a long-lived refresh token (no custom claims needed). */
    public String generateRefreshToken(String email, Long userId) {
                Instant now = Instant.now();
                Instant expiry = now.plus(jwtConfig.getRefreshExpirationMs());

                        return Jwts.builder()
                                .issuer(jwtConfig.getIssuer())
                                .subject(email)
                                .claim("userId", userId)
                                .claim("tokenType", "refresh")
                                .issuedAt(Date.from(now))
                                .expiration(Date.from(expiry))
                                .signWith(key)
                                .compact();
    }

    public String getEmailFromToken(String token) {
        return getClaims(token).getSubject();
    }

    public Long getUserIdFromToken(String token) {
        return getClaims(token).get("userId", Long.class);
    }

    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (SecurityException ex) {
            log.error("Invalid JWT signature: {}", ex.getMessage());
        } catch (Exception ex) {
            log.error("JWT validation error: {}", ex.getMessage());
        }
        return false;
    }

    public boolean isTokenExpired(String token) {
        try {
            return getClaims(token).getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    /** True iff the token has "tokenType":"refresh". */
    public boolean isRefreshToken(String token) {
                try {
                        String type = getClaims(token).get("tokenType", String.class);
                        return "refresh".equals(type);
                    } catch (Exception e) {
                        return false;
                    }
            }

            /** Optional: check access type explicitly. */
            public boolean isAccessToken(String token) {
                try {
                        String type = getClaims(token).get("tokenType", String.class);
                        return "access".equals(type);
                    } catch (Exception e) {
                        return false;
                    }
            }

    public Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .requireIssuer(jwtConfig.getIssuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

        /** Access token lifetime (used by AuthenticationService.expiresIn). */
                public Duration getTokenExpiration() {
                return jwtConfig.getExpirationMs();
            }

            /** Refresh token lifetime (handy if you need to expose it). */
            public Duration getRefreshTokenExpiration() {
                return jwtConfig.getRefreshExpirationMs();
            }
}
