-- Migration: Create transactions table
-- Version: V5
-- Description: Creates the transactions table for recording financial movements
-- Author: Open-Finance
-- Date: 2026-01-31

-- Transactions table for tracking income, expenses, and transfers
-- Supports encryption for sensitive fields and soft deletes
-- Requirement REQ-2.4.1: Transaction management
CREATE TABLE transactions (
    id                  INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id             INTEGER NOT NULL,
    account_id          INTEGER NOT NULL,                  -- Source account
    to_account_id       INTEGER,                            -- Destination account (for transfers only)
    transaction_type    TEXT NOT NULL CHECK (transaction_type IN ('INCOME', 'EXPENSE', 'TRANSFER')),
    amount              REAL NOT NULL,                      -- Positive decimal value
    currency            TEXT NOT NULL,                      -- ISO 4217 currency code (3 chars)
    category_id         INTEGER,                            -- Optional category reference
    transaction_date    TEXT NOT NULL,                      -- ISO 8601 date (YYYY-MM-DD)
    description         TEXT,                               -- Encrypted brief description
    notes               TEXT,                               -- Encrypted detailed notes
    tags                TEXT,                               -- Comma-separated tags
    payee               TEXT,                               -- Payee/payer name
    is_reconciled       INTEGER NOT NULL DEFAULT 0,        -- Reconciliation status (0=false, 1=true)
    is_deleted          INTEGER NOT NULL DEFAULT 0,        -- Soft delete flag (0=false, 1=true)
    created_at          TEXT NOT NULL,                      -- ISO 8601 datetime
    updated_at          TEXT,                               -- ISO 8601 datetime
    
    -- Foreign key constraint - ensures transaction belongs to valid user
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    
    -- Foreign key constraint - source account must exist
    FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE RESTRICT,
    
    -- Foreign key constraint - destination account must exist (for transfers)
    FOREIGN KEY (to_account_id) REFERENCES accounts(id) ON DELETE RESTRICT,
    
    -- Foreign key constraint - category must exist (optional)
    FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL,
    
    -- Business rule constraints
    CHECK (amount > 0),                                     -- Amount must be positive
    CHECK (LENGTH(currency) = 3)                            -- Currency must be 3-letter code
);

-- Index for efficient user-scoped queries
-- Primary filter for all transaction queries
CREATE INDEX idx_transaction_user_id ON transactions(user_id);

-- Index for account-based queries
-- Used for account transaction history and balance calculations
CREATE INDEX idx_transaction_account_id ON transactions(account_id);

-- Index for category-based queries
-- Used for category reports and budget tracking
CREATE INDEX idx_transaction_category_id ON transactions(category_id);

-- Index for date-based queries
-- Critical for reports, statements, and date range filters
CREATE INDEX idx_transaction_date ON transactions(transaction_date);

-- Index for transaction type filtering
-- Used for income/expense/transfer reports
CREATE INDEX idx_transaction_type ON transactions(transaction_type);

-- Composite index for common query pattern: user's transactions by date
-- Optimizes queries like "get all transactions for user in date range"
CREATE INDEX idx_transaction_user_date ON transactions(user_id, transaction_date);

-- Composite index for account transaction history with date
-- Optimizes queries like "get account transactions ordered by date"
CREATE INDEX idx_transaction_account_date ON transactions(account_id, transaction_date);

-- Index for soft-delete queries
-- Ensures deleted transactions are efficiently excluded from queries
CREATE INDEX idx_transaction_is_deleted ON transactions(is_deleted);

-- Index for reconciliation queries
-- Used to find unreconciled transactions
CREATE INDEX idx_transaction_is_reconciled ON transactions(is_reconciled);

-- Comments explaining the table structure
-- SQLite doesn't support native column comments, so documenting here:
--
-- TRANSACTION TYPES:
-- - INCOME: Money received (salary, dividends, gifts)
--   - account_id: Account receiving money
--   - to_account_id: NULL
--   - Amount increases account balance
--
-- - EXPENSE: Money spent (groceries, rent, utilities)
--   - account_id: Account spending money
--   - to_account_id: NULL
--   - Amount decreases account balance
--
-- - TRANSFER: Money moved between accounts
--   - account_id: Source account (money leaving)
--   - to_account_id: Destination account (money arriving)
--   - Decreases source balance, increases destination balance
--   - Net worth unchanged (internal movement)
--
-- SOFT DELETES:
-- - is_deleted=0: Active transaction
-- - is_deleted=1: Logically deleted (preserved for audit)
-- Soft deletes maintain referential integrity and audit trail
--
-- RECONCILIATION:
-- - is_reconciled=0: Not yet reconciled with bank statement
-- - is_reconciled=1: Verified against bank statement
-- Used for ensuring transaction accuracy
--
-- ENCRYPTION:
-- - description and notes fields are encrypted for privacy
-- - Encryption/decryption handled in application layer
--
-- BALANCE IMPACT:
-- - Application layer updates account.balance when transaction created/modified/deleted
-- - Transactions are the source of truth for balance calculations
