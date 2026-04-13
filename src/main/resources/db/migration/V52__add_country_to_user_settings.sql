-- V52: Add country preference to user_settings
-- Supports country-based tool configuration (e.g. Buy vs Rent defaults, Property Rental Simulator availability)
-- ISO 3166-1 alpha-2 country code; defaults to 'FR' to preserve existing behaviour

ALTER TABLE user_settings ADD COLUMN country VARCHAR(2) NOT NULL DEFAULT 'FR';
