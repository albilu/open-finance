-- V14: Create import_sessions table for tracking transaction file imports
-- Purpose: Track the progress and status of importing transactions from files (QIF, OFX, QFX)
-- Author: Backend Developer
-- Date: 2026-02-02

CREATE TABLE import_sessions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    upload_id VARCHAR(36) NOT NULL,
    user_id INTEGER NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_format VARCHAR(10),
    account_id INTEGER,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    total_transactions INTEGER NOT NULL DEFAULT 0,
    imported_count INTEGER NOT NULL DEFAULT 0,
    error_count INTEGER NOT NULL DEFAULT 0,
    duplicate_count INTEGER NOT NULL DEFAULT 0,
    skipped_count INTEGER NOT NULL DEFAULT 0,
    error_message VARCHAR(1000),
    metadata TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    
    -- Constraints
    CONSTRAINT fk_import_session_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_import_session_account FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE SET NULL,
    CONSTRAINT chk_import_session_status CHECK (status IN ('PENDING', 'PARSING', 'PARSED', 'REVIEWING', 'IMPORTING', 'COMPLETED', 'CANCELLED', 'FAILED')),
    CONSTRAINT chk_import_session_counts CHECK (
        total_transactions >= 0 AND 
        imported_count >= 0 AND 
        error_count >= 0 AND 
        duplicate_count >= 0 AND 
        skipped_count >= 0
    )
);

-- Indexes for efficient querying
CREATE INDEX idx_import_session_user ON import_sessions(user_id);
CREATE INDEX idx_import_session_upload ON import_sessions(upload_id);
CREATE INDEX idx_import_session_status ON import_sessions(status);
CREATE INDEX idx_import_session_created ON import_sessions(created_at);

-- Trigger to automatically update updated_at timestamp
CREATE TRIGGER update_import_session_timestamp 
AFTER UPDATE ON import_sessions
FOR EACH ROW
BEGIN
    UPDATE import_sessions 
    SET updated_at = CURRENT_TIMESTAMP 
    WHERE id = NEW.id;
END;
