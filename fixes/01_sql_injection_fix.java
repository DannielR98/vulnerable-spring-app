package com.vulnerable.app.fixes;

/**
 * ============================================
 * FIX #1: SQL Injection → Parameterized Queries
 * ============================================
 *
 * VULNERABILITY:
 *   String concatenation builds SQL queries with user input.
 *   Attacker can escape the string context and inject SQL.
 *
 * ROOT CAUSE:
 *   User data is treated as SQL code, not data.
 *
 * FIX:
 *   Use parameterized queries / PreparedStatements / Spring Data JPA.
 *   User input is never interpreted as SQL code.
 *
 * This file shows the FIXED version of AuthController and UserController.
 */

/*
─────────────────────────────────────────────────────────────
VULNERABLE CODE (AuthController.java)
─────────────────────────────────────────────────────────────

// ❌ VULNERABLE — string concatenation
String sql = "SELECT * FROM users WHERE username = '" + username
           + "' AND password = '" + password + "'";
entityManager.createNativeQuery(sql, User.class).getResultList();


─────────────────────────────────────────────────────────────
FIX OPTION A — Named parameters in native query
─────────────────────────────────────────────────────────────

@PostMapping("/login")
public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
    String username = credentials.get("username");
    String password = credentials.get("password");

    // ✅ FIXED: Named parameters — user input never touches SQL syntax
    String sql = "SELECT * FROM users WHERE username = :username AND password = :password";

    List<User> results = entityManager
        .createNativeQuery(sql, User.class)
        .setParameter("username", username)   // ← safely escaped by JDBC
        .setParameter("password", password)
        .getResultList();

    if (!results.isEmpty()) { ... }
}


─────────────────────────────────────────────────────────────
FIX OPTION B — Spring Data JPA Repository (BEST PRACTICE)
─────────────────────────────────────────────────────────────

// In UserRepository.java:
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsernameAndPassword(String username, String password);
}

// In AuthController.java:
@PostMapping("/login")
public ResponseEntity<?> login(@RequestBody Map<String, String> creds) {
    // ✅ JPA generates safe parameterized SQL automatically
    Optional<User> user = userRepository
        .findByUsernameAndPassword(creds.get("username"), creds.get("password"));

    return user.map(u -> {
        String token = jwtUtil.generateToken(u.getUsername(), u.getRole());
        return ResponseEntity.ok(Map.of("token", token, "username", u.getUsername()));
    }).orElse(ResponseEntity.status(401).body(Map.of("error", "Invalid credentials")));
}


─────────────────────────────────────────────────────────────
FIX OPTION C — JPQL (Java Persistence Query Language)
─────────────────────────────────────────────────────────────

// JPQL uses entity names, not table names, and is always parameterized
String jpql = "SELECT u FROM User u WHERE u.username = :username AND u.password = :password";

List<User> results = entityManager
    .createQuery(jpql, User.class)
    .setParameter("username", username)
    .setParameter("password", password)
    .getResultList();


─────────────────────────────────────────────────────────────
ADDITIONAL FIX — Hash passwords (BCrypt)
─────────────────────────────────────────────────────────────

// pom.xml — already in Spring Security starter:
// <dependency>
//     <groupId>org.springframework.boot</groupId>
//     <artifactId>spring-boot-starter-security</artifactId>
// </dependency>

@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}

// At registration:
user.setPassword(passwordEncoder.encode(rawPassword));

// At login — compare with hash, not plaintext:
boolean valid = passwordEncoder.matches(rawPassword, storedHash);


─────────────────────────────────────────────────────────────
ADDITIONAL FIX — Never expose query in error response
─────────────────────────────────────────────────────────────

// ❌ VULNERABLE
return ResponseEntity.status(500).body(Map.of(
    "error", e.getMessage(),
    "query", sql   // ← exposes internal SQL structure
));

// ✅ FIXED — generic error, log internally
log.error("Database error", e);   // log full error server-side
return ResponseEntity.status(500).body(Map.of("error", "Internal server error"));

─────────────────────────────────────────────────────────────
*/

public class SqlInjectionFix {
    // This class is documentation — see comments above
}
