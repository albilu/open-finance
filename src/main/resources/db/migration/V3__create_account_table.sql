-- Requirement REQ-2.2: Create accounts table for financial account management
-- This table stores user financial accounts with encryption for sensitive fields
-- Account names and descriptions are encrypted by AccountService before storage

-- Note: SQLite requires INTEGER PRIMARY KEY AUTOINCREMENT (not BIGINT)
-- SQLite maps INTEGER PRIMARY KEY to 64-bit signed integers (compatible with Java Long)
CREATE TABLE accounts (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    name VARCHAR(500) NOT NULL,                -- Encrypted field, longer for encrypted data
    account_type VARCHAR(20) NOT NULL,         -- CHECKING, SAVINGS, CREDIT_CARD, INVESTMENT, CASH, OTHER
    currency CHAR(3) NOT NULL,                  -- ISO 4217 currency code (USD, EUR, etc.)
    balance DECIMAL(19,4) NOT NULL DEFAULT 0.0, -- Current account balance
    description TEXT,                           -- Encrypted field (optional)
    is_active BOOLEAN NOT NULL DEFAULT 1,       -- Soft delete flag
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    
    -- Foreign key to users table
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    
    -- Constraints
    CONSTRAINT chk_currency CHECK (LENGTH(currency) = 3),
    CONSTRAINT chk_account_type CHECK (account_type IN ('CHECKING', 'SAVINGS', 'CREDIT_CARD', 'INVESTMENT', 'CASH', 'OTHER'))
);

-- Index on user_id for faster lookups of user's accounts
CREATE INDEX idx_account_user_id ON accounts(user_id);

-- Index on account_type for filtering by type
CREATE INDEX idx_account_type ON accounts(account_type);

-- Index on is_active for filtering active/inactive accounts
CREATE INDEX idx_account_is_active ON accounts(is_active);

-- Composite index for common query: find active accounts by user
CREATE INDEX idx_account_user_active ON accounts(user_id, is_active);

-- Composite index for query: find accounts by user and type
CREATE INDEX idx_account_user_type ON accounts(user_id, account_type);
