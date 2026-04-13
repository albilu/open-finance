package org.openfinance.entity;

/**
 * Enumeration representing payment methods used for transactions.
 * 
 * <p>This enum categorizes the different ways a transaction can be paid for
 * or received:</p>
 * <ul>
 *   <li><strong>CASH</strong>: Physical currency</li>
 *   <li><strong>CHEQUE</strong>: Physical cheque</li>
 *   <li><strong>CREDIT_CARD</strong>: Credit card payment</li>
 *   <li><strong>DEBIT_CARD</strong>: Debit card payment</li>
 *   <li><strong>BANK_TRANSFER</strong>: Wire or ACH transfer</li>
 *   <li><strong>DEPOSIT</strong>: Direct deposit</li>
 *   <li><strong>STANDING_ORDER</strong>: Recurring automatic payment</li>
 *   <li><strong>DIRECT_DEBIT</strong>: Authorized automatic debit</li>
 *   <li><strong>ONLINE</strong>: Online payment (PayPal, etc.)</li>
 *   <li><strong>OTHER</strong>: Other payment methods</li>
 * </ul>
 * 
 * @see Transaction
 * @since 1.0
 */
public enum PaymentMethod {
    /**
     * Physical currency or cash payment.
     */
    CASH,
    
    /**
     * Physical cheque payment.
     */
    CHEQUE,
    
    /**
     * Credit card payment.
     */
    CREDIT_CARD,
    
    /**
     * Debit card payment.
     */
    DEBIT_CARD,
    
    /**
     * Bank wire transfer or ACH payment.
     */
    BANK_TRANSFER,
    
    /**
     * Direct deposit (e.g., salary).
     */
    DEPOSIT,
    
    /**
     * Standing order (recurring scheduled payment).
     */
    STANDING_ORDER,
    
    /**
     * Direct debit (authorized automatic debit).
     */
    DIRECT_DEBIT,
    
    /**
     * Online payment through third-party services (PayPal, Stripe, etc.).
     */
    ONLINE,
    
    /**
     * Other payment methods not listed above.
     */
    OTHER
}
