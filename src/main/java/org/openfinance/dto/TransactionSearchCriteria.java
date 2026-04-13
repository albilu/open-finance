package org.openfinance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.openfinance.entity.TransactionType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for transaction search criteria.
 * 
 * <p>
 * This DTO is used to build dynamic queries for searching transactions.
 * All fields are optional - if a field is null, it won't be included in the
 * search filter.
 * Multiple criteria can be combined (AND logic).
 * </p>
 * 
 * <p>
 * <strong>Supported Search Filters:</strong>
 * <ul>
 * <li><strong>keyword</strong> - Search in description, notes, payee
 * (case-insensitive, partial match)</li>
 * <li><strong>accountId</strong> - Filter by specific account</li>
 * <li><strong>categoryId</strong> - Filter by specific category</li>
 * <li><strong>type</strong> - Filter by transaction type (INCOME, EXPENSE,
 * TRANSFER)</li>
 * <li><strong>dateFrom, dateTo</strong> - Filter by date range (inclusive)</li>
 * <li><strong>amountMin, amountMax</strong> - Filter by amount range</li>
 * <li><strong>tags</strong> - Search transactions containing specific tags</li>
 * <li><strong>isReconciled</strong> - Filter by reconciliation status</li>
 * </ul>
 * 
 * <p>
 * Requirement REQ-2.3.5: Transaction search and filtering
 * </p>
 * 
 * @see org.openfinance.entity.Transaction
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionSearchCriteria {

    /**
     * Keyword to search in description, notes, and payee fields (case-insensitive).
     * 
     * <p>
     * Uses LIKE query with wildcards: %keyword%
     * </p>
     * 
     * <p>
     * Example: "grocery" will match "Grocery store", "Buy groceries", etc.
     * </p>
     */
    private String keyword;

    /**
     * Filter by specific account ID.
     * 
     * <p>
     * If null, includes transactions from all user accounts.
     * </p>
     */
    private Long accountId;

    /**
     * Filter by specific category ID.
     * 
     * <p>
     * If null, includes transactions from all categories (including uncategorized).
     * </p>
     */
    private Long categoryId;

    /**
     * Filter by transaction type (INCOME, EXPENSE, TRANSFER).
     * 
     * <p>
     * If null, includes all transaction types.
     * </p>
     */
    private TransactionType type;

    /**
     * Filter transactions on or after this date (inclusive).
     * 
     * <p>
     * If null, no lower date bound.
     * </p>
     */
    private LocalDate dateFrom;

    /**
     * Filter transactions on or before this date (inclusive).
     * 
     * <p>
     * If null, no upper date bound.
     * </p>
     */
    private LocalDate dateTo;

    /**
     * Filter transactions with amount greater than or equal to this value.
     * 
     * <p>
     * If null, no lower amount bound.
     * </p>
     */
    private BigDecimal amountMin;

    /**
     * Filter transactions with amount less than or equal to this value.
     * 
     * <p>
     * If null, no upper amount bound.
     * </p>
     */
    private BigDecimal amountMax;

    /**
     * Search for transactions containing specific tags (comma-separated).
     * 
     * <p>
     * Uses LIKE query: tags field contains this value.
     * </p>
     * 
     * <p>
     * Example: "business" will match transactions with tags "business,travel" or
     * "business"
     * </p>
     */
    private String tags;

    /**
     * Filter by payee name (case-insensitive, exact or partial match depending on
     * specification).
     * 
     * <p>
     * If null, includes all payees.
     * </p>
     */
    private String payee;

    /**
     * Filter by reconciliation status.
     * 
     * <p>
     * If null, includes both reconciled and unreconciled transactions.
     * </p>
     * <ul>
     * <li>true - only reconciled transactions</li>
     * <li>false - only unreconciled transactions</li>
     * </ul>
     */
    private Boolean isReconciled;

    /**
     * Filter for transactions with no category assigned.
     * 
     * <p>
     * When true, only transactions where categoryId IS NULL are returned.
     * Mirrors the notification logic for uncategorized transactions.
     * </p>
     */
    private Boolean noCategory;

    /**
     * Filter for transactions with no payee assigned.
     * 
     * <p>
     * When true, only transactions where payee IS NULL or empty are returned.
     * Mirrors the notification logic for transactions without payees.
     * </p>
     */
    private Boolean noPayee;

    /**
     * Checks if any search criteria is provided.
     * 
     * @return true if at least one filter is set, false if all fields are null
     */
    public boolean hasAnyCriteria() {
        return keyword != null
                || accountId != null
                || categoryId != null
                || type != null
                || dateFrom != null
                || dateTo != null
                || amountMin != null
                || amountMax != null
                || tags != null
                || payee != null
                || isReconciled != null
                || Boolean.TRUE.equals(noCategory)
                || Boolean.TRUE.equals(noPayee);
    }
}
