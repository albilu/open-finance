-- Migration: Fix transactions_fts — remove contentless mode so UNINDEXED columns are stored
-- Version: V66
-- Description: The FTS5 virtual table was created with content='' (contentless mode).
--              In contentless mode, UNINDEXED columns (transaction_id, user_id) are NOT
--              stored and always return NULL on SELECT. This breaks the INNER JOIN used
--              by SearchService:
--                  INNER JOIN transactions_fts fts ON t.id = fts.transaction_id
--              because fts.transaction_id is always NULL → zero rows returned → search
--              always returns 0 results.
--              Fix: drop and recreate without content='' (regular FTS5 mode stores all
--              columns including UNINDEXED ones), then backfill payee and tags.
-- Author: Open-Finance
-- Date: 2026-04-05

-- Drop the broken contentless FTS5 table
DROP TABLE IF EXISTS transactions_fts;

-- Recreate as regular (non-contentless) FTS5 — UNINDEXED columns are now stored
CREATE VIRTUAL TABLE transactions_fts USING fts5(
    transaction_id UNINDEXED,
    user_id UNINDEXED,
    description,
    notes,
    tags,
    payee,
    tokenize='unicode61 remove_diacritics 2'
);

-- Backfill with plain-text payee and tags (description/notes are encrypted; those
-- will be populated by the application layer on the next create/update call)
INSERT INTO transactions_fts(transaction_id, user_id, description, notes, tags, payee)
SELECT
    t.id,
    t.user_id,
    '',
    '',
    COALESCE(t.tags, ''),
    COALESCE(t.payee, '')
FROM transactions t
WHERE t.is_deleted = 0;
