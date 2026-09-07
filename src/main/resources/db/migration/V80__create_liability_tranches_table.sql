-- Migration: Create liability_tranches table.
-- Each row is one planned drawdown of a liability (e.g. a construction loan released in
-- stages). Tranche numbers are unique per liability. Monetary amounts are plain numerics.
-- Date: 2026-09-07

CREATE TABLE liability_tranches (
    id                  INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id             INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    liability_id        INTEGER NOT NULL REFERENCES liabilities(id) ON DELETE CASCADE,
    tranche_no          INTEGER NOT NULL,
    planned_amount      NUMERIC(19, 2) NOT NULL,
    drawn_amount        NUMERIC(19, 2),
    planned_date        DATE,
    drawn_date          DATE,
    fee                 NUMERIC(19, 2),
    interest_only       INTEGER NOT NULL DEFAULT 0,
    interest_only_until DATE,
    status              VARCHAR(20) NOT NULL DEFAULT 'PLANNED',
    -- Plain column (no foreign key): tranches may reference a property that is
    -- archived or not yet created.
    real_estate_id      INTEGER,
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

CREATE INDEX idx_liability_tranche_user_id ON liability_tranches(user_id);
CREATE INDEX idx_liability_tranche_liability_id ON liability_tranches(liability_id);
CREATE INDEX idx_liability_tranche_status ON liability_tranches(status);
