package org.openfinance.service;

import javax.crypto.SecretKey;
import org.openfinance.entity.User;

/** Authenticates master keys and atomically rotates encrypted user data. */
public interface MasterPasswordService {
    SecretKey derive(String password, String salt);

    String createVerifier(String password, String salt);

    void verifySentinel(String verifier, SecretKey key);

    void verify(User user, SecretKey key);

    String change(Long userId, String currentPassword, String newPassword);
}
