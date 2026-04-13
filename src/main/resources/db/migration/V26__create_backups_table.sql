-- Create backups table for backup management
-- Requirements: REQ-2.14.2.1, REQ-2.14.2.2

CREATE TABLE backups (
    id                INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id          INTEGER NOT NULL,
    filename          VARCHAR(255) NOT NULL,
    file_path         VARCHAR(500) NOT NULL,
    file_size         BIGINT NOT NULL,
    checksum          VARCHAR(64) NOT NULL,
    status            VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK(status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'FAILED')),
    backup_type       VARCHAR(20) NOT NULL DEFAULT 'MANUAL' CHECK(backup_type IN ('MANUAL', 'AUTOMATIC')),
    description       VARCHAR(500),
    error_message     VARCHAR(1000),
    created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create indexes for efficient queries
CREATE INDEX idx_backup_user_id ON backups(user_id);
CREATE INDEX idx_backup_created_at ON backups(created_at);
CREATE INDEX idx_backup_status ON backups(status);
CREATE INDEX idx_backup_type ON backups(backup_type);
CREATE INDEX idx_backup_user_status ON backups(user_id, status);
CREATE INDEX idx_backup_user_type ON backups(user_id, backup_type);
