-- Requirement 1.1.3: Database migration for users table
-- Create users table for Open-Finance application
-- This table stores user authentication and encryption metadata

-- Note: SQLite requires INTEGER PRIMARY KEY AUTOINCREMENT (not BIGINT)
-- SQLite maps INTEGER PRIMARY KEY to 64-bit signed integers (compatible with Java Long)
CREATE TABLE users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    master_password_salt VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL
);

-- Note: Explicit indexes on UNIQUE columns are redundant (UNIQUE constraint creates indexes)
-- Removed explicit index creation to avoid duplication
