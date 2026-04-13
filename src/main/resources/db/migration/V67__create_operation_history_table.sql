-- Requirement: Undo/Redo Operation History
-- Stores a record of every CREATE/UPDATE/DELETE mutation on key financial entities.
-- The entity_snapshot_json captures the full entity state BEFORE the change (for undo).
-- The changed_fields_json captures a map of {field: {before, after}} for display.

CREATE TABLE IF NOT EXISTS operation_history (
    id                   INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id              INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    entity_type          TEXT    NOT NULL,
    entity_id            INTEGER,
    entity_label         TEXT,
    operation_type       TEXT    NOT NULL CHECK (operation_type IN ('CREATE', 'UPDATE', 'DELETE')),
    entity_snapshot_json TEXT,
    changed_fields_json  TEXT,
    undone_at            TIMESTAMP,
    redone_at            TIMESTAMP,
    created_at           TIMESTAMP NOT NULL DEFAULT (STRFTIME('%Y-%m-%dT%H:%M:%f', 'NOW'))
);

CREATE INDEX idx_op_history_user_id    ON operation_history(user_id);
CREATE INDEX idx_op_history_created_at ON operation_history(created_at DESC);
CREATE INDEX idx_op_history_entity     ON operation_history(entity_type, entity_id);
