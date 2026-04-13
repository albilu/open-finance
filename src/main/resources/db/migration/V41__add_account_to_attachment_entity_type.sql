-- Migration V42: Add ACCOUNT to attachment entity type constraint
-- Extends the attachments table to support account-level file attachments
--
-- SQLite does not support ALTER CONSTRAINT, so we recreate the table with
-- the updated CHECK constraint that includes 'ACCOUNT'.

-- Step 1: Create the new table with the updated constraint
CREATE TABLE IF NOT EXISTS attachments_new (
    id                 INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id            INTEGER NOT NULL,
    entity_type        VARCHAR(20) NOT NULL,
    entity_id          INTEGER NOT NULL,
    file_name          VARCHAR(255) NOT NULL,
    file_type          VARCHAR(100) NOT NULL,
    file_size          INTEGER NOT NULL,
    file_path          VARCHAR(500) NOT NULL UNIQUE,
    uploaded_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    description        VARCHAR(500),

    CONSTRAINT fk_attachment_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,

    CONSTRAINT chk_attachment_entity_type CHECK (entity_type IN (
        'TRANSACTION',
        'ASSET',
        'REAL_ESTATE',
        'LIABILITY',
        'ACCOUNT'
    )),

    CONSTRAINT chk_attachment_file_size CHECK (file_size > 0),
    CONSTRAINT chk_attachment_entity_id CHECK (entity_id > 0)
);

-- Step 2: Copy all existing data
INSERT INTO attachments_new SELECT * FROM attachments;

-- Step 3: Drop the old table
DROP TABLE attachments;

-- Step 4: Rename new table to attachments
ALTER TABLE attachments_new RENAME TO attachments;

-- Step 5: Recreate indexes
CREATE INDEX IF NOT EXISTS idx_attachment_user_id ON attachments(user_id);
CREATE INDEX IF NOT EXISTS idx_attachment_entity ON attachments(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_attachment_uploaded_at ON attachments(uploaded_at DESC);
CREATE INDEX IF NOT EXISTS idx_attachment_user_entity_type ON attachments(user_id, entity_type);
CREATE INDEX IF NOT EXISTS idx_attachment_user_entity_date ON attachments(user_id, entity_type, entity_id, uploaded_at DESC);
CREATE INDEX IF NOT EXISTS idx_attachment_file_type ON attachments(file_type);
