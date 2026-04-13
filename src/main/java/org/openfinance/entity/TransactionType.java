package org.openfinance.entity;

/**
 * Enumeration representing types of financial transactions.
 * 
 * <p>This enum categorizes transactions into three main types:</p>
 * <ul>
 *   <li><strong>INCOME</strong>: Money received (salary, dividends, gifts, etc.)</li>
 *   <li><strong>EXPENSE</strong>: Money spent (purchases, bills, fees, etc.)</li>
 *   <li><strong>TRANSFER</strong>: Money moved between accounts (internal transfer)</li>
 * </ul>
 * 
 * <p><strong>Requirement REQ-2.4.1.1</strong>: Transaction types (income, expense, transfer)</p>
 * 
 * @see Transaction
 * @since 1.0
 */
public enum TransactionType {
    /**
     * Represents income or money received.
     * 
     * <p>Examples: salary, dividends, interest, refunds, gifts, bonuses</p>
     * <p>Balance Impact: Increases account balance</p>
     */
    INCOME,
    
    /**
     * Represents expenses or money spent.
     * 
     * <p>Examples: groceries, rent, utilities, entertainment, shopping</p>
     * <p>Balance Impact: Decreases account balance</p>
     */
    EXPENSE,
    
    /**
     * Represents internal transfers between user's own accounts.
     * 
     * <p>Creates two linked transactions:</p>
     * <ul>
     *   <li>One debits the source account (decreases balance)</li>
     *   <li>One credits the destination account (increases balance)</li>
     * </ul>
     * 
     * <p><strong>Note</strong>: Transfers do not affect net worth since money
     * is only moving between accounts, not entering or leaving the system.</p>
     * 
     * <p>Balance Impact: Decreases source account, increases destination account</p>
     */
    TRANSFER
}
