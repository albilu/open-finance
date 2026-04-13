-- Requirement REQ-2.2: Add interest rate tracking to accounts table
-- Adds a flag for interest and the period of calculation
ALTER TABLE accounts ADD COLUMN is_interest_enabled BOOLEAN DEFAULT 0 NOT NULL;
ALTER TABLE accounts ADD COLUMN interest_period VARCHAR(20) DEFAULT NULL;

-- Create table to store historical interest rates
CREATE TABLE interest_rate_variations (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    account_id INTEGER NOT NULL,
    rate DECIMAL(10, 4) NOT NULL,
    tax_rate DECIMAL(10, 4) DEFAULT 0.0000,
    valid_from DATE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    
    FOREIGN KEY (account_id) REFERENCES accounts (id) ON DELETE CASCADE
);

-- Index on account_id for faster lookups of account variations
CREATE INDEX idx_interest_variation_account ON interest_rate_variations(account_id);
