/**
 * ============================================
 * FIX #2: XSS → Output Encoding + Sanitization
 * ============================================
 *
 * VULNERABILITY:
 *   User-supplied content is rendered as raw HTML in Thymeleaf
 *   using th:utext (unescaped text), allowing script injection.
 *
 * ROOT CAUSE:
 *   Untrusted input is reflected to the browser without encoding.
 *   The browser interprets it as HTML/JS code.
 *
 * FIX (Defense in Depth):
 *   1. Use th:text (escaped) instead of th:utext
 *   2. Sanitize input on the server before storing (OWASP Java HTML Sanitizer)
 *   3. Set Content Security Policy (CSP) headers
 */

/*
─────────────────────────────────────────────────────────────
FIX 1 — Thymeleaf: use th:text (auto-escapes HTML)
─────────────────────────────────────────────────────────────

❌ VULNERABLE (messages.html):
    <div class="message-content" th:utext="${msg.content}">content</div>

✅ FIXED:
    <div class="message-content" th:text="${msg.content}">content</div>

th:text automatically encodes:
    <script>alert('XSS')</script>
  → &lt;script&gt;alert('XSS')&lt;/script&gt;
  → Displayed as text, NOT executed.


─────────────────────────────────────────────────────────────
FIX 2 — Server-side sanitization with OWASP Java HTML Sanitizer
─────────────────────────────────────────────────────────────

Add to pom.xml:
    <dependency>
        <groupId>com.googlecode.owasp-java-html-sanitizer</groupId>
        <artifactId>owasp-java-html-sanitizer</artifactId>
        <version>20220608.1</version>
    </dependency>

In MessageController.java:

import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;

@PostMapping("/api/messages")
public ResponseEntity<?> postMessage(@RequestBody Map<String, String> body) {
    String rawContent = body.get("content");

    // ✅ Allow basic formatting, strip all scripts/event handlers
    PolicyFactory policy = Sanitizers.FORMATTING.and(Sanitizers.LINKS);
    String safeContent = policy.sanitize(rawContent);

    Message message = new Message(null, author, safeContent);
    messageRepository.save(message);
    ...
}


─────────────────────────────────────────────────────────────
FIX 3 — Content Security Policy (CSP) Header
─────────────────────────────────────────────────────────────

// In SecurityConfig.java — add CSP header to all responses:

http.headers(headers -> headers
    .contentSecurityPolicy(csp -> csp
        .policyDirectives(
            "default-src 'self'; " +
            "script-src 'self'; " +       // ← no inline scripts
            "style-src 'self' 'unsafe-inline'; " +
            "img-src 'self' data:; " +
            "object-src 'none'; " +
            "frame-ancestors 'none';"     // ← also prevents clickjacking
        )
    )
);

// Even if a script tag is injected, CSP blocks its execution
// because it's not from 'self'.


─────────────────────────────────────────────────────────────
FIX 4 — Cookie HttpOnly + Secure flags
─────────────────────────────────────────────────────────────

// If using session cookies, mark them HttpOnly so JS cannot read them:
// application.properties:
server.servlet.session.cookie.http-only=true
server.servlet.session.cookie.secure=true
server.servlet.session.cookie.same-site=strict

// HttpOnly prevents document.cookie from reading the session cookie,
// making cookie-theft XSS payloads ineffective.


─────────────────────────────────────────────────────────────
SUMMARY — XSS Defence Layers
─────────────────────────────────────────────────────────────

Layer 1 (Output): th:text instead of th:utext — encodes on render
Layer 2 (Input):  Server-side sanitization (OWASP) — strip on store
Layer 3 (HTTP):   Content-Security-Policy header — block execution
Layer 4 (Cookie): HttpOnly + Secure — limit blast radius
*/

public class XssFix {
    // Documentation class — see comments above
}
