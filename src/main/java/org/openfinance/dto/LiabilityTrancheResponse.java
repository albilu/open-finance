package org.openfinance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.openfinance.entity.TrancheStatus;

/**
 * Data Transfer Object for liability tranche responses.
 *
 * <p>The {@code remaining} field is computed (planned amount minus drawn amount), not persisted.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiabilityTrancheResponse {

    /** Unique identifier of the tranche. */
    private Long id;

    /** ID of the liability this tranche belongs to. */
    private Long liabilityId;

    /** Sequential number of the tranche within its liability. */
    private Integer trancheNo;

    /** Planned drawdown amount. */
    private BigDecimal plannedAmount;

    /** Amount already drawn. */
    private BigDecimal drawnAmount;

    /** Computed remaining amount (planned minus drawn). Not persisted. */
    private BigDecimal remaining;

    /** Planned drawdown date. */
    private LocalDate plannedDate;

    /** Actual drawdown date. */
    private LocalDate drawnDate;

    /** Lifecycle status of the tranche. */
    private TrancheStatus status;

    /** ID of the linked real estate property. */
    private Long realEstateId;

    /** Currency code in ISO 4217 format. */
    private String currency;
}
