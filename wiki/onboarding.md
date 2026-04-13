# Onboarding

← [Wiki Home](HOME.md)

---

## Overview

The onboarding flow runs once for each new user on their first login. It collects locale and currency preferences so that all subsequent views are pre-configured correctly from the start.

---

## Onboarding Steps

### Step 1 — Welcome

Briefly introduces Open-Finance and confirms your account is ready.

### Step 2 — Country & Region

Select your country. This is used to:

- Pre-select your base currency (e.g., France → EUR).
- Configure number formatting (decimal and thousands separators).
- Set the default date format (DD/MM/YYYY vs MM/DD/YYYY).

### Step 3 — Currency Setup

| Setting            | Description                                              |
| ------------------ | -------------------------------------------------------- |
| Base Currency      | Primary currency for all totals, dashboards, and reports |
| Secondary Currency | Optional second currency displayed alongside the base    |

Exchange rates between your currencies are fetched daily from the ECB. You can also configure how the currency symbol is displayed (before/after the number, spacing).

### Step 4 — Language

Select **English** or **French**. The UI language switches immediately.

### Step 5 — Date & Number Format

Fine-tune formatting:

- Date format: DD/MM/YYYY, MM/DD/YYYY, or YYYY-MM-DD
- Decimal separator: `.` or `,`
- Thousands separator: `,`, `.`, or space

### Step 6 — Completion

Onboarding is marked complete and you are redirected to the main Dashboard. The onboarding flow is not shown again.

---

## Skipping Onboarding

You can skip the flow at any time — all defaults are applied. To re-run the onboarding flow later, go to **Settings → Account → Reset Onboarding**.

---

## Related Pages

- [Settings & Profile](settings.md)
- [Market Data](market-data.md)
