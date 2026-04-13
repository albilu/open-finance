-- Migration: Create assets table
-- Author: Open Finance Development Team
-- Date: 2026-02-01
-- Version: V8
-- Description: Creates the assets table for tracking financial assets (stocks, ETFs, crypto, etc.)
--              Requirement REQ-2.6: Asset Management

-- Create assets table
CREATE TABLE assets (
    id                  INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id             INTEGER NOT NULL,
    account_id          INTEGER,                          -- Optional: link to brokerage account
    name                VARCHAR(500) NOT NULL,            -- Encrypted field (extra length for encryption overhead)
    asset_type          VARCHAR(20) NOT NULL,             -- STOCK, ETF, MUTUAL_FUND, BOND, CRYPTO, COMMODITY, REAL_ESTATE, OTHER
    symbol              VARCHAR(20),                      -- Ticker symbol (e.g., AAPL, BTC-USD)
    quantity            DECIMAL(19, 8) NOT NULL,          -- Number of units (8 decimals for crypto fractional amounts)
    purchase_price      DECIMAL(19, 4) NOT NULL,          -- Price per unit when purchased
    current_price       DECIMAL(19, 4) NOT NULL,          -- Current market price per unit
    currency            VARCHAR(3) NOT NULL,              -- ISO 4217 currency code (USD, EUR, etc.)
    purchase_date       DATE NOT NULL,                    -- Date asset was purchased
    notes               TEXT,                             -- Encrypted field: Optional notes about the asset
    last_updated        TIMESTAMP,                        -- When current_price was last updated
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP,
    
    -- Foreign key constraints
    CONSTRAINT fk_assets_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,
    
    CONSTRAINT fk_assets_account
        FOREIGN KEY (account_id)
        REFERENCES accounts(id)
        ON DELETE SET NULL,                               -- If account deleted, keep asset but clear link
    
    -- Check constraints
    CONSTRAINT chk_asset_quantity_positive
        CHECK (quantity > 0),
    
    CONSTRAINT chk_asset_purchase_price_non_negative
        CHECK (purchase_price >= 0),
    
    CONSTRAINT chk_asset_current_price_non_negative
        CHECK (current_price >= 0),
    
    CONSTRAINT chk_asset_currency_length
        CHECK (LENGTH(currency) = 3),
    
    CONSTRAINT chk_asset_type_valid
        CHECK (asset_type IN ('STOCK', 'ETF', 'MUTUAL_FUND', 'BOND', 'CRYPTO', 'COMMODITY', 'REAL_ESTATE', 'OTHER'))
);

-- Create indexes for performance
-- Index on user_id for fast user-specific asset queries
CREATE INDEX idx_asset_user_id ON assets(user_id);

-- Index on account_id for filtering assets by account
CREATE INDEX idx_asset_account_id ON assets(account_id);

-- Index on asset_type for filtering by asset class
CREATE INDEX idx_asset_type ON assets(asset_type);

-- Index on symbol for quick lookups and market data updates
CREATE INDEX idx_asset_symbol ON assets(symbol);

-- Composite index for user + account queries (common pattern)
CREATE INDEX idx_asset_user_account ON assets(user_id, account_id);

-- Composite index for user + type queries (portfolio composition)
CREATE INDEX idx_asset_user_type ON assets(user_id, asset_type);

-- Index on purchase_date for date range queries and reporting
CREATE INDEX idx_asset_purchase_date ON assets(purchase_date);

-- Comments for documentation
-- Note: SQLite doesn't support column comments directly, but this serves as documentation

-- Migration validation queries (commented out - for reference only)
-- SELECT COUNT(*) FROM assets;  -- Should be 0 after migration
-- SELECT * FROM sqlite_master WHERE type='table' AND name='assets';  -- Verify table creation
-- SELECT * FROM sqlite_master WHERE type='index' AND tbl_name='assets';  -- Verify indexes
