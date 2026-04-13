-- Migration: V17 - Create budget_alerts table
-- Description: Creates table for budget alert configuration and tracking
-- Author: Open-Finance Development Team
-- Date: 2026-02-02
-- Requirements: REQ-2.9.4 (Budget alert system)

-- Create budget_alerts table for SQLite
CREATE TABLE IF NOT EXISTS budget_alerts (
    id VARCHAR(36) PRIMARY KEY,
    budget_id INTEGER NOT NULL,
    threshold DECIMAL(5, 2) NOT NULL CHECK (threshold >= 1.00 AND threshold <= 150.00),
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    last_triggered TIMESTAMP,
    is_read BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign key constraint
    CONSTRAINT fk_alert_budget FOREIGN KEY (budget_id) 
        REFERENCES budgets(id) ON DELETE CASCADE,
    
    -- Unique constraint: one alert per budget per threshold
    CONSTRAINT uk_budget_threshold UNIQUE (budget_id, threshold)
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_alert_budget_id ON budget_alerts(budget_id);
-- Partial indexes are supported in SQLite 3.8.0+
CREATE INDEX IF NOT EXISTS idx_alert_enabled ON budget_alerts(is_enabled) WHERE is_enabled = 1;
CREATE INDEX IF NOT EXISTS idx_alert_unread ON budget_alerts(is_read) WHERE is_read = 0;
CREATE INDEX IF NOT EXISTS idx_alert_last_triggered ON budget_alerts(last_triggered) WHERE last_triggered IS NOT NULL;
