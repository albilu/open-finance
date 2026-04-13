-- Migration: Create liabilities table
-- Author: Open Finance Development Team
-- Date: 2026-02-01
-- Version: V9
-- Description: Creates the liabilities table for tracking debts, loans, mortgages, and credit obligations
--              Requirement REQ-6.1: Liability Management

-- Create liabilities table
CREATE TABLE liabilities (
    id                  INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id             INTEGER NOT NULL,
    name                VARCHAR(512) NOT NULL,            -- Encrypted field: descriptive name (extra length for encryption overhead)
    type                VARCHAR(20) NOT NULL,             -- LOAN, MORTGAGE, CREDIT_CARD, PERSONAL_LOAN, OTHER
    principal           VARCHAR(512) NOT NULL,            -- Encrypted field: original loan amount (encrypted BigDecimal as String)
    current_balance     VARCHAR(512) NOT NULL,            -- Encrypted field: remaining balance owed (encrypted BigDecimal as String)
    interest_rate       VARCHAR(512),                     -- Encrypted field: annual interest rate as percentage (nullable, encrypted BigDecimal as String)
    start_date          DATE NOT NULL,                    -- Date loan/debt started
    end_date            DATE,                             -- Expected payoff date (nullable for credit cards)
    minimum_payment     VARCHAR(512),                     -- Encrypted field: monthly minimum payment (nullable, encrypted BigDecimal as String)
    currency            VARCHAR(3) NOT NULL,              -- ISO 4217 currency code (USD, EUR, etc.)
    notes               TEXT,                             -- Encrypted field: optional notes about the liability
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign key constraints
    CONSTRAINT fk_liabilities_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,
    
    -- Check constraints
    CONSTRAINT chk_liability_currency_length
        CHECK (LENGTH(currency) = 3),
    
    CONSTRAINT chk_liability_type_valid
        CHECK (type IN ('LOAN', 'MORTGAGE', 'CREDIT_CARD', 'PERSONAL_LOAN', 'OTHER')),
    
    CONSTRAINT chk_liability_dates_logical
        CHECK (end_date IS NULL OR end_date >= start_date)
);

-- Create indexes for performance
-- Index on user_id for fast user-specific liability queries
CREATE INDEX idx_liability_user_id ON liabilities(user_id);

-- Index on type for filtering by liability category
CREATE INDEX idx_liability_type ON liabilities(type);

-- Composite index for user + type queries (common filtering pattern)
CREATE INDEX idx_liability_user_type ON liabilities(user_id, type);

-- Index on start_date for date range queries and reporting
CREATE INDEX idx_liability_start_date ON liabilities(start_date);

-- Index on end_date for finding loans nearing payoff
CREATE INDEX idx_liability_end_date ON liabilities(end_date);

-- Comments for documentation
-- Note: SQLite doesn't support column comments directly, but this serves as documentation
-- 
-- Encrypted fields (service-layer encryption with AES-256):
--   - name: User-friendly description of the liability
--   - principal: Original borrowed amount
--   - current_balance: Amount still owed
--   - interest_rate: Annual percentage rate (APR)
--   - minimum_payment: Required monthly payment
--   - notes: Additional details or payment schedules
--
-- Calculation notes:
--   - Total paid = principal - current_balance (both decrypted)
--   - Total interest can be calculated in service layer using amortization formulas
--   - Interest rate stored as percentage (e.g., 5.25 for 5.25% APR)

-- Migration validation queries (commented out - for reference only)
-- SELECT COUNT(*) FROM liabilities;  -- Should be 0 after migration
-- SELECT * FROM sqlite_master WHERE type='table' AND name='liabilities';  -- Verify table creation
-- SELECT * FROM sqlite_master WHERE type='index' AND tbl_name='liabilities';  -- Verify indexes
