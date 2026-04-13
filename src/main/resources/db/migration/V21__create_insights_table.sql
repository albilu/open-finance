-- Migration V21: Create insights table for AI-generated financial insights
-- Sprint 11 - AI Assistant Integration (Task 11.4)
--
-- This table stores AI-generated insights about user's financial behavior including:
-- - Spending anomalies and unusual patterns
-- - Budget warnings and recommendations
-- - Savings opportunities
-- - General financial tips and recommendations
--
-- Each insight has a type, priority level, and can be dismissed by the user.

CREATE TABLE IF NOT EXISTS insights (
    id                 INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id            INTEGER NOT NULL,
    type               VARCHAR(50) NOT NULL,  -- InsightType enum: SPENDING_ANOMALY, BUDGET_WARNING, etc.
    title              VARCHAR(200) NOT NULL, -- Brief summary title
    description        TEXT NOT NULL,         -- Detailed description with context
    priority           VARCHAR(20) NOT NULL,  -- InsightPriority enum: HIGH, MEDIUM, LOW
    dismissed          INTEGER NOT NULL DEFAULT 0, -- Boolean: 0 = active, 1 = dismissed
    created_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign key constraints
    CONSTRAINT fk_insight_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    
    -- Check constraints for enum values
    CONSTRAINT chk_insight_type CHECK (type IN (
        'SPENDING_ANOMALY',
        'BUDGET_WARNING',
        'BUDGET_RECOMMENDATION',
        'SAVINGS_OPPORTUNITY',
        'INVESTMENT_SUGGESTION',
        'DEBT_ALERT',
        'CASH_FLOW_WARNING',
        'TAX_OPTIMIZATION',
        'GOAL_PROGRESS',
        'GENERAL_TIP'
    )),
    CONSTRAINT chk_insight_priority CHECK (priority IN ('HIGH', 'MEDIUM', 'LOW')),
    CONSTRAINT chk_insight_dismissed CHECK (dismissed IN (0, 1))
);

-- Indexes for efficient querying
CREATE INDEX IF NOT EXISTS idx_insight_user_id ON insights(user_id);
CREATE INDEX IF NOT EXISTS idx_insight_created_at ON insights(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_insight_priority ON insights(priority);
CREATE INDEX IF NOT EXISTS idx_insight_dismissed ON insights(dismissed);

-- Composite index for common query: active insights for user ordered by priority and date
CREATE INDEX IF NOT EXISTS idx_insight_user_active ON insights(user_id, dismissed, priority, created_at DESC);

-- Comments
-- Table: insights
-- Purpose: Store AI-generated financial insights and recommendations for users
-- Related tables: users (via user_id foreign key)
-- Lifecycle: Insights can be dismissed but are retained for history
