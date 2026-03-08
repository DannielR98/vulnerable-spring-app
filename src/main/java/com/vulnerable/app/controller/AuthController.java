package com.vulnerable.app.controller;

import com.vulnerable.app.config.JwtUtil;
import com.vulnerable.app.model.User;
import com.vulnerable.app.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ⚠️ VULNERABILITY #1: SQL Injection
 * ⚠️ VULNERABILITY #3: Broken Authentication (no rate limiting, weak JWT)
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    @PersistenceContext
    private EntityManager entityManager;

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    /**
     * ⚠️ VULNERABILITY: SQL Injection via string concatenation.
     *
     * Attacker can login as any user without knowing the password:
     *   username = admin' --
     *   username = ' OR '1'='1
     *
     * ⚠️ VULNERABILITY: No rate limiting — brute force is trivially possible.
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        String username = credentials.get("username");
        String password = credentials.get("password");

        // ⚠️ RAW SQL with string concatenation — NEVER do this
        String sql = "SELECT * FROM users WHERE username = '" + username
                   + "' AND password = '" + password + "'";

        System.out.println("[DEBUG] Executing query: " + sql);

        try {
            @SuppressWarnings("unchecked")
            List<Object[]> results = entityManager.createNativeQuery(sql).getResultList();

            if (!results.isEmpty()) {
                Object[] row = results.get(0);
                User user = new User(
                    ((Number) row[1]).longValue(),  // id
                    (String) row[5],  // username
                    (String) row[3],  // password
                    (String) row[2],  // email
                    (String) row[4],  // role
                    (BigDecimal) row[0]  // balance
                );
                // ⚠️ Token never expires
                String token = jwtUtil.generateToken(user.getUsername(), user.getRole());

                Map<String, Object> response = new HashMap<>();
                response.put("token", token);
                response.put("username", user.getUsername());
                response.put("role", user.getRole());
                response.put("message", "Login successful");

                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
            }
        } catch (Exception e) {
            // ⚠️ Stack trace leaked to client — exposes internals
            return ResponseEntity.status(500).body(Map.of(
                "error", e.getMessage(),
                "query", sql  // ⚠️ Query exposed in response
            ));
        }
    }

    /**
     * Validate JWT token (no expiry check because there is none).
     */
    @GetMapping("/validate")
    public ResponseEntity<?> validate(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        if (jwtUtil.validateToken(token)) {
            return ResponseEntity.ok(Map.of(
                "valid", true,
                "username", jwtUtil.extractUsername(token),
                "role", jwtUtil.extractRole(token)
            ));
        }
        return ResponseEntity.status(401).body(Map.of("valid", false));
    }
}
