-- Negative entity IDs preserve orphan receipts without linking them to live records.
CREATE TABLE IF NOT EXISTS attachments_with_tombstones (
    id                 INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id            INTEGER NOT NULL,
    entity_type        VARCHAR(25) NOT NULL,
    entity_id          INTEGER NOT NULL,
    file_name          VARCHAR(255) NOT NULL,
    file_type          VARCHAR(100) NOT NULL,
    file_size          INTEGER NOT NULL,
    file_data          BLOB NOT NULL,
    uploaded_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    description        VARCHAR(500),

    CONSTRAINT fk_attachment_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,

    CONSTRAINT chk_attachment_entity_type CHECK (entity_type IN (
        'TRANSACTION',
        'ASSET',
        'REAL_ESTATE',
        'LIABILITY',
        'ACCOUNT',
        'RECURRING_TRANSACTION'
    )),

    CONSTRAINT chk_attachment_file_size CHECK (file_size > 0),
    CONSTRAINT chk_attachment_entity_id CHECK (entity_id <> 0)
);

INSERT INTO attachments_with_tombstones SELECT * FROM attachments;
DROP TABLE attachments;
ALTER TABLE attachments_with_tombstones RENAME TO attachments;
CREATE INDEX IF NOT EXISTS idx_attachment_user_id ON attachments(user_id);
CREATE INDEX IF NOT EXISTS idx_attachment_entity ON attachments(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_attachment_uploaded_at ON attachments(uploaded_at DESC);
CREATE INDEX IF NOT EXISTS idx_attachment_user_entity_type ON attachments(user_id, entity_type);
CREATE INDEX IF NOT EXISTS idx_attachment_user_entity_date ON attachments(user_id, entity_type, entity_id, uploaded_at DESC);
CREATE INDEX IF NOT EXISTS idx_attachment_file_type ON attachments(file_type);
