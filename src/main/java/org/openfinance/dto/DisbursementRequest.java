package org.openfinance.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for disbursing a liability tranche.
 *
 * <p>Either {@code trancheId} (drawdown of a planned tranche) or {@code directRealEstateId} (direct
 * disbursement linked to a property) identifies the disbursement target.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DisbursementRequest {

    /** ID of the account receiving the disbursed funds. */
    @NotNull(message = "{disbursement.toAccount.required}")
    private Long toAccountId;

    /** ID of the real estate property for a direct disbursement. */
    private Long directRealEstateId;

    /** ID of the planned tranche being drawn. */
    private Long trancheId;

    /** Disbursed amount. */
    @NotNull(message = "{disbursement.amount.required}")
    @DecimalMin(value = "0.01", message = "{disbursement.amount.min}")
    @Digits(integer = 17, fraction = 2, message = "{disbursement.amount.digits}")
    private BigDecimal amount;

    /** Date of the disbursement. */
    @NotNull(message = "{disbursement.date.required}")
    private LocalDate date;

    /** Optional notes about the disbursement. */
    @Size(max = 1000, message = "{disbursement.notes.max}")
    private String notes;

    /**
     * Validates that exactly one disbursement target is set.
     *
     * <p>Called by Jakarta Bean Validation during full-request validation.
     *
     * @return true when exactly one of trancheId and directRealEstateId is set
     */
    @AssertTrue(message = "{disbursement.target.exclusive}")
    private boolean isTargetCoherent() {
        return (trancheId != null) ^ (directRealEstateId != null);
    }
}
