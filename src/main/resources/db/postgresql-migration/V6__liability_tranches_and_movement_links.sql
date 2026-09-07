-- =============================================================================
-- V6: Liability tranches and transaction movement link columns.
--
-- Mirrors src/main/resources/db/migration/V79 (liability_tranches table) and V80
-- (movement link columns on transactions + archive mirror) for PostgreSQL
-- deployments. Parent table is created first so the inline REFERENCES in the
-- transaction link columns resolve. movement_type is VARCHAR(30): longest value
-- CAPITAL_IMPROVEMENT is 19 chars.
-- =============================================================================

CREATE TABLE IF NOT EXISTS liability_tranches (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id             BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    liability_id        BIGINT NOT NULL REFERENCES liabilities(id) ON DELETE CASCADE,
    tranche_no          INTEGER NOT NULL,
    planned_amount      NUMERIC(19, 2) NOT NULL,
    drawn_amount        NUMERIC(19, 2),
    planned_date        DATE,
    drawn_date          DATE,
    fee                 NUMERIC(19, 2),
    interest_only       BOOLEAN NOT NULL DEFAULT FALSE,
    interest_only_until DATE,
    status              VARCHAR(20) NOT NULL DEFAULT 'PLANNED',
    real_estate_id      BIGINT REFERENCES real_estate_properties(id) ON DELETE SET NULL,
    notes               TEXT,
    currency            VARCHAR(3) NOT NULL,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_liability_tranche_no UNIQUE (liability_id, tranche_no),
    CONSTRAINT chk_tranche_status_valid
        CHECK (status IN ('PLANNED', 'DRAWN', 'CANCELLED')),
    CONSTRAINT chk_tranche_currency_length
        CHECK (LENGTH(currency) = 3)
);

CREATE INDEX IF NOT EXISTS idx_liability_tranche_user_id
    ON liability_tranches(user_id);
CREATE INDEX IF NOT EXISTS idx_liability_tranche_liability_id
    ON liability_tranches(liability_id);
CREATE INDEX IF NOT EXISTS idx_liability_tranche_status
    ON liability_tranches(status);

-- V80: movement link columns on transactions (+ archive mirror, cf. V2).
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS tranche_id BIGINT REFERENCES liability_tranches(id) ON DELETE SET NULL;
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS real_estate_id BIGINT REFERENCES real_estate_properties(id) ON DELETE SET NULL;
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS asset_id BIGINT REFERENCES assets(id) ON DELETE SET NULL;
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS movement_type VARCHAR(30);

CREATE INDEX IF NOT EXISTS idx_transaction_tranche_id     ON transactions(tranche_id);
CREATE INDEX IF NOT EXISTS idx_transaction_real_estate_id ON transactions(real_estate_id);
CREATE INDEX IF NOT EXISTS idx_transaction_asset_id       ON transactions(asset_id);
CREATE INDEX IF NOT EXISTS idx_transaction_movement_type  ON transactions(movement_type);

ALTER TABLE transactions_archive ADD COLUMN IF NOT EXISTS tranche_id BIGINT;
ALTER TABLE transactions_archive ADD COLUMN IF NOT EXISTS real_estate_id BIGINT;
ALTER TABLE transactions_archive ADD COLUMN IF NOT EXISTS asset_id BIGINT;
ALTER TABLE transactions_archive ADD COLUMN IF NOT EXISTS movement_type VARCHAR(30);
