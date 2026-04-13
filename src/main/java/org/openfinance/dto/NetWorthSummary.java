package org.openfinance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO representing a net worth summary with monthly change statistics.
 * <p>
 * This DTO contains the user's current net worth broken down into assets and liabilities,
 * along with month-over-month change calculations (both absolute amount and percentage).
 * </p>
 *
 * <p><b>Requirements:</b></p>
 * <ul>
 *   <li>REQ-2.8.1.1: Dashboard Metrics - Display net worth and monthly change</li>
 * </ul>
 *
 * @see DashboardSummary
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NetWorthSummary {

    /**
     * The date for which this net worth summary is calculated.
     */
    private LocalDate date;

    /**
     * Total value of all assets (account balances, investments, etc.) in base currency.
     * This is the sum of all positive account balances.
     * <p>
     * Example: 10000.00 EUR
     * </p>
     */
    private BigDecimal totalAssets;

    /**
     * Total value of all liabilities (debts, loans, negative balances) in base currency.
     * This is the sum of all negative account balances (converted to positive).
     * <p>
     * Example: 2000.00 EUR
     * </p>
     */
    private BigDecimal totalLiabilities;

    /**
     * Net worth calculated as: totalAssets - totalLiabilities.
     * This can be negative if liabilities exceed assets.
     * <p>
     * Example: 8000.00 EUR (10000 - 2000)
     * </p>
     */
    private BigDecimal netWorth;

    /**
     * Month-over-month change in net worth (absolute amount).
     * Positive value indicates growth, negative indicates decline.
     * <p>
     * Example: +500.00 EUR (gained 500 EUR since last month)
     * </p>
     */
    private BigDecimal monthlyChangeAmount;

    /**
     * Month-over-month change in net worth (percentage).
     * Calculated as: (currentNetWorth - previousNetWorth) / previousNetWorth * 100.
     * Positive value indicates growth, negative indicates decline.
     * <p>
     * Example: +6.67% (net worth increased by 6.67% since last month)
     * </p>
     */
    private BigDecimal monthlyChangePercentage;

    /**
     * The currency in which all amounts are expressed (e.g., "EUR", "USD").
     */
    private String currency;
}
