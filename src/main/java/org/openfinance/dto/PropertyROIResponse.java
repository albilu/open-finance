package org.openfinance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Data Transfer Object for property Return on Investment (ROI) calculation response.
 * 
 * <p>This DTO provides comprehensive ROI analysis for a real estate property,
 * including appreciation, rental income, and total return metrics.</p>
 * 
 * <p>Requirement REQ-2.16.2: Calculate and display property ROI</p>
 * 
 * @see org.openfinance.entity.RealEstateProperty
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PropertyROIResponse {
    
    /**
     * Property ID.
     */
    private Long propertyId;
    
    /**
     * Property name (decrypted).
     */
    private String propertyName;
    
    /**
     * Original purchase price (decrypted).
     * 
     * <p>Requirement REQ-2.16.2: Purchase price for ROI calculation</p>
     */
    private BigDecimal purchasePrice;
    
    /**
     * Current market value (decrypted).
     * 
     * <p>Requirement REQ-2.16.2: Current value for ROI calculation</p>
     */
    private BigDecimal currentValue;
    
    /**
     * Purchase date.
     */
    private LocalDate purchaseDate;
    
    /**
     * Years since purchase (calculated from purchaseDate to current date).
     * 
     * <p><strong>Calculated field</strong> - used for annualized return calculations.</p>
     */
    private Long yearsOwned;
    
    /**
     * Total appreciation (currentValue - purchasePrice).
     * 
     * <p><strong>Calculated field</strong> - capital gain/loss.</p>
     * 
     * <p>Positive values indicate appreciation, negative indicate depreciation.</p>
     * 
     * <p>Requirement REQ-2.16.2: Appreciation calculation</p>
     */
    private BigDecimal appreciation;
    
    /**
     * Appreciation percentage ((appreciation / purchasePrice) * 100).
     * 
     * <p><strong>Calculated field</strong> - rate of appreciation.</p>
     * 
     * <p>Example: 25.5 represents 25.5% appreciation since purchase</p>
     */
    private BigDecimal appreciationPercentage;
    
    /**
     * Monthly rental income (decrypted, if applicable).
     * 
     * <p>Null for non-rental properties.</p>
     * 
     * <p>Requirement REQ-2.16.2: Rental income tracking</p>
     */
    private BigDecimal monthlyRentalIncome;
    
    /**
     * Total rental income collected over ownership period (monthlyRentalIncome * months owned).
     * 
     * <p><strong>Calculated field</strong> - cumulative rental income.</p>
     * 
     * <p>Null for non-rental properties or if rental income is not specified.</p>
     * 
     * <p>Requirement REQ-2.16.2: Total income from property</p>
     */
    private BigDecimal totalRentalIncome;
    
    /**
     * Annual rental yield ((monthlyRentalIncome * 12 / currentValue) * 100).
     * 
     * <p><strong>Calculated field</strong> - annualized return from rental income.</p>
     * 
     * <p>Example: 5.5 represents 5.5% annual yield from rent</p>
     * 
     * <p>Null for non-rental properties.</p>
     */
    private BigDecimal rentalYield;
    
    /**
     * Total return on investment percentage.
     * 
     * <p><strong>Calculated field</strong> - overall ROI including appreciation and rental income.</p>
     * 
     * <p>Formula: ((currentValue - purchasePrice + totalRentalIncome) / purchasePrice) * 100</p>
     * 
     * <p>Example: 45.0 represents 45% total return on investment</p>
     * 
     * <p>Requirement REQ-2.16.2: Total ROI calculation</p>
     */
    private BigDecimal totalROI;
    
    /**
     * Annualized return percentage (totalROI / yearsOwned).
     * 
     * <p><strong>Calculated field</strong> - average annual return.</p>
     * 
     * <p>Example: 8.5 represents 8.5% average annual return</p>
     * 
     * <p>Null if yearsOwned is 0 (property purchased within current year).</p>
     */
    private BigDecimal annualizedReturn;
    
    /**
     * Currency code in ISO 4217 format.
     */
    private String currency;
    
    /**
     * Whether this property generates rental income.
     * 
     * <p>True if property has rental income > 0, false otherwise.</p>
     * 
     * <p>Requirement REQ-2.16.2: Identify rental properties for ROI analysis</p>
     */
    @com.fasterxml.jackson.annotation.JsonProperty("isRentalProperty")
    private boolean isRentalProperty;
}
