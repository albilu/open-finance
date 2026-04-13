-- Migration V16: Add currency column to budgets table
-- Requirement REQ-2.8: Multi-currency support for budgets
--
-- This migration adds currency tracking to budgets to support
-- multi-currency budget management.

-- Add currency column with default 'USD' and validation
-- Note: Using one-step ALTER TABLE for SQLite compatibility
ALTER TABLE budgets ADD COLUMN currency VARCHAR(10) NOT NULL DEFAULT 'USD' 
    CHECK (LENGTH(currency) >= 3 AND LENGTH(currency) <= 10 AND currency = UPPER(currency) AND currency NOT GLOB '*[^A-Z]*');

-- Set default currency 'USD' for any existing budgets (safety check, though DEFAULT handles it for new columns)
UPDATE budgets SET currency = 'USD' WHERE currency IS NULL;
