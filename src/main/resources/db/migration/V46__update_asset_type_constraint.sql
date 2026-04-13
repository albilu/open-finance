-- Migration: Update assets table check constraint for physical assets
-- Description: Adds VEHICLE, JEWELRY, COLLECTIBLE, ELECTRONICS, FURNITURE to chk_asset_type_valid

PRAGMA foreign_keys=off;

-- 1. Create a new table with the updated check constraint
CREATE TABLE new_assets (
    id                  INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id             INTEGER NOT NULL,
    account_id          INTEGER,                          
    name                VARCHAR(500) NOT NULL,            
    asset_type          VARCHAR(20) NOT NULL,             
    symbol              VARCHAR(20),                      
    quantity            DECIMAL(19, 8) NOT NULL,          
    purchase_price      DECIMAL(19, 4) NOT NULL,          
    current_price       DECIMAL(19, 4) NOT NULL,          
    currency            VARCHAR(3) NOT NULL,              
    purchase_date       DATE NOT NULL,                    
    notes               TEXT,                             
    last_updated        TIMESTAMP,                        
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP,
    
    -- Added in V19
    serial_number       VARCHAR(500),
    brand               VARCHAR(500),
    model               VARCHAR(500),
    condition           VARCHAR(20),
    warranty_expiration DATE,
    useful_life_years   INTEGER,
    photo_path          VARCHAR(500),
    
    -- Foreign key constraints
    CONSTRAINT fk_assets_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,
    
    CONSTRAINT fk_assets_account
        FOREIGN KEY (account_id)
        REFERENCES accounts(id)
        ON DELETE SET NULL,                               
    
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
        CHECK (asset_type IN ('STOCK', 'ETF', 'MUTUAL_FUND', 'BOND', 'CRYPTO', 'COMMODITY', 'REAL_ESTATE', 'VEHICLE', 'JEWELRY', 'COLLECTIBLE', 'ELECTRONICS', 'FURNITURE', 'OTHER'))
);

-- 2. Copy data from the old table to the new table
INSERT INTO new_assets (
    id, user_id, account_id, name, asset_type, symbol, quantity, 
    purchase_price, current_price, currency, purchase_date, notes, 
    last_updated, created_at, updated_at, serial_number, brand, 
    model, condition, warranty_expiration, useful_life_years, photo_path
)
SELECT 
    id, user_id, account_id, name, asset_type, symbol, quantity, 
    purchase_price, current_price, currency, purchase_date, notes, 
    last_updated, created_at, updated_at, serial_number, brand, 
    model, condition, warranty_expiration, useful_life_years, photo_path
FROM assets;

-- 3. Drop the old table
DROP TABLE assets;

-- 4. Rename the new table to the original name
ALTER TABLE new_assets RENAME TO assets;

-- 5. Recreate indexes
CREATE INDEX idx_asset_user_id ON assets(user_id);
CREATE INDEX idx_asset_account_id ON assets(account_id);
CREATE INDEX idx_asset_type ON assets(asset_type);
CREATE INDEX idx_asset_symbol ON assets(symbol);
CREATE INDEX idx_asset_user_account ON assets(user_id, account_id);
CREATE INDEX idx_asset_user_type ON assets(user_id, asset_type);
CREATE INDEX idx_asset_purchase_date ON assets(purchase_date);
CREATE INDEX idx_asset_condition ON assets(condition);
CREATE INDEX idx_asset_warranty_expiration ON assets(warranty_expiration);

PRAGMA foreign_key_check;
PRAGMA foreign_keys=on;
