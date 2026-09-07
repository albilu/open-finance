package org.openfinance.util;

import java.math.BigDecimal;

/**
 * Single source of the <em>principal leg</em> computation shared by {@code TransactionService}
 * (write-path balance deltas), {@code NetWorthService} (historical balance reconstruction) and
 * {@code LiabilityTrancheService} (tranche allocation).
 *
 * <p>The principal leg of a liability-linked movement is its total minus the sum of the split
 * amounts that carry a {@code categoryId} (interest / insurance / fee legs), floored at zero. A
 * plain repayment without splits is entirely principal; a fully categorized (interest-only) payment
 * has no principal leg.
 */
public final class PrincipalLegs {

    private PrincipalLegs() {}

    /**
     * Computes the principal leg from a pre-reduced categorized sum.
     *
     * @param total the movement's total amount ({@code null} resolves to zero)
     * @param categorizedSum the sum of the movement's categorized split amounts ({@code null}
     *     resolves to zero)
     * @return {@code max(total − categorizedSum, 0)}, never negative
     */
    public static BigDecimal of(BigDecimal total, BigDecimal categorizedSum) {
        BigDecimal safeTotal = total != null ? total : BigDecimal.ZERO;
        BigDecimal safeCategorized = categorizedSum != null ? categorizedSum : BigDecimal.ZERO;
        return safeTotal.subtract(safeCategorized).max(BigDecimal.ZERO);
    }
}
