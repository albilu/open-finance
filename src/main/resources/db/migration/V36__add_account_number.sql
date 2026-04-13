-- Add account_number column to accounts table for transaction import matching
-- This field stores the official account number (e.g., checking account number, IBAN)
-- to automatically match imported transactions to the correct account

ALTER TABLE accounts ADD COLUMN account_number VARCHAR(50);

-- Index on account_number for faster lookups when matching imported transactions
CREATE INDEX idx_account_number ON accounts(account_number);

-- Composite index for finding accounts by user and account number
CREATE INDEX idx_account_user_number ON accounts(user_id, account_number);
