package com.finresearch.security;

import com.finresearch.config.FinResearchProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final FinResearchProperties props;

    private SecretKey key() {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(props.getJwt().getSecret().getBytes(StandardCharsets.UTF_8));
            return Keys.hmacShaKeyFor(digest);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public String createToken(Long userId, String username) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .issuedAt(new Date(now))
                .expiration(new Date(now + props.getJwt().getExpirationMs()))
                .signWith(key())
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
