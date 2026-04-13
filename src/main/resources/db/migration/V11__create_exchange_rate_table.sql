-- Migration: Create exchange_rates table
-- Author: Open Finance Development Team
-- Date: 2026-02-01
-- Version: V11
-- Description: Creates the exchange_rates table for storing historical currency exchange rates
--              Requirement REQ-6.2: Multi-Currency Support - Exchange Rate Management

-- Create exchange_rates table
CREATE TABLE exchange_rates (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    base_currency   VARCHAR(10) NOT NULL,             -- Base currency code (e.g., "USD")
    target_currency VARCHAR(10) NOT NULL,             -- Target currency code (e.g., "EUR")
    rate            DECIMAL(18, 8) NOT NULL,          -- Exchange rate with 8 decimal precision (for crypto)
    rate_date       DATE NOT NULL,                    -- Date of the exchange rate
    source          VARCHAR(100) DEFAULT 'system',    -- Source of the rate (API name, 'manual', 'system')
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Unique constraint: one rate per currency pair per date
    CONSTRAINT uk_exchange_rate_currencies_date 
        UNIQUE (base_currency, target_currency, rate_date),
    
    -- Check constraint: rate must be positive
    CONSTRAINT chk_exchange_rate_positive
        CHECK (rate > 0),
    
    -- Check constraint: base currency must be 3 uppercase letters
    CONSTRAINT chk_base_currency_format
        CHECK (LENGTH(base_currency) >= 3 AND LENGTH(base_currency) <= 10 AND base_currency = UPPER(base_currency) 
               AND base_currency NOT GLOB '*[^A-Z]*'),
    
    -- Check constraint: target currency must be 3 uppercase letters
    CONSTRAINT chk_target_currency_format
        CHECK (LENGTH(target_currency) >= 3 AND LENGTH(target_currency) <= 10 AND target_currency = UPPER(target_currency) 
               AND target_currency NOT GLOB '*[^A-Z]*')
);

-- Indexes for fast lookups and common query patterns

-- Index on base currency (for finding all rates from a currency)
CREATE INDEX idx_exchange_rate_base ON exchange_rates(base_currency);

-- Index on target currency (for finding all rates to a currency)
CREATE INDEX idx_exchange_rate_target ON exchange_rates(target_currency);

-- Index on rate date (for date range queries and cleanup)
CREATE INDEX idx_exchange_rate_date ON exchange_rates(rate_date);

-- Composite index for the most common query: currency pair + date lookup
-- This index supports queries like: "Get USD/EUR rate for 2024-01-15"
CREATE INDEX idx_exchange_rate_base_target_date 
    ON exchange_rates(base_currency, target_currency, rate_date);

-- Seed initial exchange rates (as of 2026-02-01)
-- Note: These are example rates for testing. Production should use real-time API data.
-- Major fiat currency pairs (base: USD)
-- NOT needed anymore as rates are live fetched and stored
/* INSERT INTO exchange_rates (base_currency, target_currency, rate, rate_date, source) VALUES
    ('USD', 'EUR', 0.92500000, '2026-02-01', 'seed')
    ('USD', 'GBP', 0.79000000, '2026-02-01', 'seed'),
    ('USD', 'JPY', 148.50000000, '2026-02-01', 'seed'),
    ('USD', 'CHF', 0.88500000, '2026-02-01', 'seed'),
    ('USD', 'CAD', 1.35000000, '2026-02-01', 'seed'),
    ('USD', 'AUD', 1.52000000, '2026-02-01', 'seed'),
    ('USD', 'NZD', 1.65000000, '2026-02-01', 'seed'),
    ('USD', 'CNY', 7.25000000, '2026-02-01', 'seed'),
    ('USD', 'INR', 83.50000000, '2026-02-01', 'seed'),
    ('USD', 'KRW', 1320.00000000, '2026-02-01', 'seed'),
    ('USD', 'SGD', 1.34000000, '2026-02-01', 'seed'),
    ('USD', 'HKD', 7.82000000, '2026-02-01', 'seed'),
    ('USD', 'MXN', 17.20000000, '2026-02-01', 'seed'),
    ('USD', 'BRL', 5.45000000, '2026-02-01', 'seed'),
    ('USD', 'ZAR', 18.75000000, '2026-02-01', 'seed') */;

