package org.openfinance.service;

import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import org.openfinance.entity.User;
import org.openfinance.security.EncryptionKeyCache;
import org.openfinance.security.EncryptionService;
import org.openfinance.security.KeyManagementService;
import org.openfinance.security.UserEncryptionLock;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class MasterPasswordServiceImpl implements MasterPasswordService {
    private static final String VERIFIER = "open-finance-master-key-v1";
    private final JdbcTemplate jdbc;
    private final EncryptionService encryption;
    private final KeyManagementService keys;
    private final EncryptedUserDataService userData;
    private final EncryptionKeyCache cache;
    private final UserEncryptionLock lock;
    private final PlatformTransactionManager transactionManager;

    public SecretKey derive(String password, String salt) {
        char[] chars = password.toCharArray();
        try {
            return keys.deriveKey(chars, Base64.getDecoder().decode(salt));
        } finally {
            Arrays.fill(chars, '\0');
        }
    }

    public String createVerifier(String password, String salt) {
        return encryption.encrypt(VERIFIER, derive(password, salt));
    }

    public void verifySentinel(String verifier, SecretKey key) {
        try {
            if (!VERIFIER.equals(encryption.decrypt(verifier, key)))
                throw new IllegalArgumentException();
        } catch (RuntimeException ex) {
            throw new BadCredentialsException("Invalid master password");
        }
    }

    public void verify(User user, SecretKey key) {
        String currentSalt =
                jdbc.queryForObject(
                        "SELECT master_password_salt FROM users WHERE id = ?",
                        String.class,
                        user.getId());
        if (!user.getMasterPasswordSalt().equals(currentSalt))
            throw new BadCredentialsException("Master password changed; sign in again");
        if (user.getMasterPasswordVerifier() != null) {
            verifySentinel(user.getMasterPasswordVerifier(), key);
            userData.protectLegacyPayloads(user.getId(), key);
            return;
        }
        try {
            userData.verifyLegacyKey(user.getId(), key);
        } catch (RuntimeException ex) {
            throw new BadCredentialsException("Invalid master password");
        }
        jdbc.update(
                "UPDATE users SET master_password_verifier = ? WHERE id = ? AND master_password_verifier IS NULL AND master_password_salt = ?",
                encryption.encrypt(VERIFIER, key),
                user.getId(),
                user.getMasterPasswordSalt());
        userData.protectLegacyPayloads(user.getId(), key);
    }

    /** The write lock remains held through commit and session replacement. */
    public String change(Long userId, String currentPassword, String newPassword) {
        if (newPassword == null || newPassword.length() < 8)
            throw new IllegalArgumentException(
                    "Master password must contain at least 8 characters");
        try (UserEncryptionLock.Scope ignored = lock.acquire(userId, true)) {
            SecretKey key =
                    new TransactionTemplate(transactionManager)
                            .execute(
                                    status -> {
                                        if (jdbc.update(
                                                        "UPDATE users SET master_password_salt = master_password_salt WHERE id = ?",
                                                        userId)
                                                != 1) {
                                            throw new IllegalArgumentException("User not found");
                                        }
                                        Map<String, Object> row =
                                                jdbc.queryForMap(
                                                        "SELECT master_password_salt, master_password_verifier FROM users WHERE id = ?",
                                                        userId);
                                        String salt = (String) row.get("master_password_salt");
                                        SecretKey oldKey = derive(currentPassword, salt);
                                        verify(
                                                User.builder()
                                                        .id(userId)
                                                        .masterPasswordSalt(salt)
                                                        .masterPasswordVerifier(
                                                                (String)
                                                                        row.get(
                                                                                "master_password_verifier"))
                                                        .build(),
                                                oldKey);
                                        String newSalt =
                                                Base64.getEncoder()
                                                        .encodeToString(keys.generateSalt());
                                        SecretKey newKey = derive(newPassword, newSalt);
                                        userData.rotate(userId, oldKey, newKey);
                                        jdbc.update(
                                                "UPDATE users SET master_password_salt = ?, master_password_verifier = ? WHERE id = ?",
                                                newSalt,
                                                encryption.encrypt(VERIFIER, newKey),
                                                userId);
                                        return newKey;
                                    });
            cache.invalidateUserSessions(userId);
            cache.cacheKey(userId, key);
            return cache.createSession(userId, key);
        }
    }
}
