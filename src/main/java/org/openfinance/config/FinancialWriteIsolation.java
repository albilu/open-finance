package org.openfinance.config;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Locks an owner's write transaction before any service reads its mutable financial state. */
@Aspect
@Configuration
@RequiredArgsConstructor
@EnableTransactionManagement(order = 100)
@Order(200)
public class FinancialWriteIsolation {
    private static final Object LOCKED_OWNERS = new Object();
    private final JdbcTemplate jdbc;

    @Around("execution(public * org.openfinance.service..*(..))")
    public Object isolate(ProceedingJoinPoint invocation) throws Throwable {
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && !TransactionSynchronizationManager.isCurrentTransactionReadOnly()) {
            Method method =
                    AopUtils.getMostSpecificMethod(
                            ((MethodSignature) invocation.getSignature()).getMethod(),
                            invocation.getTarget().getClass());
            Parameter[] parameters = method.getParameters();
            for (int i = 0; i < parameters.length; i++) {
                if (parameters[i].getName().equals("userId")
                        && invocation.getArgs()[i] instanceof Long userId) {
                    lockOwner(userId);
                    break;
                }
            }
        }
        return invocation.proceed();
    }

    @SuppressWarnings("unchecked")
    private void lockOwner(Long userId) {
        Set<Long> owners = (Set<Long>) TransactionSynchronizationManager.getResource(LOCKED_OWNERS);
        if (owners == null) {
            owners = new HashSet<>();
            Set<Long> transactionOwners = owners;
            TransactionSynchronizationManager.bindResource(LOCKED_OWNERS, owners);
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void suspend() {
                            TransactionSynchronizationManager.unbindResourceIfPossible(
                                    LOCKED_OWNERS);
                        }

                        @Override
                        public void resume() {
                            TransactionSynchronizationManager.bindResource(
                                    LOCKED_OWNERS, transactionOwners);
                        }

                        @Override
                        public void afterCompletion(int status) {
                            TransactionSynchronizationManager.unbindResourceIfPossible(
                                    LOCKED_OWNERS);
                        }
                    });
        }
        if (owners.add(userId)) {
            // PostgreSQL serializes this owner's writes; SQLite acquires its writer before
            // establishing a read snapshot. The lock is held until the outer transaction commits.
            jdbc.update("UPDATE users SET updated_at = updated_at WHERE id = ?", userId);
        }
    }
}
