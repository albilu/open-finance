package org.openfinance.entity;

/**
 * Classification of a financial movement linked to a liability, asset or real estate property.
 *
 * <p>Not yet mapped to an entity field: it will be mapped to {@code Transaction.movementType} in
 * Task 2 (deferred). Kept here so the vocabulary is fixed before the mapping lands.
 */
public enum MovementType {
    DISBURSEMENT,
    REPAYMENT,
    INTEREST,
    INSURANCE,
    FEE,
    CAPITAL_IMPROVEMENT,
    MAINTENANCE,
    REVALUATION
}
