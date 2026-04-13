-- Migration: Create recurring_transactions table
-- Version: V23
-- Description: Creates the recurring_transactions table for templates that auto-generate transactions
-- Author: Open-Finance
-- Date: 2026-02-03
-- Requirement: REQ-2.3.6 - Recurring transactions

-- Recurring transactions table for templates that automatically create transactions
-- Supports DAILY, WEEKLY, BIWEEKLY, MONTHLY, QUARTERLY, YEARLY frequencies
-- Scheduled job processes due recurring transactions daily
CREATE TABLE recurring_transactions (
    id                  INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id             INTEGER NOT NULL,
    account_id          INTEGER NOT NULL,                  -- Source account
    to_account_id       INTEGER,                            -- Destination account (for transfers only)
    transaction_type    TEXT NOT NULL CHECK (transaction_type IN ('INCOME', 'EXPENSE', 'TRANSFER')),
    amount              REAL NOT NULL,                      -- Positive decimal value
    currency            TEXT NOT NULL,                      -- ISO 4217 currency code (3 chars)
    category_id         INTEGER,                            -- Optional category reference
    description         TEXT NOT NULL,                      -- Encrypted brief description (e.g., "Monthly Rent")
    notes               TEXT,                               -- Encrypted detailed notes
    frequency           TEXT NOT NULL CHECK (frequency IN ('DAILY', 'WEEKLY', 'BIWEEKLY', 'MONTHLY', 'QUARTERLY', 'YEARLY')),
    next_occurrence     TEXT NOT NULL,                      -- ISO 8601 date (YYYY-MM-DD) - next scheduled date
    end_date            TEXT,                               -- ISO 8601 date - optional end date for recurring
    is_active           INTEGER NOT NULL DEFAULT 1,        -- Active status (0=paused, 1=active)
    created_at          TEXT NOT NULL,                      -- ISO 8601 datetime
    updated_at          TEXT NOT NULL,                      -- ISO 8601 datetime
    
    -- Foreign key constraint - ensures recurring transaction belongs to valid user
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    
    -- Foreign key constraint - source account must exist
    FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE CASCADE,
    
    -- Foreign key constraint - destination account must exist (for transfers)
    FOREIGN KEY (to_account_id) REFERENCES accounts(id) ON DELETE CASCADE,
    
    -- Foreign key constraint - category must exist (optional)
    FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL,
    
    -- Business rule constraints
    CHECK (amount > 0),                                     -- Amount must be positive
    CHECK (LENGTH(currency) = 3)                            -- Currency must be 3-letter code
);

-- Index for efficient user-scoped queries
-- Primary filter for all recurring transaction queries
CREATE INDEX idx_recurring_user_id ON recurring_transactions(user_id);

-- Index for account-based queries
-- Used to find recurring transactions for a specific account
CREATE INDEX idx_recurring_account_id ON recurring_transactions(account_id);

-- Index for next occurrence date queries
-- CRITICAL for scheduled job - finds due recurring transactions efficiently
CREATE INDEX idx_recurring_next_occurrence ON recurring_transactions(next_occurrence);

-- Index for active status filtering
-- Used by scheduled job to process only active recurring transactions
CREATE INDEX idx_recurring_is_active ON recurring_transactions(is_active);

-- Composite index for scheduled job query optimization
-- Optimizes: "find active recurring transactions due today"
CREATE INDEX idx_recurring_active_next_occurrence ON recurring_transactions(is_active, next_occurrence);

-- Index for frequency-based queries
-- Used for analytics and bulk operations by frequency
CREATE INDEX idx_recurring_frequency ON recurring_transactions(frequency);

-- Composite index for user's active recurring transactions
-- Optimizes: "get user's active recurring transactions"
CREATE INDEX idx_recurring_user_active ON recurring_transactions(user_id, is_active);

-- Comments explaining the table structure
-- SQLite doesn't support native column comments, so documenting here:
--
-- FREQUENCY TYPES:
-- - DAILY: Creates transaction every day
--   - Next occurrence: current date + 1 day
-- - WEEKLY: Creates transaction every 7 days
--   - Next occurrence: current date + 7 days
-- - BIWEEKLY: Creates transaction every 14 days
--   - Next occurrence: current date + 14 days
-- - MONTHLY: Creates transaction on same day each month
--   - Next occurrence: same day of next month (handles month-end appropriately)
-- - QUARTERLY: Creates transaction every 3 months
--   - Next occurrence: current date + 3 months
-- - YEARLY: Creates transaction on same date each year
--   - Next occurrence: same date next year (handles leap year Feb 29 appropriately)
--
-- PROCESSING WORKFLOW:
-- 1. Scheduled job runs daily (e.g., at midnight)
-- 2. Query: SELECT * WHERE is_active=1 AND next_occurrence <= today AND (end_date IS NULL OR end_date >= today)
-- 3. For each due recurring transaction:
--    a. Create actual transaction in transactions table
--    b. Calculate next_occurrence based on frequency
--    c. Update recurring_transactions.next_occurrence
--    d. If next_occurrence > end_date, set is_active=0
-- 4. Transaction creation uses encrypted description/notes
--
-- ACTIVE STATUS:
-- - is_active=1: Recurring transaction is active, will be processed
-- - is_active=0: Paused or ended, skipped by scheduled job
-- Users can pause/resume by toggling is_active
-- System automatically sets is_active=0 when end_date is reached
--
-- END DATE BEHAVIOR:
-- - end_date=NULL: Recurring transaction continues indefinitely
-- - end_date set: Last transaction created on or before end_date
-- Example: Monthly rent ends on 2026-12-31
--   - Dec 2026 transaction created
--   - Jan 2027 would be next_occurrence > end_date, so is_active set to 0
--
-- ENCRYPTION:
-- - description and notes fields are encrypted for privacy
-- - Encryption/decryption handled in application layer
--
-- TRANSACTION TYPE BEHAVIOR:
-- - INCOME: Creates INCOME transaction in account_id
-- - EXPENSE: Creates EXPENSE transaction in account_id
-- - TRANSFER: Creates TRANSFER transaction from account_id to to_account_id
--
-- EXAMPLE USE CASES:
-- - Monthly rent: amount=2000, frequency=MONTHLY, type=EXPENSE, category="Housing"
-- - Biweekly salary: amount=3500, frequency=BIWEEKLY, type=INCOME, category="Salary"
-- - Annual insurance: amount=1200, frequency=YEARLY, type=EXPENSE, category="Insurance"
-- - Weekly allowance: amount=100, frequency=WEEKLY, type=TRANSFER, account_id=checking, to_account_id=savings
--
-- SCHEDULED JOB CONFIGURATION:
-- - Frequency: Daily (runs at midnight)
-- - Query optimization: Use idx_recurring_active_next_occurrence index
-- - Batch size: Process up to 1000 due recurring transactions per run
-- - Error handling: Log failures, continue processing remaining transactions
-- - Notification: Optionally notify users of auto-generated transactions
