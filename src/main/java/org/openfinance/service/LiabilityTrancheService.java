package org.openfinance.service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openfinance.dto.TransactionSplitResponse;
import org.openfinance.entity.Liability;
import org.openfinance.entity.LiabilityTranche;
import org.openfinance.entity.MovementType;
import org.openfinance.entity.TrancheStatus;
import org.openfinance.entity.Transaction;
import org.openfinance.exception.InvalidTransactionException;
import org.openfinance.repository.LiabilityRepository;
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

    private final LiabilityRepository liabilityRepository;
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
     * against the liability and honored; otherwise the FIFO pick assigns the oldest DRAWN tranche
     * with remaining principal and persists the link on the transaction. Overpaying the target's
     * remaining principal is clamped by the reconciler's per-tranche cap.
     *
     * @param userId the owner's ID
     * @param liability the liability being repaid
     * @param tx the REPAYMENT transaction (mutated in place when a tranche is assigned)
     * @param principalLeg the repayment's principal leg (may be zero for interest-only payments)
     * @throws InvalidTransactionException if an explicit trancheId does not belong to the liability
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
        reconcile(liability);
        liabilityRepository.save(liability);
    }

    /**
     * Reconciles the spec §3.2 invariant: {@code Liability.currentBalance = SUM(tranche.remaining)}
     * over the DRAWN tranches, floored at zero. Assigns the balance absolutely (no clamp); a no-op
     * for liabilities without tranches, where the applied deltas remain authoritative.
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
        liability.setCurrentBalance(remainingSum.toPlainString());
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
}
