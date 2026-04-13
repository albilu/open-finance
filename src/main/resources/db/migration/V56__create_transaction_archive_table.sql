-- Migration: Create transaction archive table for old data
-- Version: V56
-- Description: Creates an archive table for transactions older than 3 years.
--              Data in the archive table reduces the size of the active transactions
--              table, improving query performance.
--              The archive table mirrors the schema of the transactions table
--              (all columns present as of V49) plus an archived_at timestamp.
-- Author: Open-Finance
-- Date: 2026-03-20
-- Requirement REQ-3.1: Performance – archive old data to keep active dataset small

CREATE TABLE IF NOT EXISTS transactions_archive (
    id                  INTEGER PRIMARY KEY,
    user_id             INTEGER NOT NULL,
    account_id          INTEGER NOT NULL,
    to_account_id       INTEGER,
    transaction_type    TEXT NOT NULL CHECK(transaction_type IN ('INCOME','EXPENSE','TRANSFER')),
    amount              REAL NOT NULL,
    currency            TEXT NOT NULL,
    category_id         INTEGER,
    transaction_date    TEXT NOT NULL,
    description         TEXT,
    notes               TEXT,
    tags                TEXT,
    payee               TEXT,
    is_reconciled       INTEGER NOT NULL DEFAULT 0,
    is_deleted          INTEGER NOT NULL DEFAULT 0,
    transfer_id         VARCHAR(36),
    payment_method      VARCHAR(20),
    liability_id        INTEGER,
    external_reference  VARCHAR(255),
    created_at          TEXT NOT NULL,
    updated_at          TEXT,
    archived_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes on archive table for efficient archive browsing
CREATE INDEX IF NOT EXISTS idx_txn_archive_user_date
    ON transactions_archive(user_id, transaction_date DESC);

CREATE INDEX IF NOT EXISTS idx_txn_archive_account
    ON transactions_archive(account_id);
