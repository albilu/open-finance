-- Migration: Extend liability type check constraint
-- Author: Open Finance Development Team
-- Version: V59
-- Description: Adds STUDENT_LOAN and AUTO_LOAN to the chk_liability_type_valid
--              CHECK constraint.  SQLite does not support DROP/ADD CONSTRAINT via
--              ALTER TABLE, so the table is recreated with the updated constraint.
--
--              Old allowed values: LOAN, MORTGAGE, CREDIT_CARD, PERSONAL_LOAN, OTHER
--              New allowed values: LOAN, MORTGAGE, CREDIT_CARD, PERSONAL_LOAN,
--                                  STUDENT_LOAN, AUTO_LOAN, OTHER
--
-- Tables that reference liabilities(id):
--   transactions.liability_id    (ON DELETE SET NULL — safe to leave)
--   real_estate_properties.mortgage_id (no ON DELETE action — left intact)

PRAGMA foreign_keys=OFF;

-- 1. Create new table with updated CHECK constraint and all existing columns
CREATE TABLE liabilities_new (
    id                   INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id              INTEGER NOT NULL,
    name                 VARCHAR(512) NOT NULL,
    type                 VARCHAR(20)  NOT NULL,
    principal            VARCHAR(512) NOT NULL,
    current_balance      VARCHAR(512) NOT NULL,
    interest_rate        VARCHAR(512),
    start_date           DATE         NOT NULL,
    end_date             DATE,
    minimum_payment      VARCHAR(512),
    currency             VARCHAR(3)   NOT NULL,
    notes                TEXT,
    created_at           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    insurance_percentage VARCHAR(512),
    additional_fees      VARCHAR(512),
    institution_id       INTEGER REFERENCES institutions(id),

    CONSTRAINT fk_liabilities_new_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_liability_new_currency_length
        CHECK (LENGTH(currency) = 3),

    CONSTRAINT chk_liability_new_type_valid
        CHECK (type IN (
            'LOAN', 'MORTGAGE', 'CREDIT_CARD', 'PERSONAL_LOAN',
            'STUDENT_LOAN', 'AUTO_LOAN', 'OTHER'
        )),

    CONSTRAINT chk_liability_new_dates_logical
        CHECK (end_date IS NULL OR end_date >= start_date)
);

-- 2. Copy all existing rows (column order must match new table definition)
INSERT INTO liabilities_new (
    id, user_id, name, type, principal, current_balance, interest_rate,
    start_date, end_date, minimum_payment, currency, notes,
    created_at, updated_at,
    insurance_percentage, additional_fees, institution_id
)
SELECT
    id, user_id, name, type, principal, current_balance, interest_rate,
    start_date, end_date, minimum_payment, currency, notes,
    created_at, updated_at,
    insurance_percentage, additional_fees, institution_id
FROM liabilities;

-- 3. Drop old table
DROP TABLE liabilities;

-- 4. Rename new table to the canonical name
ALTER TABLE liabilities_new RENAME TO liabilities;

-- 5. Recreate indexes (same as V9 + V54/V55 performance indexes)
CREATE INDEX idx_liability_user_id    ON liabilities(user_id);
CREATE INDEX idx_liability_type       ON liabilities(type);
CREATE INDEX idx_liability_user_type  ON liabilities(user_id, type);
CREATE INDEX idx_liability_start_date ON liabilities(start_date);
CREATE INDEX idx_liability_end_date   ON liabilities(end_date);

PRAGMA foreign_keys=ON;
