/**
 * ============================================
 * FIX #3: Broken Authentication
 * ============================================
 *
 * VULNERABILITIES:
 *   A. JWT has no expiration time → tokens valid forever
 *   B. No rate limiting on /login → brute force trivial
 *   C. Weak hardcoded JWT secret → forgeable tokens
 *   D. No token revocation mechanism
 *
 * FIXES:
 *   A. Set expiration (15 min access + refresh token pattern)
 *   B. Rate limiting with Bucket4j or Spring Boot Actuator
 *   C. Strong secret from environment variable
 *   D. Token blacklist (Redis) or short-lived tokens
 */

/*
─────────────────────────────────────────────────────────────
FIX A — JWT Expiration
─────────────────────────────────────────────────────────────

❌ VULNERABLE (JwtUtil.java):
    return Jwts.builder()
        .setSubject(username)
        .setIssuedAt(new Date())
        // No expiration set → lives forever
        .signWith(SignatureAlgorithm.HS256, secret)
        .compact();

✅ FIXED — 15 minute access token + 7 day refresh token:

    private static final long ACCESS_TOKEN_EXPIRY  = 15 * 60 * 1000L;       // 15 min
    private static final long REFRESH_TOKEN_EXPIRY = 7  * 24 * 60 * 60 * 1000L; // 7 days

    public String generateAccessToken(String username, String role) {
        return Jwts.builder()
            .setSubject(username)
            .claim("role", role)
            .claim("type", "access")
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRY))
            .signWith(SignatureAlgorithm.HS256, secret)
            .compact();
    }

    public String generateRefreshToken(String username) {
        return Jwts.builder()
            .setSubject(username)
            .claim("type", "refresh")
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + REFRESH_TOKEN_EXPIRY))
            .signWith(SignatureAlgorithm.HS256, secret)
            .compact();
    }

    // Validation now checks expiry automatically
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .setSigningKey(secret)
                .parseClaimsJws(token);  // ← throws ExpiredJwtException if expired
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("Token expired: {}", e.getMessage());
            return false;
        } catch (JwtException e) {
            log.warn("Invalid token: {}", e.getMessage());
            return false;
        }
    }


─────────────────────────────────────────────────────────────
FIX B — Rate Limiting with Bucket4j
─────────────────────────────────────────────────────────────

// pom.xml:
//   <dependency>
//       <groupId>com.bucket4j</groupId>
//       <artifactId>bucket4j-core</artifactId>
//       <version>8.7.0</version>
//   </dependency>

@Component
public class LoginRateLimiter {
    // Per-IP: max 5 login attempts per minute
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public Bucket resolveBucket(String ipAddress) {
        return buckets.computeIfAbsent(ipAddress, ip ->
            Bucket.builder()
                .addLimit(Bandwidth.classic(5, Refill.greedy(5, Duration.ofMinutes(1))))
                .build()
        );
    }
}

// In AuthController.java:
@PostMapping("/login")
public ResponseEntity<?> login(
        @RequestBody Map<String, String> credentials,
        HttpServletRequest request) {

    String ip = request.getRemoteAddr();
    Bucket bucket = loginRateLimiter.resolveBucket(ip);

    if (!bucket.tryConsume(1)) {
        return ResponseEntity.status(429).body(Map.of(
            "error", "Too many login attempts. Try again in 1 minute."
        ));
    }

    // ... proceed with authentication
}


─────────────────────────────────────────────────────────────
FIX C — Strong Secret from Environment
─────────────────────────────────────────────────────────────

❌ VULNERABLE (application.properties):
    jwt.secret=secret123

✅ FIXED — generate a 256-bit secret:

    # application.properties
    jwt.secret=${JWT_SECRET}   # ← from environment variable, never hardcoded

    # Generate a strong secret (run once):
    # openssl rand -base64 32

    # In docker-compose.yml:
    environment:
      JWT_SECRET: "your-256-bit-base64-encoded-secret-here"

    # In JwtUtil.java — use Keys helper for proper key derivation:
    @Value("${jwt.secret}")
    private String secret;

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }


─────────────────────────────────────────────────────────────
FIX D — Token Revocation with Redis Blacklist
─────────────────────────────────────────────────────────────

// pom.xml:
//   spring-boot-starter-data-redis

@Service
public class TokenBlacklist {
    private final RedisTemplate<String, String> redis;

    public void revoke(String token, long expiryMs) {
        redis.opsForValue().set(
            "blacklist:" + token,
            "revoked",
            expiryMs, TimeUnit.MILLISECONDS
        );
    }

    public boolean isRevoked(String token) {
        return Boolean.TRUE.equals(
            redis.hasKey("blacklist:" + token)
        );
    }
}

// In logout endpoint:
@PostMapping("/logout")
public ResponseEntity<?> logout(@RequestHeader("Authorization") String authHeader) {
    String token = authHeader.replace("Bearer ", "");
    long remaining = jwtUtil.getExpirationMs(token) - System.currentTimeMillis();
    tokenBlacklist.revoke(token, remaining);
    return ResponseEntity.ok(Map.of("message", "Logged out"));
}

// In JWT filter — check blacklist before accepting token:
if (tokenBlacklist.isRevoked(token)) {
    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token revoked");
    return;
}


─────────────────────────────────────────────────────────────
ADDITIONAL — Account Lockout
─────────────────────────────────────────────────────────────

// Track failed attempts per username and lock after 10 failures:
@Service
public class LoginAttemptService {
    private final Map<String, Integer> attempts = new ConcurrentHashMap<>();

    public void registerFailure(String username) {
        attempts.merge(username, 1, Integer::sum);
    }

    public void registerSuccess(String username) {
        attempts.remove(username);
    }

    public boolean isLocked(String username) {
        return attempts.getOrDefault(username, 0) >= 10;
    }
}
*/

public class BrokenAuthFix {
    // Documentation class — see comments above
}
