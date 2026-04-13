-- Migration: Backfill FTS table with existing transaction data
-- Version: V60
-- Description: Populates transactions_fts with payee and tags data from existing
--              non-deleted transactions. This fixes BUG-02 where the FTS virtual
--              table was empty (0 rows), causing all keyword searches to return no
--              results. Only payee and tags are unencrypted and can be backfilled
--              via SQL. Encrypted fields (description, notes) are left empty and
--              will be indexed at the application layer on the next update.
-- Author: Open-Finance
-- Date: 2026-04-01

-- Insert FTS rows for all existing non-deleted transactions that are not yet
-- present in the FTS index. Uses NOT EXISTS to avoid duplicates if this
-- migration runs on a database that already has partial FTS data.
INSERT INTO transactions_fts(transaction_id, user_id, description, notes, tags, payee)
SELECT
    t.id,
    t.user_id,
    '',                              -- description is encrypted; cannot backfill via SQL
    '',                              -- notes is encrypted; cannot backfill via SQL
    COALESCE(t.tags, ''),           -- tags are stored as plain text
    COALESCE(t.payee, '')           -- payee is stored as plain text
FROM transactions t
WHERE t.is_deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM transactions_fts fts WHERE fts.transaction_id = t.id
  );
