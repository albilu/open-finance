-- Migration: Add category_id to payees table correctly
-- Date: 2026-02-18

-- Add category_id column if it doesn't exist
-- SQLite allows adding columns with ALTER TABLE
ALTER TABLE payees ADD COLUMN category_id INTEGER REFERENCES categories(id);

-- Create index for the new column
CREATE INDEX IF NOT EXISTS idx_payee_category_id ON payees(category_id);

-- Optional: If we want to clean up the old 'category' column (v32), 
-- SQLite (unless very recent) doesn't support DROP COLUMN.
-- We'll leave it for now to avoid table recreation complexity.
