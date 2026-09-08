ALTER TABLE liability_tranches ALTER COLUMN created_at DROP DEFAULT;
ALTER TABLE liability_tranches ALTER COLUMN updated_at DROP DEFAULT;
ALTER TABLE liability_tranches ALTER COLUMN created_at TYPE VARCHAR(40)
    USING to_char(created_at, 'YYYY-MM-DD"T"HH24:MI:SS.US');
ALTER TABLE liability_tranches ALTER COLUMN updated_at TYPE VARCHAR(40)
    USING to_char(updated_at, 'YYYY-MM-DD"T"HH24:MI:SS.US');
ALTER TABLE liability_tranches ALTER COLUMN created_at SET DEFAULT to_char(now(), 'YYYY-MM-DD"T"HH24:MI:SS.US');
ALTER TABLE liability_tranches ALTER COLUMN updated_at SET DEFAULT to_char(now(), 'YYYY-MM-DD"T"HH24:MI:SS.US');
ALTER TABLE liability_tranches ALTER COLUMN planned_date TYPE VARCHAR(10) USING to_char(planned_date, 'YYYY-MM-DD');
ALTER TABLE liability_tranches ALTER COLUMN drawn_date TYPE VARCHAR(10) USING to_char(drawn_date, 'YYYY-MM-DD');
ALTER TABLE liability_tranches ALTER COLUMN interest_only_until TYPE VARCHAR(10) USING to_char(interest_only_until, 'YYYY-MM-DD');

ALTER TABLE users ADD COLUMN master_password_verifier VARCHAR(512);

-- Remove legacy cross-user split references without changing transaction amounts.
UPDATE transaction_splits SET category_id = NULL
WHERE category_id IS NOT NULL AND NOT EXISTS (
    SELECT 1 FROM categories c JOIN transactions t ON t.id = transaction_splits.transaction_id
    WHERE c.id = transaction_splits.category_id AND c.user_id = t.user_id
);
