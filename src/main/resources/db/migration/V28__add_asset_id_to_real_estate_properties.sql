/*
 * Migration V29: Add asset_id to real_estate_properties
 *
 * This migration adds a foreign key column to link Real Estate properties
 * to the generic Assets table, enabling unified portfolio tracking.
 *
 * rewrite for SQLite compatibility (no ALTER TABLE ADD CONSTRAINT)
 */
-- 1. Create the new table with the additional asset_id column and foreign key constraint
CREATE TABLE real_estate_properties_new (
    id                  INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id             INTEGER NOT NULL,
    name                VARCHAR(500) NOT NULL,
    address             VARCHAR(1000) NOT NULL,
    property_type       VARCHAR(20) NOT NULL,
    purchase_price      VARCHAR(500) NOT NULL,
    purchase_date       DATE NOT NULL,
    current_value       VARCHAR(500) NOT NULL,
    currency            VARCHAR(3) NOT NULL,
    mortgage_id         INTEGER,
    rental_income       VARCHAR(500),
    notes               TEXT,
    documents           TEXT,
    latitude            DECIMAL(10, 7),
    longitude           DECIMAL(10, 7),
    is_active           BOOLEAN NOT NULL DEFAULT 1,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    asset_id            BIGINT,
    -- Foreign key constraints
    CONSTRAINT fk_real_estate_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_real_estate_mortgage
        FOREIGN KEY (mortgage_id)
        REFERENCES liabilities(id)
        ON DELETE SET NULL,
    CONSTRAINT fk_real_estate_asset
        FOREIGN KEY (asset_id)
        REFERENCES assets(id)
        ON DELETE SET NULL,
    -- Check constraints
    CONSTRAINT chk_real_estate_currency_length
        CHECK (LENGTH(currency) = 3),
    CONSTRAINT chk_real_estate_property_type_valid
        CHECK (property_type IN ('RESIDENTIAL', 'COMMERCIAL', 'LAND', 'MIXED_USE', 'INDUSTRIAL', 'OTHER')),
    CONSTRAINT chk_real_estate_latitude_range
        CHECK (latitude IS NULL OR (latitude >= -90 AND latitude <= 90)),
    CONSTRAINT chk_real_estate_longitude_range
        CHECK (longitude IS NULL OR (longitude >= -180 AND longitude <= 180))
);
-- 2. Copy data from the old table to the new table
INSERT INTO real_estate_properties_new (
    id, user_id, name, address, property_type, purchase_price, purchase_date,
    current_value, currency, mortgage_id, rental_income, notes, documents,
    latitude, longitude, is_active, created_at, updated_at
)
SELECT
    id, user_id, name, address, property_type, purchase_price, purchase_date,
    current_value, currency, mortgage_id, rental_income, notes, documents,
    latitude, longitude, is_active, created_at, updated_at
FROM real_estate_properties;
-- 3. Drop the old table
DROP TABLE real_estate_properties;
-- 4. Rename the new table to the original name
ALTER TABLE real_estate_properties_new RENAME TO real_estate_properties;
-- 5. Recreate indexes
CREATE INDEX idx_real_estate_user_id ON real_estate_properties(user_id);
CREATE INDEX idx_real_estate_property_type ON real_estate_properties(property_type);
CREATE INDEX idx_real_estate_user_type ON real_estate_properties(user_id, property_type);
CREATE INDEX idx_real_estate_mortgage_id ON real_estate_properties(mortgage_id);
CREATE INDEX idx_real_estate_purchase_date ON real_estate_properties(purchase_date);
CREATE INDEX idx_real_estate_user_active ON real_estate_properties(user_id, is_active);
CREATE INDEX idx_real_estate_location ON real_estate_properties(latitude, longitude);
CREATE INDEX idx_real_estate_asset_id ON real_estate_properties(asset_id);
