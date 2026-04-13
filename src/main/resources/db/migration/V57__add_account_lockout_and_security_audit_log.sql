-- Requirement TASK-15.1.8: Account lockout after N failed login attempts
-- Requirement TASK-15.1.7: Security audit log for authentication events
-- Adds login attempt tracking fields to the users table

ALTER TABLE users ADD COLUMN failed_login_attempts INTEGER NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN locked_until TIMESTAMP NULL;
ALTER TABLE users ADD COLUMN last_login_at TIMESTAMP NULL;
ALTER TABLE users ADD COLUMN last_login_ip TEXT NULL;

-- Security audit log for authentication events (TASK-15.1.7)
CREATE TABLE IF NOT EXISTS security_audit_log (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id      INTEGER NULL REFERENCES users(id) ON DELETE SET NULL,
    username     TEXT NOT NULL,
    event_type   TEXT NOT NULL CHECK (event_type IN (
                    'LOGIN_SUCCESS', 'LOGIN_FAILURE', 'LOGOUT',
                    'ACCOUNT_LOCKED', 'ACCOUNT_UNLOCKED',
                    'PASSWORD_CHANGED', 'MASTER_PASSWORD_CHANGED',
                    'REGISTRATION', 'UNAUTHORIZED_ACCESS')),
    ip_address   TEXT NOT NULL,
    user_agent   TEXT,
    details      TEXT,
    created_at   TIMESTAMP NOT NULL DEFAULT (STRFTIME('%Y-%m-%dT%H:%M:%f', 'NOW'))
);

CREATE INDEX idx_security_audit_log_user_id    ON security_audit_log(user_id);
CREATE INDEX idx_security_audit_log_event_type ON security_audit_log(event_type);
CREATE INDEX idx_security_audit_log_created_at ON security_audit_log(created_at);
CREATE INDEX idx_security_audit_log_ip_address ON security_audit_log(ip_address);
