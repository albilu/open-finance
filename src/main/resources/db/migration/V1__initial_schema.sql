-- Open-Finance Initial Database Schema
-- Migration Version: V1
-- Requirements: REQ-1.3, REQ-2.18
--
-- This is the initial schema migration placeholder.
-- Actual table definitions will be added as entities are created in future sprints.
--
-- Migration Strategy:
--   - Development: Hibernate ddl-auto=update generates tables automatically
--   - Production: Flyway migrations control schema changes (ddl-auto=validate)
--
-- Future migrations will include:
--   - V2__create_user_table.sql (Sprint 1)
--   - V3__create_account_table.sql (Sprint 2)
--   - V4__create_category_table.sql (Sprint 3)
--   - V5__create_transaction_table.sql (Sprint 3)
--   - V6__create_net_worth_table.sql (Sprint 4)
--   - V7__create_asset_table.sql (Sprint 5)
--   - And so on...

-- For now, this migration creates a schema_info table to track database version
CREATE TABLE IF NOT EXISTS schema_info (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    version VARCHAR(50) NOT NULL,
    description VARCHAR(255),
    installed_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    execution_time INTEGER,
    success BOOLEAN DEFAULT TRUE
);

-- Insert initial version
INSERT INTO schema_info (version, description, execution_time, success)
VALUES ('V1', 'Initial schema placeholder', 0, TRUE);

-- Create a system settings table for application configuration
CREATE TABLE IF NOT EXISTS system_settings (
    setting_key VARCHAR(100) PRIMARY KEY,
    setting_value TEXT,
    description VARCHAR(255),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Insert initial system settings
INSERT INTO system_settings (setting_key, setting_value, description)
VALUES
    ('app_version', '0.1.0', 'Application version'),
    ('schema_version', 'V1', 'Current database schema version'),
    ('initialized_at', CURRENT_TIMESTAMP, 'Database initialization timestamp');
