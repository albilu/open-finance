package org.openfinance.entity;

/** Classification of a financial movement linked to a liability, asset or real estate property. */
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
