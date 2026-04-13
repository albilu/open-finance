-- Migration V53: Update insights table CHECK constraint for new InsightType enum values
--
-- The original V21 migration created a CHECK constraint (chk_insight_type) that only
-- allows the original 10 InsightType values. Since then, the Java enum has gained:
--   - UNUSUAL_TRANSACTION (anomaly detection scheduler)
--   - REGION_COMPARISON   (Task 11.7 — regional salary/net-worth comparison)
--   - TAX_OBLIGATION      (Task 11.7 — estimated tax liability insights)
--   - RECURRING_BILLING   (Task 11.7 — subscription/billing analysis)
--
-- SQLite does not support ALTER TABLE ... DROP CONSTRAINT, so we must recreate the table.

-- Step 1: Rename the existing table
ALTER TABLE insights RENAME TO insights_old;

-- Step 2: Create the new table with the updated CHECK constraint
CREATE TABLE insights (
    id                 INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id            INTEGER NOT NULL,
    type               VARCHAR(50) NOT NULL,
    title              VARCHAR(200) NOT NULL,
    description        TEXT NOT NULL,
    priority           VARCHAR(20) NOT NULL,
    dismissed          INTEGER NOT NULL DEFAULT 0,
    created_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_insight_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,

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
        'GENERAL_TIP',
        'UNUSUAL_TRANSACTION',
        'REGION_COMPARISON',
        'TAX_OBLIGATION',
        'RECURRING_BILLING'
    )),
    CONSTRAINT chk_insight_priority CHECK (priority IN ('HIGH', 'MEDIUM', 'LOW')),
    CONSTRAINT chk_insight_dismissed CHECK (dismissed IN (0, 1))
);

-- Step 3: Copy existing data
INSERT INTO insights (id, user_id, type, title, description, priority, dismissed, created_at)
SELECT id, user_id, type, title, description, priority, dismissed, created_at
FROM insights_old;

-- Step 4: Drop the old table
DROP TABLE insights_old;

-- Step 5: Recreate indexes (dropped with the old table)
CREATE INDEX IF NOT EXISTS idx_insight_user_id ON insights(user_id);
CREATE INDEX IF NOT EXISTS idx_insight_created_at ON insights(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_insight_priority ON insights(priority);
CREATE INDEX IF NOT EXISTS idx_insight_dismissed ON insights(dismissed);
CREATE INDEX IF NOT EXISTS idx_insight_user_active ON insights(user_id, dismissed, priority, created_at DESC);
