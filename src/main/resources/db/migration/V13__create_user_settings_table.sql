-- Migration: Create user_settings table for storing user display and locale preferences
-- Author: Open-Finance Development Team
-- Date: 2026-02-02
-- Requirement: REQ-6.3 (User Settings & Preferences)

-- Create user_settings table
CREATE TABLE user_settings (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL UNIQUE,
    theme VARCHAR(10) NOT NULL DEFAULT 'dark',
    date_format VARCHAR(20) NOT NULL DEFAULT 'MM/DD/YYYY',
    number_format VARCHAR(20) NOT NULL DEFAULT '1,234.56',
    language VARCHAR(10) NOT NULL DEFAULT 'en',
    timezone VARCHAR(50) NOT NULL DEFAULT 'UTC',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    
    -- Foreign key constraint with CASCADE delete
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    
    -- Check constraints for valid values
    CHECK (theme IN ('dark', 'light')),
    CHECK (date_format IN ('MM/DD/YYYY', 'DD/MM/YYYY', 'YYYY-MM-DD')),
    CHECK (number_format IN ('1,234.56', '1.234,56', '1 234,56')),
    CHECK (language IN ('en', 'fr', 'es', 'de', 'it', 'pt', 'ja', 'zh', 'ko', 'ar', 'ru')),
    CHECK (LENGTH(timezone) >= 3 AND LENGTH(timezone) <= 50)
);

-- Create index on user_id for fast lookups
CREATE INDEX idx_user_settings_user_id ON user_settings(user_id);

-- Insert default settings for all existing users
-- This ensures backward compatibility - existing users get default settings
INSERT INTO user_settings (user_id, theme, date_format, number_format, language, timezone, created_at, updated_at)
SELECT 
    id,
    'dark',
    'MM/DD/YYYY',
    '1,234.56',
    'en',
    'UTC',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM users
WHERE NOT EXISTS (
    SELECT 1 FROM user_settings WHERE user_settings.user_id = users.id
);
