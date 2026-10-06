package com.example.restarch.security;

import com.example.restarch.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties properties;
    private volatile SecretKey key;

    private SecretKey key() {
        if (key == null) {
            key = Keys.hmacShaKeyFor(properties.getJwtSecret().getBytes(StandardCharsets.UTF_8));
        }
        return key;
    }

    public String generateToken(String clientId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + properties.getJwtExpiryMs());
        return Jwts.builder()
                .subject(clientId)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key())
                .compact();
    }

    public String extractClientId(String token) {
        Jws<Claims> parsed = Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token);
        return parsed.getPayload().getSubject();
    }
}
