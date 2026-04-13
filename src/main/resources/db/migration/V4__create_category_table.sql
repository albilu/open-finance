-- Migration: Create categories table
-- Version: V4
-- Description: Creates the categories table for organizing transactions
-- Author: Open-Finance
-- Date: 2026-01-31

-- Categories table for transaction categorization
-- Supports hierarchical structure with parent-child relationships
-- Requirement REQ-2.10: Category management
CREATE TABLE categories (
    id                 INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id            INTEGER NOT NULL,
    name               TEXT NOT NULL,                    -- Encrypted category name
    category_type      TEXT NOT NULL CHECK (category_type IN ('INCOME', 'EXPENSE')),
    parent_id          INTEGER,                           -- Self-referencing foreign key for subcategories
    icon               TEXT,                              -- Icon identifier (emoji or icon name)
    color              TEXT,                              -- Hex color code for UI
    is_system          INTEGER NOT NULL DEFAULT 0,       -- System-provided category flag (0=false, 1=true)
    created_at         TEXT NOT NULL,                    -- ISO 8601 datetime
    updated_at         TEXT,                              -- ISO 8601 datetime
    
    -- Foreign key constraint - ensures category belongs to valid user
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    
    -- Self-referencing foreign key - parent category relationship
    FOREIGN KEY (parent_id) REFERENCES categories(id) ON DELETE CASCADE
);

-- Index for efficient user-scoped queries
-- Most queries filter by user_id first
CREATE INDEX idx_category_user_id ON categories(user_id);

-- Index for filtering by category type (INCOME/EXPENSE)
-- Used in category selection dropdowns and reports
CREATE INDEX idx_category_type ON categories(category_type);

-- Index for hierarchical queries (finding subcategories)
-- Used when navigating category tree structure
CREATE INDEX idx_category_parent_id ON categories(parent_id);

-- Composite index for common query pattern: user's categories of specific type
-- Optimizes queries like "get all expense categories for user"
CREATE INDEX idx_category_user_type ON categories(user_id, category_type);

-- Comments explaining the table structure
-- SQLite doesn't support native column comments, so documenting here:
--
-- HIERARCHICAL STRUCTURE:
-- - parent_id IS NULL: Root category (top-level)
-- - parent_id NOT NULL: Subcategory
-- Example:
--   Shopping (parent_id=NULL)
--     ├─ Groceries (parent_id=Shopping.id)
--     ├─ Clothing (parent_id=Shopping.id)
--     └─ Electronics (parent_id=Shopping.id)
--
-- SYSTEM CATEGORIES:
-- - is_system=1: Default categories created on user registration
-- - is_system=0: User-created categories
-- System categories cannot be deleted but can be customized
--
-- ENCRYPTION:
-- - name field is encrypted for privacy
-- - Encryption/decryption handled in application layer
