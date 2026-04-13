-- Migration: Create institutions table and add institution_id to accounts
-- Requirement: REQ-2.6.1.3 - Predefined Financial Institutions
-- Date: 2026-02-17

-- Create institutions table
CREATE TABLE IF NOT EXISTS institutions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name VARCHAR(200) NOT NULL,
    bic VARCHAR(11),
    country CHAR(2),
    logo TEXT,
    is_system BOOLEAN NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_institution_country ON institutions(country);
CREATE INDEX IF NOT EXISTS idx_institution_is_system ON institutions(is_system);
CREATE INDEX IF NOT EXISTS idx_institution_name ON institutions(name);

-- Add institution_id to accounts table (optional foreign key)
ALTER TABLE accounts ADD COLUMN institution_id INTEGER;

-- Create foreign key constraint (optional)
-- SQLite doesn't enforce foreign keys by default, but we add the reference for documentation
-- Note: This is informational; actual FK enforcement requires PRAGMA foreign_keys=ON

-- Create index for faster lookups
CREATE INDEX IF NOT EXISTS idx_account_institution ON accounts(institution_id);
