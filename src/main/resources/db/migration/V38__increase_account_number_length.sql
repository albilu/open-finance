-- Increase account_number column length to accommodate encrypted values
-- The original length of 50 was too short for the 64-character encrypted tokens
-- In SQLite, VARCHAR(N) is equivalent to TEXT and does not enforce a length limit,
-- so this migration is technically redundant but kept for versioning consistency.

-- ALTER TABLE accounts ALTER COLUMN account_number TYPE VARCHAR(512);
;
