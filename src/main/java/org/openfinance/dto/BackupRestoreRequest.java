package org.openfinance.dto;

/** Password used when the portable backup was created; never persisted or logged. */
public record BackupRestoreRequest(String masterPassword) {
    @Override
    public String toString() {
        return "BackupRestoreRequest[masterPassword=REDACTED]";
    }
}
