# Security Features

← [Wiki Home](HOME.md)

---

## Overview

Open-Finance is built with security as a priority. This page describes the security protections in place to keep your financial data safe.

---

## Authentication

Every session is secured with a signed token that expires after 24 hours. You are automatically prompted to log in again when your session expires.

Passwords are stored using a strong one-way hash — your actual password is never saved anywhere in the system.

---

## Master Password & Data Encryption

Your financial data (account names, transaction amounts, balances, notes, and more) is **encrypted at rest** using AES-256, one of the strongest encryption standards available. The encryption key is derived from your master password and never leaves your device in a usable form.

The following data is always encrypted in the database:

- Account names and account numbers
- Transaction amounts and notes
- Asset names and values
- Liability balances and names
- Real estate values and names

This means that even if someone accessed the database file directly, they could not read your financial data without your master password.

---

## Account Lockout

After **5 consecutive failed login attempts**, your account is automatically locked for **15 minutes**. This protects against brute-force password guessing. The lockout resets as soon as you log in successfully.

---

## Security Audit Log

Open-Finance keeps an internal log of authentication events:

| Event            | When it is recorded             |
| ---------------- | ------------------------------- |
| Login success    | Every successful login          |
| Login failed     | Wrong password entered          |
| Account locked   | Lockout threshold reached       |
| Password changed | You changed your login password |

Each log entry records the timestamp, IP address, and browser information. Administrators can review this log to detect suspicious activity.

---

## Rate Limiting

To prevent abuse, the number of requests you can make is limited:

- **Login attempts:** 10 per minute per IP address
- **File uploads:** 10 per minute per IP address
- **All other actions:** 200 per minute per user account

If you exceed a limit, you will receive a message asking you to wait before retrying.

---

## Input Protection

All text you enter is automatically sanitised to prevent cross-site scripting (XSS) attacks. SQL injection is prevented throughout the application by using parameterized database queries.

---

## Changing Your Master Password

Go to **Settings → Security → Change Master Password**. Changing your master password triggers a full re-encryption of all your sensitive data with the new key. The process is atomic — if anything goes wrong, your old password remains valid.

---

## Related Pages

- [Backup & Restore](backup-restore.md)
- [Onboarding](onboarding.md)
- [Settings & Profile](settings.md)
