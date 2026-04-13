-- Migration: Add insurance percentage and additional fees to liabilities
-- Author: Open Finance Development Team
-- Date: 2026-03-03
-- Version: V40
-- Description: Adds insurance_percentage and additional_fees encrypted fields to the liabilities table
--              to support comprehensive liability cost tracking.
--              Requirement REQ-LIA-1: Insurance Percentage Field
--              Requirement REQ-LIA-2: Additional Fees Field

-- Add insurance_percentage column (encrypted BigDecimal as String, optional)
ALTER TABLE liabilities ADD COLUMN insurance_percentage VARCHAR(512);

-- Add additional_fees column (encrypted BigDecimal as String, optional)
ALTER TABLE liabilities ADD COLUMN additional_fees VARCHAR(512);

-- Comments for documentation:
-- insurance_percentage: Annual insurance rate as a percentage of the principal amount (e.g., 0.5 for 0.5%)
--   Monthly insurance cost = principal × (insurance_percentage / 100) / 12
-- additional_fees: One-time or periodic fees associated with this liability
--   (e.g., processing fees, origination fees, late payment fees already incurred)
