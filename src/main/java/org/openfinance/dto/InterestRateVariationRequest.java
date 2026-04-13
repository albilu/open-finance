package org.openfinance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterestRateVariationRequest {

    @NotNull(message = "{interest.rate.required}")
    private BigDecimal rate;

    private BigDecimal taxRate;

    @NotNull(message = "{interest.valid.from.required}")
    private LocalDate validFrom;
}
