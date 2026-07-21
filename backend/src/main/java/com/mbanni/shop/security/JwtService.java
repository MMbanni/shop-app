package com.mbanni.shop.security;

import com.mbanni.shop.user.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Create and validate jwt
 */

@Service
public class JwtService {
    @Value("${JWT_SECRET}")
    private String secret;

    // Create & sign JWT { id, role }
    public String createToken(User user) {
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("role", user.getRole().name())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 1000 *60 *60))
                .signWith(getKey())
                .compact();

    }

    // Verify jwt and extract id
    public JwtClaimsDTO extractUserData(String token) {
        var payload = Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return new JwtClaimsDTO(
                payload.getSubject(),
                payload.get("role", String.class)
        );

    }

    // Get key to sign jwt
    private SecretKey getKey() {
        byte [] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
