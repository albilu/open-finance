-- Migration: Create real_estate_properties table
-- Author: Open Finance Development Team
-- Date: 2026-02-02
-- Version: V18
-- Description: Creates the real_estate_properties table for tracking residential, commercial, land, and other real estate assets
--              Requirement REQ-2.16: Real Estate Management

-- Create real_estate_properties table
CREATE TABLE real_estate_properties (
    id                  INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id             INTEGER NOT NULL,
    name                VARCHAR(500) NOT NULL,            -- Encrypted field: property name or description (extra length for encryption overhead)
    address             VARCHAR(1000) NOT NULL,           -- Encrypted field: full property address (extra length for encryption overhead)
    property_type       VARCHAR(20) NOT NULL,             -- RESIDENTIAL, COMMERCIAL, LAND, MIXED_USE, INDUSTRIAL, OTHER
    purchase_price      VARCHAR(500) NOT NULL,            -- Encrypted field: purchase price (encrypted BigDecimal as String)
    purchase_date       DATE NOT NULL,                    -- Date property was purchased
    current_value       VARCHAR(500) NOT NULL,            -- Encrypted field: current estimated value (encrypted BigDecimal as String)
    currency            VARCHAR(3) NOT NULL,              -- ISO 4217 currency code (USD, EUR, etc.)
    mortgage_id         INTEGER,                          -- Optional link to mortgage liability
    rental_income       VARCHAR(500),                     -- Encrypted field: monthly rental income if applicable (nullable, encrypted BigDecimal as String)
    notes               TEXT,                             -- Encrypted field: optional notes about the property
    documents           TEXT,                             -- Encrypted field: JSON array of document metadata (file IDs, names, types)
    latitude            DECIMAL(10, 7),                   -- Geographic latitude (-90 to 90)
    longitude           DECIMAL(10, 7),                   -- Geographic longitude (-180 to 180)
    is_active           BOOLEAN NOT NULL DEFAULT 1,       -- Soft delete flag (1 = active, 0 = deleted)
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign key constraints
    CONSTRAINT fk_real_estate_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,
    
    CONSTRAINT fk_real_estate_mortgage
        FOREIGN KEY (mortgage_id)
        REFERENCES liabilities(id)
        ON DELETE SET NULL,
    
    -- Check constraints
    CONSTRAINT chk_real_estate_currency_length
        CHECK (LENGTH(currency) = 3),
    
    CONSTRAINT chk_real_estate_property_type_valid
        CHECK (property_type IN ('RESIDENTIAL', 'COMMERCIAL', 'LAND', 'MIXED_USE', 'INDUSTRIAL', 'OTHER')),
    
    CONSTRAINT chk_real_estate_purchase_date_not_future
        CHECK (purchase_date <= DATE('now')),
    
    CONSTRAINT chk_real_estate_latitude_range
        CHECK (latitude IS NULL OR (latitude >= -90 AND latitude <= 90)),
    
    CONSTRAINT chk_real_estate_longitude_range
        CHECK (longitude IS NULL OR (longitude >= -180 AND longitude <= 180))
);

-- Create indexes for performance
-- Index on user_id for fast user-specific property queries
CREATE INDEX idx_real_estate_user_id ON real_estate_properties(user_id);

-- Index on property_type for filtering by property category
CREATE INDEX idx_real_estate_property_type ON real_estate_properties(property_type);

-- Composite index for user + type queries (common filtering pattern)
CREATE INDEX idx_real_estate_user_type ON real_estate_properties(user_id, property_type);

-- Index on mortgage_id for finding properties linked to specific mortgages
CREATE INDEX idx_real_estate_mortgage_id ON real_estate_properties(mortgage_id);

-- Index on purchase_date for date range queries and historical analysis
CREATE INDEX idx_real_estate_purchase_date ON real_estate_properties(purchase_date);

-- Composite index for active properties per user (most common query)
CREATE INDEX idx_real_estate_user_active ON real_estate_properties(user_id, is_active);

-- Spatial index for location-based queries (bounding box searches)
CREATE INDEX idx_real_estate_location ON real_estate_properties(latitude, longitude);

-- Comments for documentation
-- Note: SQLite doesn't support column comments directly, but this serves as documentation
-- 
-- Encrypted fields (service-layer encryption with AES-256):
--   - name: User-friendly description of the property (e.g., "Main Residence", "Rental Property #1")
--   - address: Full street address including city, state, zip code, country
--   - purchase_price: Original purchase price (encrypted BigDecimal with 2 decimal places)
--   - current_value: Current estimated market value (encrypted BigDecimal with 2 decimal places)
--   - rental_income: Monthly rental income for investment properties (encrypted BigDecimal with 2 decimal places)
--   - notes: Additional details about the property (condition, renovations, tenant info, etc.)
--   - documents: JSON string with references to attached documents (deeds, contracts, inspection reports)
--
-- Calculation notes:
--   - Appreciation = current_value - purchase_price (both decrypted)
--   - Appreciation % = ((current_value - purchase_price) / purchase_price) * 100
--   - Rental yield % = (rental_income * 12 / current_value) * 100
--   - Equity = current_value - mortgage_balance (if mortgage linked)
--   - ROI % = ((current_value - purchase_price + total_rental_income - mortgage_interest_paid) / purchase_price) * 100
--
-- Location data:
--   - latitude/longitude stored unencrypted for efficient spatial queries
--   - Used for map visualization and location-based filtering
--   - Not considered sensitive when stored separately from address
--
-- Soft delete:
--   - is_active = 1 (true) for current properties included in net worth
--   - is_active = 0 (false) for sold or deleted properties (historical records)
--
-- Property types:
--   - RESIDENTIAL: Houses, apartments, condos, townhouses
--   - COMMERCIAL: Office buildings, retail spaces, warehouses
--   - LAND: Vacant lots, agricultural land, undeveloped property
--   - MIXED_USE: Properties combining residential and commercial use
--   - INDUSTRIAL: Factories, manufacturing plants, distribution centers
--   - OTHER: Unique or specialized property types

-- Migration validation queries (commented out - for reference only)
-- SELECT COUNT(*) FROM real_estate_properties;  -- Should be 0 after migration
-- SELECT * FROM sqlite_master WHERE type='table' AND name='real_estate_properties';  -- Verify table creation
-- SELECT * FROM sqlite_master WHERE type='index' AND tbl_name='real_estate_properties';  -- Verify indexes
