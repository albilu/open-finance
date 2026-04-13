package org.openfinance.entity;

/**
 * Enumeration representing the frequency of recurring transactions.
 * 
 * <p>This enum defines how often a recurring transaction should repeat:</p>
 * <ul>
 *   <li><strong>DAILY</strong>: Repeats every day</li>
 *   <li><strong>WEEKLY</strong>: Repeats every 7 days</li>
 *   <li><strong>BIWEEKLY</strong>: Repeats every 14 days (twice a month)</li>
 *   <li><strong>MONTHLY</strong>: Repeats on the same day each month</li>
 *   <li><strong>QUARTERLY</strong>: Repeats every 3 months</li>
 *   <li><strong>YEARLY</strong>: Repeats on the same date each year</li>
 * </ul>
 * 
 * <p><strong>Requirement REQ-2.3.6</strong>: Recurring transactions with configurable frequency</p>
 * 
 * @see RecurringTransaction
 * @since 1.0
 */
public enum RecurringFrequency {
    /**
     * Repeats every day.
     * 
     * <p>Examples: daily coffee subscription, daily stock trading fees</p>
     * <p>Next occurrence: Current date + 1 day</p>
     */
    DAILY,
    
    /**
     * Repeats every 7 days (weekly).
     * 
     * <p>Examples: weekly gym membership, weekly cleaning service</p>
     * <p>Next occurrence: Current date + 7 days</p>
     */
    WEEKLY,
    
    /**
     * Repeats every 14 days (biweekly).
     * 
     * <p>Examples: biweekly payroll, biweekly mortgage payment</p>
     * <p>Next occurrence: Current date + 14 days</p>
     */
    BIWEEKLY,
    
    /**
     * Repeats monthly on the same day of the month.
     * 
     * <p>Examples: monthly rent, monthly utility bills, monthly subscriptions</p>
     * <p>Next occurrence: Same day of next month (handles month-end appropriately)</p>
     * 
     * <p><strong>Note</strong>: If the current day is 31 and next month has only 30 days,
     * the transaction will occur on day 30.</p>
     */
    MONTHLY,
    
    /**
     * Repeats every 3 months (quarterly).
     * 
     * <p>Examples: quarterly tax payments, quarterly HOA fees, quarterly insurance premiums</p>
     * <p>Next occurrence: Current date + 3 months</p>
     */
    QUARTERLY,
    
    /**
     * Repeats annually on the same date each year.
     * 
     * <p>Examples: annual insurance premiums, annual membership fees, annual taxes</p>
     * <p>Next occurrence: Same date next year</p>
     * 
     * <p><strong>Note</strong>: For February 29 (leap year), the next occurrence
     * in non-leap years will be February 28.</p>
     */
    YEARLY;
    
    /**
     * Returns a human-readable display name for the frequency.
     * 
     * @return Display name (e.g., "Daily", "Monthly", "Yearly")
     */
    public String getDisplayName() {
        return switch (this) {
            case DAILY -> "Daily";
            case WEEKLY -> "Weekly";
            case BIWEEKLY -> "Biweekly";
            case MONTHLY -> "Monthly";
            case QUARTERLY -> "Quarterly";
            case YEARLY -> "Yearly";
        };
    }
}
