-- V33__add_mcc_code_to_categories.sql
-- Add ISO 18245 Merchant Category Code to categories table
-- Requirement: REQ-CAT-1.1.1 - ISO 18245 based categories

ALTER TABLE categories ADD COLUMN mcc_code VARCHAR(10);

-- Create index for faster lookups by MCC code
CREATE INDEX idx_category_mcc_code ON categories(mcc_code);
