package org.openfinance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.openfinance.entity.BudgetPeriod;
import org.openfinance.entity.CategoryType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Data Transfer Object for budget responses.
 * 
 * <p>This DTO is returned to clients when retrieving budget information.
 * It contains decrypted values and includes denormalized category information
 * for client convenience.</p>
 * 
 * <p>Requirement REQ-2.9.1.1: Budget creation and retrieval</p>
 * <p>Requirement REQ-2.9.1.2: Display budget information with category details</p>
 * 
 * @see org.openfinance.entity.Budget
 * @author Open-Finance Development Team
 * @version 1.0
 * @since 2026-02-02
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetResponse {
    
    /**
     * Unique identifier of the budget.
     */
    private Long id;
    
    /**
     * ID of the category this budget applies to.
     * 
     * <p>Requirement REQ-2.9.1.1: Budget associated with specific category</p>
     */
    private Long categoryId;
    
    /**
     * Name of the category (denormalized for convenience).
     * 
     * <p>Populated from Category entity to avoid client-side lookups.
     * This field is decrypted.</p>
     */
    private String categoryName;
    
    /**
     * Type of category - INCOME or EXPENSE.
     * 
     * <p>Denormalized from Category entity to help clients filter
     * and display budgets appropriately.</p>
     */
    private CategoryType categoryType;
    
    /**
     * Budget amount (decrypted).
     * 
     * <p>The total amount allocated for this category during the period.
     * Requirement REQ-2.9.1.1: Display budget amount</p>
     */
    private BigDecimal amount;
    
    /**
     * Currency code in ISO 4217 format (e.g., "USD", "EUR", "GBP").
     * 
     * <p>Requirement REQ-2.8: Multi-currency support</p>
     */
    private String currency;
    
    /**
     * Budget period type (WEEKLY, MONTHLY, QUARTERLY, YEARLY).
     * 
     * <p>Requirement REQ-2.9.1.1: Support multiple budget periods</p>
     */
    private BudgetPeriod period;
    
    /**
     * Start date of the budget period.
     * 
     * <p>First day when budget tracking begins.</p>
     */
    private LocalDate startDate;
    
    /**
     * End date of the budget period.
     * 
     * <p>Last day of budget tracking. Must be >= startDate.</p>
     */
    private LocalDate endDate;
    
    /**
     * Whether unused budget should roll over to next period.
     * 
     * <p>If true, unspent amount carries forward to the next period.
     * If false, budget resets at the start of each period.</p>
     * 
     * <p>Requirement REQ-2.9.1.1: Rollover option support</p>
     */
    private Boolean rollover;
    
    /**
     * Optional notes or description for the budget.
     * 
     * <p>User-provided explanation or reminders about the budget.</p>
     */
    private String notes;
    
    /**
     * Timestamp when the budget was created.
     */
    private LocalDateTime createdAt;
    
    /**
     * Timestamp when the budget was last updated.
     */
    private LocalDateTime updatedAt;
}
