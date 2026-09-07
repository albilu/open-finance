package org.openfinance.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link PrincipalLegs}, the single source of the principal-leg computation shared
 * by {@code TransactionService}, {@code NetWorthService} and {@code LiabilityTrancheService}.
 */
@DisplayName("PrincipalLegs — principal leg of a movement (total − categorized splits)")
class PrincipalLegsTest {

    @Test
    @DisplayName("normal case: total minus categorized sum")
    void ofReturnsTotalMinusCategorizedSum() {
        assertThat(PrincipalLegs.of(new BigDecimal("1200"), new BigDecimal("400")))
                .isEqualByComparingTo("800");
    }

    @Test
    @DisplayName("zero categorized sum returns the full total")
    void ofWithZeroCategorizedReturnsTotal() {
        assertThat(PrincipalLegs.of(new BigDecimal("1200"), BigDecimal.ZERO))
                .isEqualByComparingTo("1200");
    }

    @Test
    @DisplayName("fully categorized movement floors at zero")
    void ofFloorsAtZero() {
        assertThat(PrincipalLegs.of(new BigDecimal("300"), new BigDecimal("300")))
                .isEqualByComparingTo("0");
        assertThat(PrincipalLegs.of(new BigDecimal("100"), new BigDecimal("300")))
                .isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("null total resolves to zero")
    void ofWithNullTotalResolvesToZero() {
        assertThat(PrincipalLegs.of(null, new BigDecimal("400"))).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("null categorized sum resolves to zero")
    void ofWithNullCategorizedResolvesToZero() {
        assertThat(PrincipalLegs.of(new BigDecimal("1200"), null)).isEqualByComparingTo("1200");
    }

    @Test
    @DisplayName("both null arguments resolve to zero")
    void ofWithBothNullResolvesToZero() {
        assertThat(PrincipalLegs.of(null, null)).isEqualByComparingTo("0");
    }
}
