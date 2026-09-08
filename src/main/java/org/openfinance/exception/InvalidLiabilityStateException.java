package org.openfinance.exception;

import java.math.BigDecimal;
import org.openfinance.entity.TrancheStatus;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a liability is in a contradictory state that must be reconciled before the
 * requested operation can proceed.
 *
 * <p>This exception is thrown when:
 *
 * <ul>
 *   <li>A disbursement is attempted while the liability balance already exceeds the total drawn
 *       amount of its DRAWN tranches (invariant: currentBalance = SUM(tranche.remaining))
 *   <li>A disbursement amount exceeds the planned amount of the tranche being drawn
 *   <li>The current balance is edited manually while linked transactions or tranches exist (the
 *       balance is owned by those movements and the tranche reconciler)
 *   <li>A repayment explicitly targets a tranche that is not DRAWN
 * </ul>
 *
 * <p>Requirement REQ-6.1: Liability Management - staged loan tranche invariants
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class InvalidLiabilityStateException extends RuntimeException
        implements LocalizableException {

    private final String messageKey;
    private final Object[] messageArgs;

    private InvalidLiabilityStateException(
            String message, String messageKey, Object[] messageArgs) {
        super(message);
        this.messageKey = messageKey;
        this.messageArgs = messageArgs;
    }

    /**
     * Factory method for a disbursement attempted while the balance exceeds the drawn tranches.
     *
     * @param liabilityId the liability ID in a contradictory state
     * @param balance the current liability balance
     * @param drawnSum the total drawn amount of DRAWN tranches
     * @return a new InvalidLiabilityStateException
     */
    public static InvalidLiabilityStateException balanceExceedsDrawnTranches(
            Long liabilityId, BigDecimal balance, BigDecimal drawnSum) {
        return new InvalidLiabilityStateException(
                String.format(
                        "Cannot disburse liability %d: current balance %s exceeds the total drawn"
                                + " amount %s of its DRAWN tranches (reconcile first)",
                        liabilityId, balance.toPlainString(), drawnSum.toPlainString()),
                "error.liability.balance.exceeds.drawn",
                new Object[] {liabilityId, balance.toPlainString(), drawnSum.toPlainString()});
    }

    /**
     * Factory method for a disbursement amount exceeding the tranche's planned amount.
     *
     * @param amount the requested disbursement amount
     * @param trancheId the tranche ID being overdrawn
     * @param plannedAmount the planned amount of the tranche
     * @return a new InvalidLiabilityStateException
     */
    public static InvalidLiabilityStateException disbursementOverdraw(
            BigDecimal amount, Long trancheId, BigDecimal plannedAmount) {
        return new InvalidLiabilityStateException(
                String.format(
                        "Cannot disburse %s on tranche %d: the amount exceeds the planned amount"
                                + " %s",
                        amount.toPlainString(), trancheId, plannedAmount.toPlainString()),
                "error.liability.disbursement.overdraw",
                new Object[] {amount.toPlainString(), trancheId, plannedAmount.toPlainString()});
    }

    /**
     * Factory method for a repayment allocation explicitly targeting a tranche that is not DRAWN.
     *
     * @param trancheId the tranche ID that was targeted
     * @param status the actual status of the tranche
     * @return a new InvalidLiabilityStateException
     */
    public static InvalidLiabilityStateException repaymentTargetNotDrawn(
            Long trancheId, TrancheStatus status) {
        return new InvalidLiabilityStateException(
                String.format(
                        "Cannot allocate a repayment to tranche %d: it is not DRAWN (status: %s)"
                                + " — disburse the tranche first",
                        trancheId, status),
                "error.liability.repayment.target.not.drawn",
                new Object[] {trancheId, status});
    }

    /**
     * Factory method for a manual balance edit attempted while linked transactions or tranches
     * exist.
     *
     * @param liabilityId the liability whose balance is locked
     * @return a new InvalidLiabilityStateException
     */
    public static InvalidLiabilityStateException liabilityBalanceLocked(Long liabilityId) {
        return new InvalidLiabilityStateException(
                String.format(
                        "Cannot manually edit the balance of liability %d: it is driven by linked"
                                + " transactions or disbursed tranches (edit or delete those"
                                + " instead)",
                        liabilityId),
                "error.liability.balance.locked",
                new Object[] {liabilityId});
    }

    /**
     * Factory method for a deletion attempted while drawn tranches or linked transactions exist.
     *
     * @param liabilityId the liability that cannot be deleted
     * @return a new InvalidLiabilityStateException
     */
    public static InvalidLiabilityStateException liabilityDeletionBlocked(Long liabilityId) {
        return new InvalidLiabilityStateException(
                String.format(
                        "Cannot delete liability %d: drawn tranches or linked transactions exist"
                                + " (repay or reassign them first)",
                        liabilityId),
                "error.liability.deletion.blocked",
                new Object[] {liabilityId});
    }

    @Override
    public String getMessageKey() {
        return messageKey;
    }

    @Override
    public Object[] getMessageArgs() {
        return messageArgs;
    }
}
