-- Migration: Add additional composite indexes for common query patterns
-- Version: V55
-- Description: Additional performance indexes identified during query profiling:
--              - net_worth snapshots (user + date lookups)
--              - assets (user + type lookups)
--              - assets (user + currency lookups)
--              - exchange_rates (currency pair + date lookups)
--              - insights (user + dismissed status)
--              - ai_conversations (user + created_at for history pagination)
-- Author: Open-Finance
-- Date: 2026-03-20
-- Requirement REQ-3.1: Performance – avoid full-table scans on hot query paths

-- net_worth table: user + snapshot_date (used for historical trend queries)
-- Note: V7 already has a UNIQUE index idx_net_worth_user_date on (user_id, snapshot_date).
--       This additional index adds DESC ordering hint for the common "latest snapshots first" query.
CREATE INDEX IF NOT EXISTS idx_net_worth_user_date_desc
    ON net_worth(user_id, snapshot_date DESC);

-- assets table: user + currency (used for multi-currency portfolio queries)
-- Note: V8 already has idx_asset_user_type on (user_id, asset_type).
CREATE INDEX IF NOT EXISTS idx_asset_user_currency
    ON assets(user_id, currency);

-- exchange_rates table: base_currency + target_currency + rate_date (most frequent lookup)
-- Note: V11 already has idx_exchange_rate_base_target_date on the same columns.
--       This adds IF NOT EXISTS guard for safety; the planner will use whichever it prefers.
CREATE INDEX IF NOT EXISTS idx_exchange_rate_currencies_date
    ON exchange_rates(base_currency, target_currency, rate_date DESC);

-- insights table: user + dismissed + created_at (used for unread/active insights count)
-- The insights table uses 'dismissed' (INTEGER 0/1), not 'is_read'.
CREATE INDEX IF NOT EXISTS idx_insight_user_dismissed_date
    ON insights(user_id, dismissed, created_at DESC);

-- ai_conversations table: user + created_at (used for pagination of chat history)
-- Note: V20 has separate indexes on user_id and created_at; this composite covers both.
CREATE INDEX IF NOT EXISTS idx_ai_conversation_user_date
    ON ai_conversations(user_id, created_at DESC);

-- transaction_splits table: transaction_id (used for split lookup by parent)
-- Note: V42 already creates idx_transaction_splits_transaction_id; guard with IF NOT EXISTS.
CREATE INDEX IF NOT EXISTS idx_transaction_split_transaction
    ON transaction_splits(transaction_id);
