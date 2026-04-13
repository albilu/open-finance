package org.openfinance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.openfinance.entity.AssetCondition;
import org.openfinance.entity.AssetType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Data Transfer Object for asset responses.
 * 
 * <p>This DTO is returned to clients when retrieving asset information.
 * It contains decrypted values, excludes sensitive internal fields, and
 * includes calculated fields for portfolio analysis.</p>
 * 
 * <p>Requirement REQ-2.6.1: Users can view their asset information</p>
 * <p>Requirement REQ-2.6.3: Display portfolio value and gains/losses</p>
 * 
 * @see org.openfinance.entity.Asset
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetResponse {
    
    /**
     * Unique identifier of the asset.
     */
    private Long id;
    
    /**
     * ID of the user who owns this asset.
     */
    private Long userId;
    
    /**
     * Optional account ID this asset belongs to.
     */
    private Long accountId;
    
    /**
     * Optional account name (denormalized for convenience).
     * Populated when asset is linked to an account.
     */
    private String accountName;
    
    /**
     * Name of the asset (decrypted).
     * 
     * <p>Requirement REQ-2.6.2: Display asset name</p>
     */
    private String name;
    
    /**
     * Type of asset (STOCK, ETF, MUTUAL_FUND, BOND, CRYPTO, COMMODITY, REAL_ESTATE, OTHER).
     * 
     * <p>Requirement REQ-2.6: Asset type categorization</p>
     */
    private AssetType type;
    
    /**
     * Ticker symbol or identifier (e.g., "AAPL", "BTC-USD", "SPY").
     */
    private String symbol;
    
    /**
     * Quantity or number of units owned.
     * 
     * <p>Requirement REQ-2.6.2: Display quantity</p>
     */
    private BigDecimal quantity;
    
    /**
     * Purchase price per unit.
     * 
     * <p>Requirement REQ-2.6.2: Display purchase price</p>
     */
    private BigDecimal purchasePrice;
    
    /**
     * Current market price per unit.
     * 
     * <p>Requirement REQ-2.6.4: Display current price for portfolio valuation</p>
     */
    private BigDecimal currentPrice;
    
    /**
     * Currency code in ISO 4217 format.
     * 
     * <p>Requirement REQ-2.8: Multi-currency support</p>
     */
    private String currency;
    
    /**
     * Date when the asset was purchased.
     */
    private LocalDate purchaseDate;
    
    /**
     * Optional notes about the asset (decrypted).
     */
    private String notes;
    
    /**
     * Timestamp when the current price was last updated.
     * 
     * <p>Requirement REQ-2.6.5: Track when prices were last updated</p>
     */
    private LocalDateTime lastUpdated;
    
    /**
     * Timestamp when the asset record was created.
     */
    private LocalDateTime createdAt;
    
    /**
     * Timestamp when the asset record was last modified.
     */
    private LocalDateTime updatedAt;
    
    // === Calculated Fields ===
    
    /**
     * Total current value of the asset (quantity * currentPrice).
     * 
     * <p><strong>Calculated field</strong> - computed from quantity and currentPrice.</p>
     * 
     * <p>Requirement REQ-2.6.3: Display total asset value</p>
     */
    private BigDecimal totalValue;
    
    /**
     * Total cost basis of the asset (quantity * purchasePrice).
     * 
     * <p><strong>Calculated field</strong> - computed from quantity and purchasePrice.</p>
     * 
     * <p>Used for gain/loss calculations.</p>
     */
    private BigDecimal totalCost;
    
    /**
     * Unrealized gain or loss amount ((currentPrice - purchasePrice) * quantity).
     * 
     * <p><strong>Calculated field</strong> - computed from price difference and quantity.</p>
     * <p>Positive value indicates gain, negative indicates loss.</p>
     * 
     * <p>Requirement REQ-2.6.3: Display unrealized gains/losses</p>
     */
    private BigDecimal unrealizedGain;
    
    /**
     * Unrealized gain or loss percentage ((currentPrice - purchasePrice) / purchasePrice).
     * 
     * <p><strong>Calculated field</strong> - expressed as decimal (0.15 = 15% gain, -0.10 = 10% loss).</p>
     * 
     * <p>Requirement REQ-2.6.3: Display gain/loss percentage</p>
     */
    private BigDecimal gainPercentage;
    
    /**
     * Number of days the asset has been held (from purchase date to current date).
     * 
     * <p><strong>Calculated field</strong> - useful for tax implications and holding period analysis.</p>
     */
    private Long holdingDays;
    
    // ===== Physical Asset Fields =====
    
    /**
     * Serial number or identification number for physical assets (decrypted).
     * 
     * <p>Examples: VIN for vehicles, serial number for electronics, certificate number for jewelry.</p>
     */
    private String serialNumber;
    
    /**
     * Brand or manufacturer name for physical assets (decrypted).
     * 
     * <p>Examples: Tesla, Apple, Rolex, IKEA.</p>
     */
    private String brand;
    
    /**
     * Model name or number for physical assets (decrypted).
     * 
     * <p>Examples: Model 3, iPhone 15 Pro, Submariner, MALM.</p>
     */
    private String model;
    
    /**
     * Physical condition of the asset.
     * 
     * <p>Valid values: NEW, EXCELLENT, GOOD, FAIR, POOR</p>
     */
    private AssetCondition condition;
    
    /**
     * Warranty expiration date for physical assets.
     */
    private LocalDate warrantyExpiration;
    
    /**
     * Expected useful life in years (for depreciation calculation).
     */
    private Integer usefulLifeYears;
    
    /**
     * Path to photo or image file for the asset.
     */
    private String photoPath;
    
    /**
     * Depreciated value of the asset (for physical assets with useful life).
     * 
     * <p><strong>Calculated field</strong> - computed using straight-line depreciation.</p>
     */
    private BigDecimal depreciatedValue;
    
    /**
     * Condition-adjusted value (depreciated value * condition retention factor).
     * 
     * <p><strong>Calculated field</strong> - applies condition multiplier to depreciated value.</p>
     */
    private BigDecimal conditionAdjustedValue;
    
    /**
     * Whether this asset is a physical asset (not financial).
     * 
     * <p><strong>Calculated field</strong> - true for VEHICLE, JEWELRY, COLLECTIBLE, ELECTRONICS, FURNITURE.</p>
     */
    private Boolean isPhysical;
    
    /**
     * Whether the warranty is still valid.
     * 
     * <p><strong>Calculated field</strong> - true if warranty expiration date is in the future.</p>
     */
    private Boolean isWarrantyValid;

    // === Currency Conversion Fields (Requirement REQ-2.2) ===

    /**
     * Total asset value ({@code totalValue}) converted to the user's base currency.
     *
     * <p>Populated only when the asset currency differs from the user's base currency
     * and a valid exchange rate is available. Falls back to {@code totalValue} otherwise.</p>
     *
     * <p>Requirement REQ-2.2: Conversion metadata for base-currency display</p>
     */
    private BigDecimal valueInBaseCurrency;

    /**
     * The user's base currency (ISO 4217) at the time this response was built.
     *
     * <p>Requirement REQ-2.2: Base currency reference</p>
     */
    private String baseCurrency;

    /**
     * Exchange rate used to convert {@code totalValue} to {@code valueInBaseCurrency}.
     *
     * <p>Represents: 1 unit of {@code currency} = {@code exchangeRate} units of
     * {@code baseCurrency}. Null when no conversion was performed.</p>
     *
     * <p>Requirement REQ-2.6: Exchange rate used for conversion</p>
     */
    private BigDecimal exchangeRate;

    /**
     * Whether the asset value has been converted from a foreign currency to the base currency.
     *
     * <p>{@code true} only when {@code currency != baseCurrency} AND conversion succeeded.</p>
     *
     * <p>Requirement REQ-3.6: isConverted flag semantics</p>
     */
    private Boolean isConverted;

    // === Secondary Currency Conversion Fields (Requirement REQ-3.2, REQ-3.5) ===

    /**
     * Total asset value converted to the user's optional secondary currency.
     *
     * <p>Populated only when the user has a secondary currency configured AND the asset
     * currency differs from the secondary currency AND a valid exchange rate is available.
     * Null otherwise.</p>
     *
     * <p>Requirement REQ-3.2, REQ-3.5: Secondary conversion metadata</p>
     */
    private BigDecimal valueInSecondaryCurrency;

    /**
     * The user's secondary currency (ISO 4217) at the time this response was built.
     * Echoed from user settings. Null when no secondary currency is configured.
     *
     * <p>Requirement REQ-3.2: Secondary currency reference</p>
     */
    private String secondaryCurrency;

    /**
     * Exchange rate used to convert {@code totalValue} to {@code valueInSecondaryCurrency}.
     *
     * <p>Represents: 1 unit of {@code currency} = {@code secondaryExchangeRate} units of
     * {@code secondaryCurrency}. Null when no secondary conversion was performed.</p>
     *
     * <p>Requirement REQ-3.7: Secondary exchange rate</p>
     */
    private BigDecimal secondaryExchangeRate;
}
