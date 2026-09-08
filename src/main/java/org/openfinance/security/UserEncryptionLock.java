package org.openfinance.security;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.springframework.stereotype.Component;

/** Keeps encryption-key changes exclusive with in-flight work for the same user. */
@Component
public class UserEncryptionLock {
    private final ReentrantReadWriteLock[] locks = new ReentrantReadWriteLock[256];

    public UserEncryptionLock() {
        for (int i = 0; i < locks.length; i++) {
            locks[i] = new ReentrantReadWriteLock(true);
        }
    }

    public Scope acquire(Long userId, boolean exclusive) {
        ReentrantReadWriteLock userLock = locks[Math.floorMod(userId.hashCode(), locks.length)];
        Lock lock = exclusive ? userLock.writeLock() : userLock.readLock();
        lock.lock();
        return lock::unlock;
    }

    public interface Scope extends AutoCloseable {
        @Override
        void close();
    }
}
