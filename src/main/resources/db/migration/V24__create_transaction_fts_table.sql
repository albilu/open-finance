-- Migration: Create FTS5 virtual table for transaction full-text search
-- Version: V24
-- Description: Creates a virtual table using SQLite FTS5 extension for efficient full-text search
--              on transaction descriptions and notes. This enables fast keyword search across
--              encrypted transaction data after decryption.
-- Author: Open-Finance
-- Date: 2026-02-03
-- Task: TASK-12.4.1
-- Requirement: REQ-2.3.5

-- ==============================================================================
-- FULL-TEXT SEARCH OVERVIEW
-- ==============================================================================
-- SQLite FTS5 (Full-Text Search) provides:
-- - Fast keyword search across large text datasets
-- - Boolean operators (AND, OR, NOT)
-- - Phrase queries ("exact phrase")
-- - Prefix matching (partial word search)
-- - Ranking and relevance scoring
--
-- ARCHITECTURE:
-- 1. transactions table (main data) - stores encrypted description/notes
-- 2. transactions_fts table (virtual FTS5) - stores DECRYPTED text for search
-- 3. Application layer:
--    - INSERT: Decrypt description/notes → insert into FTS table
--    - UPDATE: Decrypt new description/notes → update FTS table
--    - DELETE: Remove from FTS table
--    - SEARCH: Query FTS table → get transaction IDs → fetch full records
--
-- SECURITY NOTE:
-- - FTS table stores DECRYPTED text for search functionality
-- - Database file encryption (SQLCipher) protects FTS data at rest
-- - Application-level authorization ensures users only search their own transactions
-- ==============================================================================

-- Create FTS5 virtual table for transaction full-text search
-- contentless_delete=1: Allows row deletion (required for maintenance)
-- tokenize='unicode61': Unicode-aware tokenization (supports international text)
CREATE VIRTUAL TABLE IF NOT EXISTS transactions_fts USING fts5(
    transaction_id UNINDEXED,          -- Transaction ID (not searchable, used for joining)
    user_id UNINDEXED,                 -- User ID (not searchable, used for filtering)
    description,                        -- Searchable decrypted description
    notes,                              -- Searchable decrypted notes
    tags,                               -- Searchable tags (comma-separated)
    payee,                              -- Searchable payee name
    content='',                         -- Contentless FTS (we manage content manually)
    contentless_delete=1,               -- Allow deletes
    tokenize='unicode61 remove_diacritics 2'  -- Unicode tokenization with diacritic folding
);

-- ==============================================================================
-- INDEXES FOR EFFICIENT FILTERING
-- ==============================================================================
-- FTS5 doesn't support composite indexes, so we rely on:
-- 1. FTS5 internal indexing for full-text search (automatic)
-- 2. Post-filtering in application layer by user_id (authorization)
-- 3. Regular B-tree indexes on transactions table for other filters
--
-- QUERY PATTERN:
-- SELECT t.* 
-- FROM transactions t
-- INNER JOIN transactions_fts fts ON t.id = fts.transaction_id
-- WHERE fts.transactions_fts MATCH 'search query'
--   AND fts.user_id = ?
--   AND t.is_deleted = 0
-- ORDER BY fts.rank;
-- ==============================================================================

-- ==============================================================================
-- USAGE EXAMPLES
-- ==============================================================================
--
-- 1. Simple keyword search:
--    SELECT * FROM transactions_fts WHERE transactions_fts MATCH 'groceries';
--
-- 2. Multiple keywords (AND):
--    SELECT * FROM transactions_fts WHERE transactions_fts MATCH 'groceries walmart';
--
-- 3. Phrase search:
--    SELECT * FROM transactions_fts WHERE transactions_fts MATCH '"monthly subscription"';
--
-- 4. Boolean operators:
--    SELECT * FROM transactions_fts WHERE transactions_fts MATCH 'groceries OR dining';
--    SELECT * FROM transactions_fts WHERE transactions_fts MATCH 'subscription NOT netflix';
--
-- 5. Prefix search (partial word):
--    SELECT * FROM transactions_fts WHERE transactions_fts MATCH 'groc*';
--
-- 6. Column-specific search:
--    SELECT * FROM transactions_fts WHERE transactions_fts MATCH 'description:groceries';
--    SELECT * FROM transactions_fts WHERE transactions_fts MATCH 'notes:important';
--
-- 7. Ranked results (relevance sorting):
--    SELECT *, rank FROM transactions_fts 
--    WHERE transactions_fts MATCH 'groceries'
--    ORDER BY rank;
--
-- 8. Combined with regular table (typical application usage):
--    SELECT t.id, t.amount, t.transaction_date, fts.description
--    FROM transactions t
--    INNER JOIN transactions_fts fts ON t.id = fts.transaction_id
--    WHERE fts.transactions_fts MATCH 'groceries'
--      AND fts.user_id = 1
--      AND t.is_deleted = 0
--    ORDER BY fts.rank
--    LIMIT 20;
-- ==============================================================================

-- ==============================================================================
-- MAINTENANCE NOTES
-- ==============================================================================
-- The FTS5 table is maintained by the application layer:
--
-- INSERT TRANSACTION:
-- 1. Encrypt description/notes → INSERT into transactions table
-- 2. Decrypt description/notes → INSERT into transactions_fts table
--
-- UPDATE TRANSACTION:
-- 1. Encrypt new description/notes → UPDATE transactions table
-- 2. Decrypt new description/notes → UPDATE transactions_fts table
--
-- DELETE TRANSACTION:
-- 1. Set is_deleted=1 in transactions table (soft delete)
-- 2. DELETE FROM transactions_fts WHERE transaction_id = ? (hard delete)
--
-- REBUILD FTS INDEX (if needed):
-- INSERT INTO transactions_fts(transactions_fts) VALUES('rebuild');
--
-- OPTIMIZE FTS INDEX (periodic maintenance):
-- INSERT INTO transactions_fts(transactions_fts) VALUES('optimize');
-- ==============================================================================
