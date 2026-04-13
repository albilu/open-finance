package org.openfinance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.openfinance.entity.BudgetPeriod;

import java.math.BigDecimal;
import java.util.List;

/**
 * Data Transfer Object for budget summary information.
 * 
 * <p>This DTO provides an aggregate view of all budgets for a specific period,
 * including total budgeted amounts, total spent, and individual budget details.</p>
 * 
 * <p>Used by the GET /api/budgets/summary?period=MONTHLY endpoint.</p>
 * 
 * <p>Requirement REQ-2.9.1.3: Budget reports with budgeted vs actual</p>
 * <p>Requirement REQ-2.9.1.3: Aggregate statistics across all budgets</p>
 * 
 * @author Open-Finance Development Team
 * @version 1.0
 * @since 2026-02-02
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetSummaryResponse {
    
    /**
     * Budget period being summarized.
     * 
     * <p>All budgets in the summary share this period type.</p>
     */
    private BudgetPeriod period;
    
    /**
     * Total number of budgets for this period.
     * 
     * <p>Includes all budgets regardless of status (active, expired, etc.).</p>
     */
    private Integer totalBudgets;
    
    /**
     * Number of currently active budgets.
     * 
     * <p>Budgets where current date falls between startDate and endDate.</p>
     */
    private Integer activeBudgets;
    
    /**
     * Total budgeted amount across all budgets.
     * 
     * <p>Sum of all budget amounts for this period.
     * Requirement REQ-2.9.1.3: Display total budgeted amount</p>
     */
    private BigDecimal totalBudgeted;
    
    /**
     * Total amount spent across all budgets.
     * 
     * <p>Sum of all spending in categories with budgets for this period.
     * Requirement REQ-2.9.1.3: Display total actual spending</p>
     */
    private BigDecimal totalSpent;
    
    /**
     * Total remaining amount across all budgets.
     * 
     * <p>Calculated as: totalBudgeted - totalSpent</p>
     * <p>Can be negative if over budget overall.</p>
     * 
     * <p>Requirement REQ-2.9.1.3: Calculate variance (budgeted - actual)</p>
     */
    private BigDecimal totalRemaining;
    
    /**
     * Average percentage spent across all budgets.
     * 
     * <p>Provides an overall budget health indicator.
     * Calculated as average of individual budget percentages.</p>
     */
    private BigDecimal averageSpentPercentage;
    
    /**
     * List of individual budget progress details.
     * 
     * <p>Contains full progress information for each budget in this period.
     * Allows clients to display detailed breakdowns.</p>
     * 
     * <p>Requirement REQ-2.9.1.3: Report individual budget performance</p>
     */
    private List<BudgetProgressResponse> budgets;
    
    /**
     * Currency code for all monetary values.
     * 
     * <p>Note: If user has budgets in multiple currencies, this summary
     * assumes single currency. Multi-currency summary requires currency
     * conversion logic (future enhancement).</p>
     * 
     * <p>Requirement REQ-2.8: Multi-currency awareness</p>
     */
    private String currency;
}
