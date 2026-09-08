package org.openfinance.service;

import java.util.Map;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openfinance.config.EncryptionProperties;
import org.openfinance.security.EncryptionContext;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Runs the (potentially long-running) import confirmation on a background thread.
 *
 * <p>This lives in a dedicated Spring bean so that the {@link Async} proxy boundary is actually
 * crossed when {@link ImportService} triggers it. Calling an {@code @Async} method from within the
 * same bean (self-invocation) bypasses the proxy and executes synchronously, which is precisely the
 * behaviour we want to avoid here — the confirmation of large imports can take well over a minute
 * and must not hold the HTTP worker thread.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ImportConfirmationExecutor {

    private final ImportService importService;
    private final EncryptionProperties encryptionProperties;
    private final org.openfinance.security.UserEncryptionLock userEncryptionLock;
    private final MasterPasswordService masterPasswordService;
    private final org.openfinance.repository.UserRepository userRepository;

    /**
     * Execute the import confirmation asynchronously. On success the session is left in {@code
     * COMPLETED} status; on failure it is marked {@code FAILED} so the polling client can surface
     * an error.
     *
     * @param sessionId the import session ID
     * @param userId the owning user ID
     * @param accountId the target account ID (may be null for auto-creation)
     * @param categoryMappings imported-category-name to category-ID mappings
     * @param skipDuplicates whether to skip transactions flagged as duplicates
     */
    @Async("taskExecutor")
    public void runAsync(
            Long sessionId,
            Long userId,
            Long accountId,
            Map<String, Long> categoryMappings,
            boolean skipDuplicates,
            SecretKey encryptionKey) {
        try (org.openfinance.security.UserEncryptionLock.Scope ignored =
                userEncryptionLock.acquire(userId, false)) {
            if (encryptionProperties.isEnabled() && encryptionKey == null) {
                throw new IllegalStateException(
                        "An encryption key is required to confirm this import");
            }
            if (encryptionProperties.isEnabled()) {
                masterPasswordService.verify(
                        userRepository.findById(userId).orElseThrow(), encryptionKey);
            }
            EncryptionContext.setKey(encryptionKey);
            importService.confirmImport(
                    sessionId, userId, accountId, categoryMappings, skipDuplicates);
        } catch (Exception ex) {
            log.error(
                    "Async import confirmation failed for session {}: {}",
                    sessionId,
                    ex.getMessage(),
                    ex);
            importService.markImportFailed(sessionId, ex.getMessage());
        } finally {
            EncryptionContext.clear();
        }
    }
}
