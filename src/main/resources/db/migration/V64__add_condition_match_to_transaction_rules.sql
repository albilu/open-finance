-- V64: Add condition_match column to transaction_rules table
-- Allows rules to use OR logic (any condition matches) in addition to AND (all must match).
-- Defaults to 'AND' to preserve existing rule behavior.
ALTER TABLE transaction_rules ADD COLUMN condition_match VARCHAR(3) NOT NULL DEFAULT 'AND' CHECK (condition_match IN ('AND', 'OR'));
