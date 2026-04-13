-- Migration: V6__add_transfer_id_to_transactions.sql
-- Description: Add transfer_id column to transactions table for linking transfer transactions
-- Author: Open-Finance Development Team
-- Date: 2024-01-31

-- Add transfer_id column to transactions table
-- This column is used to link two transactions that represent a transfer between accounts
-- Both the source (EXPENSE) and destination (INCOME) transactions share the same transferId
ALTER TABLE transactions ADD COLUMN transfer_id VARCHAR(36);

-- Add index on transfer_id for faster lookups when finding linked transfer transactions
CREATE INDEX idx_transaction_transfer_id ON transactions(transfer_id);

-- Comment on the column
-- SQLite doesn't support column comments directly, but we document here:
-- transfer_id: UUID linking source and destination transactions in a transfer operation
-- Format: UUID v4 (36 characters including hyphens)
-- Example: 'a1b2c3d4-e5f6-4789-a012-b3c4d5e6f789'
-- NULL for non-transfer transactions (INCOME/EXPENSE without toAccountId)
