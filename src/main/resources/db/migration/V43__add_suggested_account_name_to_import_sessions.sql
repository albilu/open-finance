-- Add suggested_account_name column to import_sessions table
ALTER TABLE import_sessions ADD COLUMN suggested_account_name VARCHAR(255);
