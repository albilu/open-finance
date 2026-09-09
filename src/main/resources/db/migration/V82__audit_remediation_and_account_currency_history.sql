ALTER TABLE transactions ADD COLUMN account_amount VARCHAR(512);
ALTER TABLE transactions ADD COLUMN account_currency VARCHAR(3);
ALTER TABLE transactions_archive ADD COLUMN account_amount VARCHAR(512);
ALTER TABLE transactions_archive ADD COLUMN account_currency VARCHAR(3);
ALTER TABLE import_sessions ADD COLUMN confirmation_started BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE account_currency_changes (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    account_id INTEGER NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    effective_date TEXT NOT NULL,
    from_currency VARCHAR(3) NOT NULL,
    to_currency VARCHAR(3) NOT NULL,
    rate VARCHAR(512) NOT NULL
);
CREATE INDEX idx_account_currency_changes_owner ON account_currency_changes(user_id, account_id, effective_date);

-- Remove associations to another owner's private catalog entries. User data itself is retained.
UPDATE accounts SET institution_id = NULL WHERE institution_id IN (
    SELECT id FROM institutions WHERE is_system = FALSE AND user_id <> accounts.user_id);
UPDATE liabilities SET institution_id = NULL WHERE institution_id IN (
    SELECT id FROM institutions WHERE is_system = FALSE AND user_id <> liabilities.user_id);
UPDATE payees SET category_id = NULL WHERE category_id IN (
    SELECT id FROM categories WHERE payees.user_id IS NULL OR user_id <> payees.user_id);
UPDATE transactions SET payee_id = NULL WHERE payee_id IN (
    SELECT id FROM payees WHERE is_system = FALSE AND user_id <> transactions.user_id);

-- Make existing invalid category hierarchies usable without deleting any category.
WITH RECURSIVE ancestry(origin, id, parent_id, path, cycle) AS (
    SELECT id, id, parent_id, ',' || id || ',', FALSE FROM categories
    UNION ALL
    SELECT ancestry.origin, category.id, category.parent_id,
           ancestry.path || category.id || ',', instr(ancestry.path, ',' || category.id || ',') > 0
    FROM ancestry JOIN categories category ON category.id = ancestry.parent_id
    WHERE ancestry.cycle = FALSE
)
UPDATE categories SET parent_id = NULL WHERE id IN (SELECT origin FROM ancestry WHERE cycle = TRUE);
