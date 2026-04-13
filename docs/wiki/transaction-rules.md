# Transaction Rules Engine

← [Wiki Home](HOME.md)

---

## Overview

The Transaction Rules Engine automatically categorises, tags, and processes transactions when they are created or imported. Rules are evaluated in priority order and can be configured to stop after the first match or to continue applying all matching rules.

---

## How Rules Work

When a new transaction arrives, Open-Finance evaluates your active rules in order. For each rule:

1. All conditions in the rule are checked (using AND or OR logic).
2. If the conditions match, the rule’s actions are applied.
3. If the rule is set to **stop on first match**, evaluation ends; otherwise, the next rule is checked.

---

## Rule Conditions

Each rule has one or more conditions. A condition tests a field against a value using an operator.

### Fields you can test

| Field       | Description                    |
| ----------- | ------------------------------ |
| Description | Transaction description / memo |
| Amount      | Transaction amount             |
| Payee       | Payee name                     |
| Category    | Current category name          |
| Account     | Account name                   |
| Notes       | Transaction notes              |

### Available operators

| Operator     | Meaning                                     |
| ------------ | ------------------------------------------- |
| Contains     | Field contains the value (case-insensitive) |
| Equals       | Exact match                                 |
| Starts with  | Field starts with value                     |
| Ends with    | Field ends with value                       |
| Greater than | Numeric comparison                          |
| Less than    | Numeric comparison                          |
| Regex        | Regular expression match                    |

### Condition match mode

| Mode | Behaviour                                    |
| ---- | -------------------------------------------- |
| All  | All conditions must match (AND logic)        |
| Any  | At least one condition must match (OR logic) |

---

## Rule Actions

When a rule matches, one or more actions are applied to the transaction:

| Action       | Effect                                           |
| ------------ | ------------------------------------------------ |
| Set Category | Assign the specified category                    |
| Set Payee    | Assign the specified payee                       |
| Add Tag      | Append a tag to the transaction                  |
| Set Notes    | Replace or append notes                          |
| Set Type     | Override the transaction type (Income / Expense) |

---

## Creating a Rule

Go to **Settings → Transaction Rules → New Rule**:

1. Give the rule a **name** and optionally a **description**.
2. Set the **priority** (lower number = evaluated first).
3. Add one or more **conditions**.
4. Choose the **condition match** mode (All / Any).
5. Add one or more **actions**.
6. Toggle the rule **active**.

**Example:** A rule named “Supermarket → Groceries” with condition `Description contains LECLERC` and action `Set Category = Groceries` will automatically categorise all Leclerc transactions.

---

## Enabling and Disabling Rules

Rules can be toggled on or off without deleting them. Use the toggle switch next to each rule in the rules list.

---

## Re-applying Rules to Existing Transactions

Rules apply automatically to new transactions. To retroactively apply your current rules to all existing transactions, go to **Settings → Transaction Rules → Re-apply All Rules**.

---

## Related Pages

- [Transactions](transactions.md)
- [Categories](categories.md)
- [Payees](payees.md)
- [Import & Export](import-export.md)
