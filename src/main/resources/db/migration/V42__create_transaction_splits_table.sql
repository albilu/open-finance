-- Migration V43: Create transaction_splits table
-- Requirement REQ-SPL-1.3: Splits are stored in a dedicated transaction_splits table
-- Requirement REQ-SPL-2.7: Deleting a parent transaction also deletes all its splits (cascade)

CREATE TABLE transaction_splits (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    transaction_id INTEGER NOT NULL REFERENCES transactions(id) ON DELETE CASCADE,
    category_id    INTEGER REFERENCES categories(id) ON DELETE SET NULL,
    amount         DECIMAL(19, 4) NOT NULL,
    description    VARCHAR(2000),  -- encrypted, optional; extra space for AES-256-GCM overhead
    created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP
);

CREATE INDEX idx_transaction_splits_transaction_id ON transaction_splits(transaction_id);
