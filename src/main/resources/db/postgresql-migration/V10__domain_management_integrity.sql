-- Current development schema: explicit principal ledger and financing relationships.
ALTER TABLE transactions ADD COLUMN principal_amount TEXT;
ALTER TABLE transactions_archive ADD COLUMN principal_amount TEXT;
CREATE TABLE liability_principal_allocations (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    liability_id BIGINT NOT NULL REFERENCES liabilities(id) ON DELETE CASCADE,
    transaction_id BIGINT NOT NULL REFERENCES transactions(id) ON DELETE CASCADE,
    tranche_id BIGINT REFERENCES liability_tranches(id),
    amount TEXT NOT NULL
);
CREATE INDEX idx_principal_allocation_liability ON liability_principal_allocations(liability_id, user_id);
CREATE INDEX idx_principal_allocation_transaction ON liability_principal_allocations(transaction_id, user_id);
CREATE INDEX idx_principal_allocation_tranche ON liability_principal_allocations(tranche_id, user_id);
ALTER TABLE liability_tranches ALTER COLUMN planned_amount TYPE TEXT USING planned_amount::TEXT;
ALTER TABLE liability_tranches ALTER COLUMN drawn_amount TYPE TEXT USING drawn_amount::TEXT;
ALTER TABLE liability_tranches ALTER COLUMN fee TYPE TEXT USING fee::TEXT;
ALTER TABLE liabilities ADD COLUMN opening_principal TEXT NOT NULL DEFAULT '0';
ALTER TABLE liabilities ADD COLUMN opening_balance TEXT NOT NULL DEFAULT '0';
ALTER TABLE liabilities ADD COLUMN credit_limit TEXT;
CREATE TABLE liability_asset_links (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    liability_id BIGINT NOT NULL REFERENCES liabilities(id) ON DELETE CASCADE,
    asset_id BIGINT NOT NULL REFERENCES assets(id) ON DELETE CASCADE,
    relationship VARCHAR(20) NOT NULL CHECK (relationship IN ('FINANCING', 'COLLATERAL')),
    allocation_percentage TEXT NOT NULL,
    UNIQUE (liability_id, asset_id, relationship)
);
CREATE INDEX idx_financing_asset ON liability_asset_links(asset_id, user_id);
ALTER TABLE liability_tranches ADD COLUMN direct_disbursement BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE liability_tranches ADD COLUMN reversed_date VARCHAR(10);
ALTER TABLE real_estate_value_history ADD COLUMN source_tranche_id BIGINT REFERENCES liability_tranches(id) ON DELETE SET NULL;
ALTER TABLE liabilities ADD COLUMN represented_by_account_id BIGINT REFERENCES accounts(id);
CREATE UNIQUE INDEX uq_liability_account_source ON liabilities(represented_by_account_id) WHERE represented_by_account_id IS NOT NULL;
ALTER TABLE assets ADD COLUMN acquisition_type VARCHAR(20) NOT NULL DEFAULT 'PURCHASE' CHECK (acquisition_type IN ('PURCHASE', 'GIFT', 'PLANNED'));
ALTER TABLE real_estate_properties ADD COLUMN acquisition_type VARCHAR(20) NOT NULL DEFAULT 'PURCHASE' CHECK (acquisition_type IN ('PURCHASE', 'GIFT', 'PLANNED'));
