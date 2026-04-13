# Accounts

← [Wiki Home](HOME.md)

---

## Overview

Accounts are the foundation of Open-Finance. Every transaction, import, and budget is linked to an account. Accounts represent your real-world bank accounts, investment accounts, wallets, and other financial containers.

---

## Account Types

| Type        | Description                                    |
| ----------- | ---------------------------------------------- |
| Checking    | Day-to-day spending account                    |
| Savings     | High-interest or emergency fund account        |
| Credit Card | Revolving credit facility                      |
| Investment  | Brokerage or retirement account                |
| Loan        | Borrowed funds (links to a liability record)   |
| Cash        | Physical cash holdings                         |
| Other       | Any account that does not fit above categories |

---

## Creating an Account

Go to **Dashboard → Accounts → New Account** and fill in the following:

| Field           | Notes                                            |
| --------------- | ------------------------------------------------ |
| Name            | A label for this account (e.g., "Main Checking") |
| Type            | Select from the account types above              |
| Currency        | Defaults to your base currency                   |
| Opening Balance | The starting balance when you begin tracking     |

Optional fields include your account number, the linked financial institution, and free-text notes.

---

## Balance History

Open-Finance automatically calculates your account balance based on the opening balance plus all recorded transactions. Your current balance and recent changes are visible on the account summary in the Dashboard.

---

## Closing & Reopening Accounts

You can close an account without losing its history:

- **Close:** Open the account → **Edit** → set status to **Closed**. The account is hidden from active views, but all transactions are preserved.
- **Reopen:** Edit the account again and set the status back to **Active**.
- **Permanent delete:** Use **Delete Account** in the account menu. This removes the account and all its transactions permanently and cannot be undone.

---

## Transfer Transactions

When you move money between two of your accounts, Open-Finance records a matched pair of transactions — a debit on the source and a credit on the destination. Both sides appear in their respective account ledgers and are linked together. Deleting one side automatically removes both.

---

## Related Pages

- [Transactions](transactions.md)
- [Import & Export](import-export.md)
- [Financial Institutions](institutions.md)
- [Dashboard](dashboard.md)
