-- Migration: Link transactions to liability tranches, real estate properties and assets.
-- Adds the movement classification columns consumed by the liability / asset / real-estate
-- module. All columns are nullable so existing rows are unaffected. Each FK is declared
-- inline at column-creation time; parents all exist when this migration runs
-- (liability_tranches via V79, real_estate_properties via V18, assets via V8).
-- movement_type is VARCHAR(30): longest value CAPITAL_IMPROVEMENT is 19 chars.
-- Date: 2026-09-07

ALTER TABLE transactions ADD COLUMN tranche_id INTEGER REFERENCES liability_tranches(id) ON DELETE SET NULL;
ALTER TABLE transactions ADD COLUMN real_estate_id INTEGER REFERENCES real_estate_properties(id) ON DELETE SET NULL;
ALTER TABLE transactions ADD COLUMN asset_id INTEGER REFERENCES assets(id) ON DELETE SET NULL;
ALTER TABLE transactions ADD COLUMN movement_type VARCHAR(30);

CREATE INDEX IF NOT EXISTS idx_transaction_tranche_id ON transactions(tranche_id);
CREATE INDEX IF NOT EXISTS idx_transaction_real_estate_id ON transactions(real_estate_id);
CREATE INDEX IF NOT EXISTS idx_transaction_asset_id ON transactions(asset_id);
CREATE INDEX IF NOT EXISTS idx_transaction_movement_type ON transactions(movement_type);

-- Mirror the new columns in the archive table so archival does not drop them (cf. V69, V75).
ALTER TABLE transactions_archive ADD COLUMN tranche_id INTEGER;
ALTER TABLE transactions_archive ADD COLUMN real_estate_id INTEGER;
ALTER TABLE transactions_archive ADD COLUMN asset_id INTEGER;
ALTER TABLE transactions_archive ADD COLUMN movement_type VARCHAR(30);
