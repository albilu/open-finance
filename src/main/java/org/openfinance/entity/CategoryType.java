package org.openfinance.entity;

/**
 * Enumeration for transaction category types.
 * 
 * <p>Categories are classified as either INCOME or EXPENSE to organize
 * financial transactions and facilitate budget tracking and reporting.</p>
 * 
 * <p><strong>Requirement REQ-2.10</strong>: Category management for income and expenses</p>
 * 
 * @see Category
 * @since 1.0
 */
public enum CategoryType {
    /**
     * Categories for income transactions.
     * 
     * <p>Examples: Salary, Freelance, Investments, Gifts</p>
     */
    INCOME,
    
    /**
     * Categories for expense transactions.
     * 
     * <p>Examples: Groceries, Rent, Utilities, Entertainment</p>
     */
    EXPENSE
}
