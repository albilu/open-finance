-- Migration: Add opening balance and opening date to accounts table
-- Requirement: REQ-2.6.1.2 - Account Balance Tracking - Historical snapshots
-- Date: 2026-02-17
-- Flyway Migrate: mixed

PRAGMA foreign_keys=OFF;

CREATE TABLE accounts_new (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    name VARCHAR(500) NOT NULL,
    account_type VARCHAR(20) NOT NULL,
    currency CHAR(3) NOT NULL,
    balance NUMERIC(19, 4) NOT NULL DEFAULT 0,
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    opening_balance NUMERIC(19, 4) NOT NULL DEFAULT 0,
    opening_date DATE NOT NULL DEFAULT '2026-01-01'
);

INSERT INTO accounts_new (id, user_id, name, account_type, currency, balance, description, is_active, created_at, updated_at, opening_balance, opening_date)
SELECT id, user_id, name, account_type, currency, balance, description, is_active, created_at, updated_at, balance, DATE('now')
FROM accounts;

DROP TABLE accounts;

ALTER TABLE accounts_new RENAME TO accounts;

CREATE INDEX IF NOT EXISTS idx_account_user_id ON accounts(user_id);
CREATE INDEX IF NOT EXISTS idx_account_type ON accounts(account_type);
CREATE INDEX IF NOT EXISTS idx_account_is_active ON accounts(is_active);
CREATE INDEX IF NOT EXISTS idx_account_user_active ON accounts(user_id, is_active);
CREATE INDEX IF NOT EXISTS idx_account_user_type ON accounts(user_id, account_type);
CREATE INDEX IF NOT EXISTS idx_account_opening_date ON accounts(opening_date);

PRAGMA foreign_keys=ON;