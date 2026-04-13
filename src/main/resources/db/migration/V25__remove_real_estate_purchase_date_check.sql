-- Migration: Remove non-deterministic purchase_date CHECK constraint
-- Author: Open Finance Development Team
-- Date: 2026-02-04
-- Version: V25
-- Description: SQLite does not allow non-deterministic functions like DATE('now')
--              inside CHECK constraints. Recreate the table without the
--              purchase_date <= DATE('now') check.

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

    CONSTRAINT fk_real_estate_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_real_estate_mortgage
        FOREIGN KEY (mortgage_id)
        REFERENCES liabilities(id)
        ON DELETE SET NULL,

    CONSTRAINT chk_real_estate_currency_length
        CHECK (LENGTH(currency) = 3),

    CONSTRAINT chk_real_estate_property_type_valid
        CHECK (property_type IN ('RESIDENTIAL', 'COMMERCIAL', 'LAND', 'MIXED_USE', 'INDUSTRIAL', 'OTHER')),

    CONSTRAINT chk_real_estate_latitude_range
        CHECK (latitude IS NULL OR (latitude >= -90 AND latitude <= 90)),

    CONSTRAINT chk_real_estate_longitude_range
        CHECK (longitude IS NULL OR (longitude >= -180 AND longitude <= 180))
);

INSERT INTO real_estate_properties_new (
    id,
    user_id,
    name,
    address,
    property_type,
    purchase_price,
    purchase_date,
    current_value,
    currency,
    mortgage_id,
    rental_income,
    notes,
    documents,
    latitude,
    longitude,
    is_active,
    created_at,
    updated_at
)
SELECT
    id,
    user_id,
    name,
    address,
    property_type,
    purchase_price,
    purchase_date,
    current_value,
    currency,
    mortgage_id,
    rental_income,
    notes,
    documents,
    latitude,
    longitude,
    is_active,
    created_at,
    updated_at
FROM real_estate_properties;

DROP TABLE real_estate_properties;
ALTER TABLE real_estate_properties_new RENAME TO real_estate_properties;

CREATE INDEX idx_real_estate_user_id ON real_estate_properties(user_id);
CREATE INDEX idx_real_estate_property_type ON real_estate_properties(property_type);
CREATE INDEX idx_real_estate_user_type ON real_estate_properties(user_id, property_type);
CREATE INDEX idx_real_estate_mortgage_id ON real_estate_properties(mortgage_id);
CREATE INDEX idx_real_estate_purchase_date ON real_estate_properties(purchase_date);
CREATE INDEX idx_real_estate_user_active ON real_estate_properties(user_id, is_active);
CREATE INDEX idx_real_estate_location ON real_estate_properties(latitude, longitude);
