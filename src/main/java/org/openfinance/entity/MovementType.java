package org.openfinance.entity;

/**
 * Classification of a financial movement linked to a liability, asset or real estate property.
 *
 * <p>Mapped to {@code Transaction.movementType} since Task 2. Kept here so the vocabulary is fixed
 * before the mapping lands.
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
