-- V65: Track real estate property value changes over time.
-- When a property's currentValue is edited, a row is inserted here so
-- historical net worth snapshots can use the value that was correct at
-- any given point in the past rather than the current (most recent) value.

CREATE TABLE IF NOT EXISTS real_estate_value_history (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    property_id    INTEGER NOT NULL REFERENCES real_estate_properties(id) ON DELETE CASCADE,
    user_id        INTEGER NOT NULL REFERENCES users(id),
    effective_date DATE    NOT NULL,
    recorded_value TEXT    NOT NULL,   -- encrypted, same AES-256-GCM format as current_value
    currency       TEXT    NOT NULL,
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_re_value_history_property_date
    ON real_estate_value_history(property_id, effective_date);
