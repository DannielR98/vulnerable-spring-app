# VulnBank — Threat Model

## Application Overview

VulnBank is a simulated online banking portal that allows users to:
- Log in with username and password
- View their account balance
- Post messages on a message board
- Search for users

## Assets

| Asset               | Sensitivity | Notes                        |
|---------------------|-------------|------------------------------|
| User credentials    | Critical    | Stored in plaintext          |
| Account balances    | High        | Financial data               |
| JWT tokens          | High        | No expiry, forgeable         |
| User PII (email)    | Medium      | Exposed via SQLi             |
| Message board       | Low         | XSS attack surface           |

## Threat Actors

| Actor          | Capability | Motivation                        |
|----------------|------------|-----------------------------------|
| External hacker| Medium     | Financial gain, data theft        |
| Competitor     | Medium     | Business intelligence             |
| Curious user   | Low        | Privilege escalation              |
| Automated scanner | High    | Opportunistic exploitation        |

## Attack Surface

```
Internet → HTTP :8080 → [Spring Boot App]
                              ├── /api/auth/login    (SQLi, brute force)
                              ├── /api/users/search  (SQLi, IDOR)
                              ├── /messages          (Stored XSS)
                              ├── /h2-console        (Direct DB access!)
                              └── /api/auth/validate (No token revocation)
```

## Vulnerability Summary (STRIDE)

| Threat              | Type       | Vulnerability                  | Severity |
|---------------------|------------|--------------------------------|----------|
| Credential bypass   | Spoofing   | SQL injection in login         | Critical |
| Data theft          | Info Disc. | SQL injection in search        | Critical |
| Account takeover    | Spoofing   | Brute force (no rate limit)    | High     |
| Token reuse         | Elevation  | JWT without expiry             | High     |
| Script injection    | Tampering  | Stored XSS in messages         | High     |
| Session hijack      | Spoofing   | XSS cookie theft               | High     |
| Token forgery       | Spoofing   | Weak JWT secret                | High     |
| DB access           | Info Disc. | H2 console exposed             | Medium   |
| Info leakage        | Info Disc. | SQL errors returned to client  | Medium   |
| Password exposure   | Info Disc. | Plaintext passwords stored     | Critical |