-- Cryptocurrency rates (base: USD)
/* INSERT INTO exchange_rates (base_currency, target_currency, rate, rate_date, source) VALUES
    ('USD', 'BTC', 0.00001050, '2026-02-01', 'seed'),      -- ~$95,238 per BTC
    ('USD', 'ETH', 0.00030000, '2026-02-01', 'seed'),      -- ~$3,333 per ETH
    ('USD', 'BNB', 0.00165000, '2026-02-01', 'seed'),      -- ~$606 per BNB
    ('USD', 'XRP', 1.43000000, '2026-02-01', 'seed'),      -- ~$0.70 per XRP
    ('USD', 'ADA', 2.22000000, '2026-02-01', 'seed'),      -- ~$0.45 per ADA
    ('USD', 'USDT', 1.00000000, '2026-02-01', 'seed'),     -- Stablecoin pegged to USD
    ('USD', 'USDC', 1.00000000, '2026-02-01', 'seed'); */     -- Stablecoin pegged to USD

-- Cross-currency rates (EUR as base)
/* INSERT INTO exchange_rates (base_currency, target_currency, rate, rate_date, source) VALUES
    ('EUR', 'GBP', 0.85400000, '2026-02-01', 'seed'),
    ('EUR', 'CHF', 0.95700000, '2026-02-01', 'seed'),
    ('EUR', 'JPY', 160.54000000, '2026-02-01', 'seed'); */

-- Comments for documentation
-- Note: SQLite doesn't support column comments directly, but this serves as documentation
-- 
-- Exchange Rate Storage Format:
--   - Rate represents: 1 base_currency = X target_currency
--   - Example: base=USD, target=EUR, rate=0.925 means 1 USD = 0.925 EUR
--   - To convert $100 to EUR: 100 * 0.925 = 92.50 EUR
--   - Inverse rate (EUR to USD): 1 / 0.925 = 1.081 (calculated on-the-fly)
--
-- Precision:
--   - DECIMAL(18, 8) supports 8 decimal places (sufficient for crypto like Bitcoin)
--   - Example: 1 USD = 0.00001050 BTC (precision up to 8 decimals)
--
-- Data Management:
--   - Only store one direction per pair (USD/EUR), calculate inverse as needed
--   - Use composite index (base_currency, target_currency, rate_date) for fast lookups
--   - Query pattern for historical rates: SELECT * WHERE rate_date <= ? ORDER BY rate_date DESC LIMIT 1
--
-- Source field values:
--   - 'seed': Initial seed data from migration
--   - 'manual': Manually entered rate
--   - 'yfinance': Fetched with yfinance
--   - 'system': System-generated or calculated rate
--
-- Rate Update Strategy:
--   - Scheduled job runs daily at 1:00 AM to fetch latest rates
--   - Historical rates preserved for audit and time-based calculations
--   - Cleanup job can delete rates older than retention period (e.g., 2 years)
--
-- Usage Examples:
--   -- Get latest USD to EUR rate:
--   SELECT * FROM exchange_rates 
--   WHERE base_currency='USD' AND target_currency='EUR' 
--   ORDER BY rate_date DESC LIMIT 1;
--
--   -- Get rate for specific date (or closest earlier):
--   SELECT * FROM exchange_rates 
--   WHERE base_currency='USD' AND target_currency='EUR' AND rate_date <= '2024-01-15'
--   ORDER BY rate_date DESC LIMIT 1;
--
--   -- Convert $1000 to EUR using latest rate:
--   SELECT 1000 * rate AS converted_amount FROM exchange_rates
--   WHERE base_currency='USD' AND target_currency='EUR'
--   ORDER BY rate_date DESC LIMIT 1;

-- Migration validation queries (commented out - for reference only)
-- SELECT COUNT(*) FROM exchange_rates;  -- Should be 25 (seed data)
-- SELECT DISTINCT base_currency FROM exchange_rates;  -- Should include USD, EUR
-- SELECT * FROM exchange_rates WHERE rate <= 0;  -- Should be empty (positive check)
-- SELECT base_currency, target_currency, COUNT(*) as cnt 
-- FROM exchange_rates 
-- GROUP BY base_currency, target_currency, rate_date 
-- HAVING cnt > 1;  -- Should be empty (uniqueness check)
