-- V49: Add external_reference column to transactions table
--
-- Stores the original financial-institution transaction ID (e.g. OFX FITID,
-- QIF check/reference number) that was present in the imported file.
-- Used by ImportService to perform fast, authoritative duplicate detection:
-- if a new import contains a transaction whose referenceNumber already exists
-- in this column for the same account, it is flagged as a definite duplicate
-- without needing any fuzzy matching.
--
-- The column is intentionally nullable because:
--   a) manually-entered transactions never carry an external reference, and
--   b) legacy transactions imported before this migration have no such value.
--
-- An index is added to make the lookup in ImportService O(log n) instead of
-- a full table scan.

ALTER TABLE transactions
    ADD COLUMN external_reference VARCHAR(255);

CREATE INDEX idx_transaction_external_reference
    ON transactions (account_id, external_reference)
    WHERE external_reference IS NOT NULL;
