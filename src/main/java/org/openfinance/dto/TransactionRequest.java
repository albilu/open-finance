package org.openfinance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.openfinance.entity.PaymentMethod;
import org.openfinance.entity.TransactionType;
import org.openfinance.util.TagsDeserializer;
import org.openfinance.validation.ValidCurrency;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for creating or updating a transaction.
 * 
 * <p>
 * This DTO is used for both POST (create) and PUT (update) operations.
 * Validation annotations ensure data integrity before processing.
 * </p>
 * 
 * <p>
 * Requirement REQ-2.3.1: Transaction creation with account, amount, category,
 * and date
 * </p>
 * <p>
 * Requirement REQ-2.3.2: Transaction updates
 * </p>
 * 
 * @see org.openfinance.entity.Transaction
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionRequest {

    /**
     * ID of the account this transaction belongs to.
     * 
     * <p>
     * For TRANSFER transactions, this is the source account (money leaving).
     * </p>
     * 
     * <p>
     * Requirement REQ-2.3.1: Transaction must be associated with an account
     * </p>
     */
    @NotNull(message = "{transaction.account.required}")
    private Long accountId;

    /**
     * ID of the destination account (only for TRANSFER transactions).
     * 
     * <p>
     * For INCOME and EXPENSE transactions, this should be null.
     * </p>
     * 
     * <p>
     * Requirement REQ-2.3.4: Support for transfers between accounts
     * </p>
     */
    private Long toAccountId;

    /**
     * Type of transaction: INCOME, EXPENSE, or TRANSFER.
     * 
     * <p>
     * Requirement REQ-2.3.1: Classify transactions by type
     * </p>
     */
    @NotNull(message = "{transaction.type.required}")
    private TransactionType type;

    /**
     * Amount of the transaction.
     * 
     * <p>
     * Must be positive. For expenses, the service layer will handle the sign.
     * </p>
     * 
     * <p>
     * Requirement REQ-2.3.1: Record transaction amount
     * </p>
     */
    @NotNull(message = "{transaction.amount.required}")
    @DecimalMin(value = "0.01", message = "{transaction.amount.greater}")
    @Digits(integer = 15, fraction = 4, message = "{transaction.amount.digits}")
    private BigDecimal amount;

    /**
     * Currency code in ISO 4217 format (e.g., "USD", "EUR", "GBP").
     * 
     * <p>
     * Should match the account's currency for consistency.
     * </p>
     * 
     * <p>
     * Requirement REQ-2.8: Multi-currency support
     * </p>
     */
    @NotBlank(message = "{account.currency.required}")
    @ValidCurrency
    private String currency;

    /**
     * ID of the category this transaction belongs to (optional).
     * 
     * <p>
     * Category must match the transaction type (INCOME category for INCOME
     * transaction).
     * </p>
     * <p>
     * For TRANSFER transactions, category is typically null.
     * </p>
     * 
     * <p>
     * Requirement REQ-2.3.1: Categorize transactions
     * </p>
     */
    private Long categoryId;

    /**
     * Date when the transaction occurred.
     * 
     * <p>
     * Not the same as createdAt - this is the actual transaction date
     * (e.g., purchase date, deposit date). Can be in the past or future.
     * </p>
     * 
     * <p>
     * Note: Future dates are allowed for financial planning purposes.
     * For recurring scheduled transactions, use RecurringTransaction instead.
     * </p>
     * 
     * <p>
     * Requirement REQ-2.3.1: Record transaction date
     * </p>
     */
    @NotNull(message = "{transaction.date.required}")
    private LocalDate date;

    /**
     * Brief description of the transaction (optional, encrypted).
     * 
     * <p>
     * Examples: "Grocery shopping at Walmart", "Monthly salary"
     * </p>
     * 
     * <p>
     * Requirement REQ-2.3.1: Add transaction descriptions
     * </p>
     */
    @Size(max = 255, message = "{transaction.description.max}")
    private String description;

    /**
     * Additional notes about the transaction (optional, encrypted).
     * 
     * <p>
     * More detailed information than description. For user's reference.
     * </p>
     * 
     * <p>
     * Requirement REQ-2.3.1: Add detailed notes
     * </p>
     */
    @Size(max = 1000, message = "{transaction.notes.max}")
    private String notes;

    /**
     * Comma-separated tags for organization (optional).
     * 
     * <p>
     * Examples: "business,client-meeting,reimbursable"
     * </p>
     * 
     * <p>
     * Requirement REQ-2.3.5: Tag transactions for search/filtering
     * </p>
     */
    @Size(max = 500, message = "{transaction.tags.max}")
    @JsonDeserialize(using = TagsDeserializer.class)
    private String tags;

    /**
     * Optional payee name (person or merchant).
     * 
     * <p>
     * Examples: "Amazon", "John Doe", "Starbucks"
     * </p>
     * 
     * <p>
     * Requirement REQ-2.3.1: Track payee information
     * </p>
     */
    @Size(max = 100, message = "{transaction.payee.max}")
    private String payee;

    /**
     * Payment method used for this transaction (optional).
     * 
     * <p>
     * How the transaction was paid: cash, cheque, credit card, debit card,
     * bank transfer, deposit, standing order, direct debit, online, or other.
     * </p>
     * 
     * <p>
     * Requirement REQ-2.3.1: Track payment method
     * </p>
     */
    private PaymentMethod paymentMethod;
    
    /**
     * Flag indicating whether the transaction has been reconciled with bank
     * statement.
     * 
     * <p>
     * Defaults to false. Users manually mark transactions as reconciled.
     * </p>
     * 
     * <p>
     * Requirement REQ-2.3.3: Support transaction reconciliation
     * </p>
     */
    @Builder.Default
    private Boolean isReconciled = false;

    /**
     * Optional ID of a liability that this transaction is a payment for.
     *
     * <p>When set, this transaction (typically EXPENSE) is linked to a loan,
     * mortgage, or other liability. This allows tracking of loan payments
     * and computing the breakdown of principal, interest, and fees paid.</p>
     *
     * <p>Requirement REQ-LIA-4: Transaction-liability linking</p>
     */
    private Long liabilityId;

    /**
     * Optional list of split lines for this transaction.
     *
     * <p>When present, the transaction amount is distributed across the provided
     * split entries. The sum of all split amounts must equal {@code amount} within
     * a ±0.01 tolerance. Splits are only valid for INCOME and EXPENSE
     * transactions — not TRANSFER.</p>
     *
     * <p>Requirement REQ-SPL-2.1: Accept splits on POST</p>
     * <p>Requirement REQ-SPL-2.2: Accept splits on PUT (replace existing)</p>
     * <p>Requirement REQ-SPL-1.5: Splits only for INCOME/EXPENSE</p>
     */
    @Valid
    private List<TransactionSplitRequest> splits;
}
