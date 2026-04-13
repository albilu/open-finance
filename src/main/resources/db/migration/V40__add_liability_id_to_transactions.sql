-- V41: Add liability_id to transactions table
-- Requirement REQ-LIA-4: Allow transactions to be linked to a liability for loan payment tracking

ALTER TABLE transactions ADD COLUMN liability_id INTEGER REFERENCES liabilities(id) ON DELETE SET NULL;
CREATE INDEX idx_transaction_liability_id ON transactions(liability_id);
