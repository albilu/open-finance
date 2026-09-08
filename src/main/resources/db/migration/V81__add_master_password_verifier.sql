ALTER TABLE users ADD COLUMN master_password_verifier VARCHAR(512);

-- Remove legacy cross-user split references without changing transaction amounts.
UPDATE transaction_splits SET category_id = NULL
WHERE category_id IS NOT NULL AND NOT EXISTS (
    SELECT 1 FROM categories c JOIN transactions t ON t.id = transaction_splits.transaction_id
    WHERE c.id = transaction_splits.category_id AND c.user_id = t.user_id
);
