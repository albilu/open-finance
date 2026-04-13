package org.openfinance.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for creating or updating a budget alert.
 * 
 * <p>Budget alerts notify users when spending approaches or exceeds budgeted amounts.
 * Common threshold values:
 * <ul>
 *   <li>50.00 - Early warning at 50% spent</li>
 *   <li>75.00 - Standard warning at 75% spent</li>
 *   <li>90.00 - Critical warning at 90% spent</li>
 *   <li>100.00 - Budget exceeded alert</li>
 * </ul>
 * 
 * <p><strong>Example JSON:</strong>
 * <pre>
 * {
 *   "threshold": 75.00,
 *   "isEnabled": true
 * }
 * </pre>
 * 
 * <p><strong>Requirements:</strong></p>
 * <ul>
 *   <li>REQ-2.9.4: Budget alert configuration</li>
 *   <li>REQ-2.9.4.2: Enable/disable alerts per budget</li>
 * </ul>
 * 
 * @author Open-Finance Development Team
 * @version 1.0
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetAlertRequest {
    
    /**
     * Alert threshold as percentage of budget amount.
     * 
     * <p>When spending reaches this percentage, the alert triggers.
     * Valid range: 1.00 to 150.00 (allows alerts for overspending)</p>
     * 
     * <p>Example: 75.00 means "alert when 75% of budget is spent"</p>
     */
    @NotNull(message = "{budget.alert.threshold.required}")
    @DecimalMin(value = "1.00", message = "{budget.alert.threshold.min}")
    @DecimalMax(value = "150.00", message = "{budget.alert.threshold.max}")
    private BigDecimal threshold;
    
    /**
     * Whether this alert is currently enabled.
     * 
     * <p>When false, alert will not trigger even if threshold is exceeded.
     * Defaults to true if not specified.</p>
     */
    @Builder.Default
    private Boolean isEnabled = true;
}
