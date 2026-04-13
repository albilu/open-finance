-- V19__add_physical_asset_fields.sql
-- Migration to add physical asset tracking fields to the assets table
-- Requirement REQ-2.6: Asset Management - Physical Asset Tracking

-- Add physical asset identification and details
ALTER TABLE assets ADD COLUMN serial_number VARCHAR(500);
ALTER TABLE assets ADD COLUMN brand VARCHAR(500);
ALTER TABLE assets ADD COLUMN model VARCHAR(500);
ALTER TABLE assets ADD COLUMN condition VARCHAR(20);
ALTER TABLE assets ADD COLUMN warranty_expiration DATE;
ALTER TABLE assets ADD COLUMN useful_life_years INTEGER;
ALTER TABLE assets ADD COLUMN photo_path VARCHAR(500);

-- Add comment explaining encrypted fields
-- Note: serial_number, brand, and model will be encrypted by the application layer
-- condition is stored as plaintext enum value for filtering and reporting

-- Create index for condition filtering (useful for reports and analytics)
CREATE INDEX idx_asset_condition ON assets(condition);

-- Create index for warranty tracking
CREATE INDEX idx_asset_warranty_expiration ON assets(warranty_expiration);
