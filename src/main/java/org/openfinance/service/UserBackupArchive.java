package org.openfinance.service;

import java.io.IOException;
import java.nio.file.Path;

/** Portable SQLite archives containing one user's data. */
public interface UserBackupArchive {
    void write(Long userId, Path target) throws IOException;

    void validateDownload(Long userId, Path source) throws IOException;

    void restore(Long userId, Path source, String masterPassword) throws IOException;
}
