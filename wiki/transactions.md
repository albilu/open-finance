# Transactions

← [Wiki Home](HOME.md)

---

## Overview

Transactions are the primary record of financial activity. Each transaction belongs to one account and can optionally be split across multiple categories, linked to a payee, tagged, or annotated with notes and attachments.

---

## Transaction Types

| Type     | Description                                  |
| -------- | -------------------------------------------- |
| Income   | Money received (salary, interest, dividends) |
| Expense  | Money spent                                  |
| Transfer | Movement between two of your accounts        |

---

## Core Fields

| Field              | Notes                                               |
| ------------------ | --------------------------------------------------- |
| Date               | The date the transaction occurred                   |
| Amount             | The transaction value                               |
| Type               | Income, Expense, or Transfer                        |
| Category           | Optional; drives budget tracking and insights       |
| Payee              | Optional; link to the payee registry                |
| Payment Method     | How the payment was made (see below)                |
| Notes              | Free text for your own reference                    |
| Tags               | Labels you can use to filter and group transactions |
| External Reference | Bank or broker reference ID for reconciliation      |

---

## Payment Methods

| Method         | Description                   |
| -------------- | ----------------------------- |
| Cash           | Physical currency             |
| Cheque         | Paper cheque                  |
| Credit Card    | Credit card                   |
| Debit Card     | Debit card                    |
| Bank Transfer  | Wire or bank transfer         |
| Deposit        | Direct deposit (e.g., salary) |
| Standing Order | Recurring scheduled payment   |
| Direct Debit   | Authorised automatic debit    |
| Online         | Online payment (PayPal, etc.) |
| Other          | Catch-all                     |

---

## Split Transactions

A single transaction can be split into multiple line items, each with its own amount and category. This is useful when one payment covers several categories — for example, a supermarket receipt that includes groceries, household goods, and personal care items.

- The sum of all split amounts must equal the parent transaction amount.
- Each split line can have its own category and an optional note.

**How to split:** Open a transaction → click **Split** → add rows and assign categories → save.

---

## Bulk Edit

Select multiple transactions in the list view and apply a shared category, tag, payee, or date adjustment to all of them at once. This saves time when categorising a batch of imported transactions.

---

## Transfer Transactions

Creating a transfer links two accounts together:

1. A debit is recorded on the source account.
2. A credit is recorded on the destination account.

Deleting either record removes both. The transfer pair appears as a single linked item in each account’s ledger.

---

## Transaction Archiving

Transactions older than three years are automatically archived by the system. Archived transactions:

- Are excluded from the main transaction list by default.
- Are still searchable via the global search bar.

This keeps your active transaction list fast and manageable without losing historical data.

---

## Related Pages

- [Transaction Rules](transaction-rules.md)
- [Recurring Transactions](recurring-transactions.md)
- [Categories](categories.md)
- [Payees](payees.md)
- [Import & Export](import-export.md)
- [Global Search](search.md)
