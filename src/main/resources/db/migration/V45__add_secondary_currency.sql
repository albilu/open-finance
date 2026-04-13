-- Requirement REQ-2.2: Add optional secondary currency column to users table.
-- This column stores the user's preferred secondary comparison currency (ISO 4217).
-- Nullable: when NULL, secondary currency display is omitted from amount tooltips.
ALTER TABLE users ADD COLUMN secondary_currency VARCHAR(3);
