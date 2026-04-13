-- Migration: Add composite indexes for performance optimization
-- Version: V54
-- Description: Adds missing composite indexes to improve query performance for
--              the most common access patterns identified during profiling.
-- Author: Open-Finance
-- Date: 2026-03-19
-- Requirement REQ-3.1: Performance – avoid full-table scans on hot query paths

-- -----------------------------------------------------------------------
-- transactions table
-- -----------------------------------------------------------------------

-- Composite index covering user + soft-delete + date: the most common
-- query pattern (dashboard recent transactions, date-range reports).
-- V5 has (user_id, transaction_date) but without is_deleted, so the
-- planner cannot satisfy the soft-delete filter from the index alone.
CREATE INDEX IF NOT EXISTS idx_transaction_user_deleted_date
    ON transactions(user_id, is_deleted, transaction_date DESC);

-- Composite index for account-scoped queries that always filter is_deleted.
-- Used by TransactionService when fetching transactions per account.
CREATE INDEX IF NOT EXISTS idx_transaction_account_deleted
    ON transactions(account_id, is_deleted);

-- -----------------------------------------------------------------------
-- recurring_transactions table
-- -----------------------------------------------------------------------

-- Composite index for the scheduling query: active recurring transactions
-- per user ordered by next_occurrence.  Prevents full-table scans in the
-- recurring-transaction processor scheduler.
CREATE INDEX IF NOT EXISTS idx_recurring_user_active_next
    ON recurring_transactions(user_id, is_active, next_occurrence);

-- -----------------------------------------------------------------------
-- budgets table
-- -----------------------------------------------------------------------

-- Composite index for the period-based budget queries used in budget
-- tracking (user + period start/end lookups).
CREATE INDEX IF NOT EXISTS idx_budget_user_period_start
    ON budgets(user_id, period, start_date);
