-- Migration V15: Create budgets table
-- Requirement REQ-2.9.1.1: Budget creation and management
-- Requirement REQ-2.9.1.2: Budget tracking by period and category
--
-- This migration creates the budgets table for tracking user spending budgets
-- across different time periods (weekly, monthly, quarterly, yearly).

CREATE TABLE budgets (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    category_id INTEGER NOT NULL,
    amount VARCHAR(512) NOT NULL,  -- Encrypted amount using user's master password
    period VARCHAR(20) NOT NULL,   -- WEEKLY, MONTHLY, QUARTERLY, YEARLY
    start_date DATE NOT NULL,      -- Budget period start date
    end_date DATE NOT NULL,        -- Budget period end date
    rollover BOOLEAN NOT NULL DEFAULT FALSE,  -- Carry unused budget to next period
    notes VARCHAR(500),            -- Optional user notes about the budget
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign key constraints with cascade delete
    CONSTRAINT fk_budget_user FOREIGN KEY (user_id) 
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_budget_category FOREIGN KEY (category_id) 
        REFERENCES categories(id) ON DELETE CASCADE,
    
    -- Check constraint: end date must be >= start date
    CONSTRAINT chk_budget_dates CHECK (end_date >= start_date),
    
    -- Check constraint: period must be valid enum value
    CONSTRAINT chk_budget_period CHECK (period IN ('WEEKLY', 'MONTHLY', 'QUARTERLY', 'YEARLY'))
);

-- Index on user_id for fast user-scoped queries
CREATE INDEX idx_budget_user_id ON budgets(user_id);

-- Index on category_id for category-based lookups
CREATE INDEX idx_budget_category_id ON budgets(category_id);

-- Index on period for filtering by period type
CREATE INDEX idx_budget_period ON budgets(period);

-- Composite index on start_date and end_date for date range queries
CREATE INDEX idx_budget_dates ON budgets(start_date, end_date);

-- Composite index on user_id and category_id for user+category queries
CREATE INDEX idx_budget_user_category ON budgets(user_id, category_id);

-- Composite index on user_id and period for user+period queries
CREATE INDEX idx_budget_user_period ON budgets(user_id, period);
