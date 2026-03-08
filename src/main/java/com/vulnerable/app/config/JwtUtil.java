package com.vulnerable.app.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * ⚠️ VULNERABILITY #3: Broken Authentication
 *
 * Issues in this class:
 *  1. JWT has NO expiration time set
 *  2. Weak hardcoded secret ("secret123") used for signing
 *  3. Algorithm is not explicitly validated on parse
 *  4. No token revocation / blacklist mechanism
 */
@Component
public class JwtUtil {

    // ⚠️ Weak, hardcoded secret read from properties
    @Value("${jwt.secret}")
    private String secret;

    /**
     * Generates a JWT token with NO expiration date.
     * Once issued, this token is valid forever.
     */
    public String generateToken(String username, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date())
                // ⚠️ NO .setExpiration() call — token never expires
                .signWith(SignatureAlgorithm.HS256, secret)
                .compact();
    }

    public String extractUsername(String token) {
        return getClaims(token).getSubject();
    }

    public String extractRole(String token) {
        return (String) getClaims(token).get("role");
    }

    /**
     * ⚠️ Validates only signature — never checks expiration
     * because there is none.
     */
    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .setSigningKey(secret)
                .parseClaimsJws(token)
                .getBody();
    }
}
