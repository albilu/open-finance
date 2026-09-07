package org.openfinance.service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openfinance.dto.TransactionSplitResponse;
import org.openfinance.entity.Liability;
import org.openfinance.entity.LiabilityTranche;
import org.openfinance.entity.MovementType;
import org.openfinance.entity.TrancheStatus;
import org.openfinance.entity.Transaction;
import org.openfinance.exception.InvalidLiabilityStateException;
import org.openfinance.exception.InvalidTransactionException;
import org.openfinance.repository.LiabilityTrancheRepository;
import org.openfinance.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Owner of the tranche allocation ledger and the spec §3.2 invariant reconciler for staged loans.
 *
 * <p>There is no separate allocation entity: the principal allocated to a tranche is derived from
 * the non-deleted {@code REPAYMENT} transactions carrying its {@code trancheId}. Each repayment's
 * principal leg is its total amount minus the categorized split lines (interest, insurance, fees),
 * floored at zero; the per-tranche sum is capped at the drawn amount.
 *
 * <p><strong>V1 decision:</strong> a single payment allocates to a single tranche. An explicit
 * {@code trancheId} on the transaction is honored (validated against the liability); otherwise the
 * FIFO pick targets the oldest DRAWN tranche with remaining principal. A payment spanning several
 * tranches is NOT split — user targeting or sequential payments cover that case.
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class LiabilityTrancheService {

    /** FIFO order: earliest drawn date first (nulls last), then by tranche number. */
    private static final Comparator<LiabilityTranche> FIFO_ORDER =
            Comparator.comparing(
                            LiabilityTranche::getDrawnDate,
                            Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(LiabilityTranche::getTrancheNo);

    private final LiabilityTrancheRepository liabilityTrancheRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionSplitService transactionSplitService;

    /**
     * Principal allocated to a tranche by its non-deleted REPAYMENT transactions. Each repayment
     * contributes its principal leg (total minus categorized splits); the sum is capped at the
     * drawn amount.
     */
    @Transactional(readOnly = true)
    public BigDecimal allocatedPrincipal(LiabilityTranche tranche) {
        if (tranche.getDrawnAmount() == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal allocated = BigDecimal.ZERO;
        for (Transaction tx :
                transactionRepository.findByTrancheIdAndUserId(
                        tranche.getId(), tranche.getUserId())) {
            if (tx.getMovementType() != MovementType.REPAYMENT) {
                continue;
            }
            List<TransactionSplitResponse> splits =
                    transactionSplitService.getSplitsForTransaction(tx.getId());
            allocated = allocated.add(principalLeg(tx.getAmount(), splits));
        }
        return allocated.min(tranche.getDrawnAmount()).max(BigDecimal.ZERO);
    }

    /**
     * Outstanding drawn principal of a tranche: {@code drawnAmount − allocatedPrincipal}, never
     * negative. Zero for tranches that are not DRAWN (or have no drawn amount yet).
     */
    @Transactional(readOnly = true)
    public BigDecimal remainingOf(LiabilityTranche tranche) {
        if (tranche.getStatus() != TrancheStatus.DRAWN || tranche.getDrawnAmount() == null) {
            return BigDecimal.ZERO;
        }
        return tranche.getDrawnAmount().subtract(allocatedPrincipal(tranche)).max(BigDecimal.ZERO);
    }

    /**
     * Allocates a repayment's principal leg to a tranche (spec §Tranches).
     *
     * <p>When the transaction already carries a {@code trancheId} (advanced picker) it is validated
     * against the liability (membership and DRAWN status) and honored; otherwise the FIFO pick
     * assigns the oldest DRAWN tranche with remaining principal and persists the link on the
     * transaction. Overpaying the target's remaining principal is clamped by the reconciler's
     * per-tranche cap.
     *
     * <p><strong>Reconcile ownership:</strong> this method only links the tranche; it does NOT
     * reconcile or save the liability. Callers own the reconcile so that each write path runs
     * exactly ONE {@link #reconcile(Liability)} after all its mutations — the WARN inside the
     * reconciler then fires only on genuine drift, never on mid-flow intermediates.
     *
     * @param userId the owner's ID
     * @param liability the liability being repaid
     * @param tx the REPAYMENT transaction (mutated in place when a tranche is assigned)
     * @param principalLeg the repayment's principal leg (may be zero for interest-only payments)
     * @throws InvalidTransactionException if an explicit trancheId does not belong to the liability
     * @throws InvalidLiabilityStateException if an explicit trancheId targets a tranche that is not
     *     DRAWN
     */
    public void allocateRepayment(
            Long userId, Liability liability, Transaction tx, BigDecimal principalLeg) {
        if (principalLeg == null || principalLeg.signum() <= 0) {
            return;
        }
        List<LiabilityTranche> tranches =
                liabilityTrancheRepository.findByLiabilityIdAndUserId(liability.getId(), userId);
        if (tranches.isEmpty()) {
            return;
        }

        LiabilityTranche target;
        if (tx.getTrancheId() != null) {
            Long targetId = tx.getTrancheId();
            target =
                    tranches.stream()
                            .filter(t -> targetId.equals(t.getId()))
                            .findFirst()
                            .orElse(null);
            if (target == null) {
                throw new InvalidTransactionException(
                        String.format(
                                "Tranche %d does not belong to liability %d",
                                targetId, liability.getId()));
            }
            if (target.getStatus() != TrancheStatus.DRAWN) {
                throw InvalidLiabilityStateException.repaymentTargetNotDrawn(
                        target.getId(), target.getStatus());
            }
        } else {
            target = pickFifoTranche(tranches);
            if (target == null) {
                log.info(
                        "No DRAWN tranche with remaining principal on liability {}: repayment {}"
                                + " left unallocated",
                        liability.getId(),
                        principalLeg);
                return;
            }
            tx.setTrancheId(target.getId());
            transactionRepository.save(tx);
        }

        BigDecimal remaining = remainingOf(target);
        if (principalLeg.compareTo(remaining) > 0) {
            log.info(
                    "Repayment principal {} exceeds remaining {} of tranche {}: allocation clamped"
                            + " to the remaining principal (v1 single-tranche payments)",
                    principalLeg,
                    remaining,
                    target.getId());
        }
    }

    /**
     * Reconciles the spec §3.2 invariant: {@code Liability.currentBalance = SUM(tranche.remaining)}
     * over the DRAWN tranches, floored at zero. Assigns the balance absolutely (no clamp); a no-op
     * for liabilities without tranches, where the applied deltas remain authoritative.
     *
     * <p>When the re-derived value differs from the stored balance a WARN is emitted: the stored
     * balance was silently overridden, which signals untracked money (e.g. a disbursement without a
     * tranche) that deserves observability.
     */
    public void reconcile(Liability liability) {
        List<LiabilityTranche> tranches =
                liabilityTrancheRepository.findByLiabilityIdAndUserId(
                        liability.getId(), liability.getUserId());
        if (tranches.isEmpty()) {
            return;
        }
        BigDecimal remainingSum =
                tranches.stream()
                        .filter(t -> t.getStatus() == TrancheStatus.DRAWN)
                        .map(this::remainingOf)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .max(BigDecimal.ZERO);
        BigDecimal previous = parseBalanceOrNull(liability.getCurrentBalance());
        liability.setCurrentBalance(remainingSum.toPlainString());
        if (previous != null && previous.compareTo(remainingSum) != 0) {
            log.warn(
                    "Reconciler changed liability {} balance from {} to {} (SUM of DRAWN tranche"
                            + " remaining) — the stored balance contained untracked money",
                    liability.getId(),
                    previous.toPlainString(),
                    remainingSum.toPlainString());
        }
    }

    /**
     * Sums the drawn amounts of a liability's DRAWN tranches (null drawn amounts count as zero).
     * Single source for the disbursement fail-fast pre-check ({@link #assertDisbursementAllowed});
     * the invariant itself is owned by {@link #reconcile}.
     */
    @Transactional(readOnly = true)
    public BigDecimal sumDrawnOfDrawn(Long liabilityId, Long userId) {
        return liabilityTrancheRepository.findByLiabilityIdAndUserId(liabilityId, userId).stream()
                .filter(t -> t.getStatus() == TrancheStatus.DRAWN)
                .map(LiabilityTranche::getDrawnAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Shared fail-fast pre-check for the disbursement write paths ({@code
     * LiabilityService.disburse} and the raw DISBURSEMENT transaction path): the stored balance
     * must never exceed what the DRAWN tranches account for — otherwise the drawdown would silently
     * rely on untracked money that the reconciler would clamp away. Checked before any mutation so
     * the flow rolls back untouched.
     *
     * <p>The disbursement endpoint applies it unconditionally; the raw transaction path applies it
     * to staged loans only (tranches exist), where an unstaged liability's manually-managed balance
     * stays authoritative.
     *
     * @param liability the liability about to be drawn (loaded by the caller)
     * @param userId the owner's ID
     * @throws InvalidLiabilityStateException when the balance exceeds the total drawn amount of the
     *     DRAWN tranches
     */
    public void assertDisbursementAllowed(Liability liability, Long userId) {
        BigDecimal currentBalance = parseBalanceOrNull(liability.getCurrentBalance());
        if (currentBalance == null) {
            return;
        }
        BigDecimal drawnSum = sumDrawnOfDrawn(liability.getId(), userId);
        if (currentBalance.compareTo(drawnSum) > 0) {
            throw InvalidLiabilityStateException.balanceExceedsDrawnTranches(
                    liability.getId(), currentBalance, drawnSum);
        }
    }

    /**
     * Bulk variant of {@link #remainingOf} for listing endpoints: fetches the tranches' REPAYMENT
     * transactions in one query and their splits in one query, then derives each tranche's
     * remaining principal — avoiding the per-tranche (and per-transaction) N+1 of calling {@code
     * remainingOf} in a loop.
     *
     * @param userId the owner's ID (for authorization)
     * @param tranches the tranches to derive remaining principal for
     * @return remaining principal keyed by tranche ID; zero for tranches that are not DRAWN
     */
    @Transactional(readOnly = true)
    public Map<Long, BigDecimal> remainingByTrancheId(
            Long userId, List<LiabilityTranche> tranches) {
        if (tranches.isEmpty()) {
            return Map.of();
        }
        List<Long> trancheIds =
                tranches.stream().map(LiabilityTranche::getId).collect(Collectors.toList());
        List<Transaction> txs =
                transactionRepository.findByTrancheIdInAndUserId(trancheIds, userId);
        Map<Long, List<TransactionSplitResponse>> splitsByTx =
                txs.isEmpty()
                        ? Map.of()
                        : transactionSplitService.getSplitsForTransactions(
                                txs.stream().map(Transaction::getId).collect(Collectors.toList()));

        Map<Long, BigDecimal> allocatedByTranche = new HashMap<>();
        for (Transaction tx : txs) {
            if (tx.getMovementType() != MovementType.REPAYMENT) {
                continue;
            }
            BigDecimal principal =
                    principalLeg(tx.getAmount(), splitsByTx.getOrDefault(tx.getId(), List.of()));
            allocatedByTranche.merge(tx.getTrancheId(), principal, BigDecimal::add);
        }

        Map<Long, BigDecimal> remainingByTranche = new HashMap<>();
        for (LiabilityTranche tranche : tranches) {
            BigDecimal allocated =
                    allocatedByTranche.getOrDefault(tranche.getId(), BigDecimal.ZERO);
            remainingByTranche.put(tranche.getId(), remainingOf(tranche, allocated));
        }
        return remainingByTranche;
    }

    /** Picks the FIFO target: the oldest DRAWN tranche with remaining principal. */
    private LiabilityTranche pickFifoTranche(List<LiabilityTranche> tranches) {
        return tranches.stream()
                .filter(t -> t.getStatus() == TrancheStatus.DRAWN)
                .filter(t -> remainingOf(t).signum() > 0)
                .min(FIFO_ORDER)
                .orElse(null);
    }

    /**
     * Computes a movement's principal leg from stored splits: the total minus the sum of split
     * amounts carrying a categoryId, floored at zero.
     */
    private BigDecimal principalLeg(BigDecimal total, List<TransactionSplitResponse> splits) {
        BigDecimal categorized = BigDecimal.ZERO;
        if (splits != null) {
            for (TransactionSplitResponse split : splits) {
                if (split.getCategoryId() != null) {
                    categorized = categorized.add(split.getAmount());
                }
            }
        }
        BigDecimal safeTotal = total != null ? total : BigDecimal.ZERO;
        return safeTotal.subtract(categorized).max(BigDecimal.ZERO);
    }

    /** Pre-computed-allocation variant of {@link #remainingOf(LiabilityTranche)}. */
    private BigDecimal remainingOf(LiabilityTranche tranche, BigDecimal allocated) {
        if (tranche.getStatus() != TrancheStatus.DRAWN || tranche.getDrawnAmount() == null) {
            return BigDecimal.ZERO;
        }
        return tranche.getDrawnAmount()
                .subtract(allocated.min(tranche.getDrawnAmount()))
                .max(BigDecimal.ZERO);
    }

    /** Parses a plain balance string (null/blank/unparseable resolves to null). */
    private BigDecimal parseBalanceOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
