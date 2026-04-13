-- V45: Create transaction rules tables
-- Requirement: REQ-TR-NFR-2 (Persisted in relational tables with FK cascades)

-- Main rules table
CREATE TABLE transaction_rules (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id     INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name        TEXT    NOT NULL,
    priority    INTEGER NOT NULL DEFAULT 0,
    is_enabled  INTEGER NOT NULL DEFAULT 1,
    created_at  TEXT    NOT NULL DEFAULT (datetime('now')),
    updated_at  TEXT    NOT NULL DEFAULT (datetime('now'))
);

CREATE INDEX idx_transaction_rules_user_priority
    ON transaction_rules(user_id, is_enabled, priority);

-- Conditions table (AND-logic per rule)
CREATE TABLE transaction_rule_conditions (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    rule_id     INTEGER NOT NULL REFERENCES transaction_rules(id) ON DELETE CASCADE,
    field       TEXT    NOT NULL,
    operator    TEXT    NOT NULL,
    value       TEXT    NOT NULL,
    sort_order  INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_transaction_rule_conditions_rule
    ON transaction_rule_conditions(rule_id);

-- Actions table (applied in sort_order when all conditions match)
CREATE TABLE transaction_rule_actions (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    rule_id         INTEGER NOT NULL REFERENCES transaction_rules(id) ON DELETE CASCADE,
    action_type     TEXT    NOT NULL,
    action_value    TEXT,
    action_value2   TEXT,
    action_value3   TEXT,
    sort_order      INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_transaction_rule_actions_rule
    ON transaction_rule_actions(rule_id);
