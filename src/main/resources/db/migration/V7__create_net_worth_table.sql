-- Migration: Create net_worth table
-- Description: Creates table to store daily snapshots of user's net worth (assets - liabilities)
-- Requirements: REQ-2.5.1, REQ-2.5.2

CREATE TABLE net_worth (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    snapshot_date DATE NOT NULL,
    total_assets DECIMAL(19, 2) NOT NULL,
    total_liabilities DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    net_worth DECIMAL(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'EUR',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign key constraint
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Unique index to prevent duplicate snapshots for same user and date
CREATE UNIQUE INDEX idx_net_worth_user_date ON net_worth(user_id, snapshot_date);

-- Index for date range queries (performance optimization)
CREATE INDEX idx_net_worth_snapshot_date ON net_worth(snapshot_date);

-- Comments for documentation
PRAGMA table_info(net_worth);
