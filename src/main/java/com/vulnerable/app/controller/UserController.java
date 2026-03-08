package com.vulnerable.app.controller;

import com.vulnerable.app.config.JwtUtil;
import com.vulnerable.app.model.User;
import com.vulnerable.app.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * ⚠️ VULNERABILITY #1: SQL Injection in search endpoint
 * ⚠️ VULNERABILITY: No authorization — any user can see all user data
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    @PersistenceContext
    private EntityManager entityManager;

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    /**
     * ⚠️ VULNERABILITY: SQL Injection in search parameter.
     *
     * Normal use: GET /api/users/search?q=alice
     *
     * Attack — dump ALL users:
     *   GET /api/users/search?q=' OR '1'='1
     *
     * Attack — UNION-based data extraction:
     *   GET /api/users/search?q=' UNION SELECT id,username,password,email,role,balance FROM users--
     *
     * ⚠️ Also no authentication check — anyone can call this endpoint
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchUsers(@RequestParam String q) {
        // ⚠️ Direct string interpolation into SQL
        String sql = "SELECT * FROM users WHERE username LIKE '%" + q + "%'";

        System.out.println("[DEBUG] Search query: " + sql);

        try {
            @SuppressWarnings("unchecked")
            List<User> results = entityManager.createNativeQuery(sql, User.class).getResultList();
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            // ⚠️ Full exception + query exposed
            return ResponseEntity.status(500).body(Map.of(
                "error", e.getMessage(),
                "query", sql
            ));
        }
    }

    /**
     * ⚠️ VULNERABILITY: IDOR — user ID in URL, no ownership check.
     * Any authenticated user can access ANY user's profile by changing the ID.
     *
     * Attack: GET /api/users/1  (to see admin profile even if logged in as bob)
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        return userRepository.findById(id)
                .map(user -> ResponseEntity.ok((Object) user))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * List all users — no auth required.
     * ⚠️ Returns passwords in plaintext.
     */
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userRepository.findAll());
    }
}
