-- Add institution_id column to liabilities table
ALTER TABLE liabilities ADD COLUMN institution_id INTEGER REFERENCES institutions(id);
