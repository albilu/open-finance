-- Migration V22: Create attachments table for file attachment system
-- Sprint 12 - File Attachments & Polish (Task 12.1)
--
-- This table stores metadata about files attached to various financial entities:
-- - Transactions (receipts, invoices)
-- - Assets (purchase documents, certificates)
-- - Real Estate Properties (deeds, contracts, inspection reports)
-- - Liabilities (loan documents, statements)
--
-- Files are stored encrypted on the filesystem. This table stores metadata
-- and the path to the encrypted file, not the file content itself.
--
-- Requirement REQ-2.12: File Attachment System

CREATE TABLE IF NOT EXISTS attachments (
    id                 INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id            INTEGER NOT NULL,
    entity_type        VARCHAR(20) NOT NULL,  -- EntityType enum: TRANSACTION, ASSET, REAL_ESTATE, LIABILITY
    entity_id          INTEGER NOT NULL,      -- Foreign key to the associated entity
    file_name          VARCHAR(255) NOT NULL, -- Original filename as uploaded by user
    file_type          VARCHAR(100) NOT NULL, -- MIME type (e.g., "application/pdf", "image/jpeg")
    file_size          INTEGER NOT NULL,      -- Size in bytes
    file_path          VARCHAR(500) NOT NULL UNIQUE, -- Path to encrypted file on filesystem
    uploaded_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    description        VARCHAR(500),          -- Optional user description/notes
    
    -- Foreign key constraints
    CONSTRAINT fk_attachment_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    
    -- Check constraints for enum values
    CONSTRAINT chk_attachment_entity_type CHECK (entity_type IN (
        'TRANSACTION',
        'ASSET',
        'REAL_ESTATE',
        'LIABILITY'
    )),
    
    -- Validation constraints
    CONSTRAINT chk_attachment_file_size CHECK (file_size > 0),
    CONSTRAINT chk_attachment_entity_id CHECK (entity_id > 0)
);

-- Indexes for efficient querying

-- Primary index: Find all attachments for a user
CREATE INDEX IF NOT EXISTS idx_attachment_user_id ON attachments(user_id);

-- Critical index: Find attachments for a specific entity (e.g., transaction receipts)
CREATE INDEX IF NOT EXISTS idx_attachment_entity ON attachments(entity_type, entity_id);

-- Date index: Sort attachments by upload date
CREATE INDEX IF NOT EXISTS idx_attachment_uploaded_at ON attachments(uploaded_at DESC);

-- Composite index: User + entity type (e.g., all transaction receipts for user)
CREATE INDEX IF NOT EXISTS idx_attachment_user_entity_type ON attachments(user_id, entity_type);

-- Composite index: User + entity + upload date (common query pattern)
CREATE INDEX IF NOT EXISTS idx_attachment_user_entity_date ON attachments(user_id, entity_type, entity_id, uploaded_at DESC);

-- File type index: Filter by MIME type (e.g., show only PDFs or images)
CREATE INDEX IF NOT EXISTS idx_attachment_file_type ON attachments(file_type);

-- Comments
-- Table: attachments
-- Purpose: Store metadata for files attached to financial entities
-- Storage: Actual files stored encrypted on filesystem at path specified in file_path
-- Security: Files are encrypted at rest using AES-256-GCM
-- Related tables: users (via user_id), transactions, assets, real_estate_properties, liabilities (via entity_type + entity_id)
-- Lifecycle: Attachments are deleted when parent entity is deleted or user explicitly deletes them
-- File size limit: 10MB per file (enforced by application layer)
-- Allowed file types: PDF, images (PNG, JPG, GIF, WEBP), documents (DOC, DOCX, XLS, XLSX) - enforced by application layer
