package org.openfinance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.openfinance.entity.AssetType;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Data Transfer Object for asset search criteria.
 * 
 * <p>This DTO is used to build dynamic queries for searching assets.
 * All fields are optional - if a field is null, it won't be included in the search filter.
 * Multiple criteria can be combined (AND logic).</p>
 * 
 * <p><strong>Supported Search Filters:</strong>
 * <ul>
 *   <li><strong>keyword</strong> - Search in asset name (case-insensitive, partial match)</li>
 *   <li><strong>type</strong> - Filter by asset type (STOCK, ETF, CRYPTO, etc.)</li>
 *   <li><strong>accountId</strong> - Filter by account ID</li>
 *   <li><strong>currency</strong> - Filter by currency code (USD, EUR, etc.)</li>
 *   <li><strong>symbol</strong> - Filter by ticker symbol (case-insensitive, partial match)</li>
 *   <li><strong>purchaseDateFrom</strong> - Filter by purchase date >= this date</li>
 *   <li><strong>purchaseDateTo</strong> - Filter by purchase date <= this date</li>
 *   <li><strong>valueMin</strong> - Filter assets with total value >= this value</li>
 *   <li><strong>valueMax</strong> - Filter assets with total value <= this value</li>
 * </ul>
 * 
 * @see org.openfinance.entity.Asset
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetSearchCriteria {
    
    /**
     * Keyword to search in asset name (case-insensitive).
     * 
     * <p>Uses LIKE query with wildcards: %keyword%</p>
     * 
     * <p>Example: "apple" will match "Apple Inc.", "Apple Watch", etc.</p>
     */
    private String keyword;
    
    /**
     * Filter by asset type.
     * 
     * <p>If null, includes all asset types.</p>
     */
    private AssetType type;
    
    /**
     * Filter by account ID.
     * 
     * <p>If null, includes assets from all accounts.</p>
     */
    private Long accountId;
    
    /**
     * Filter by currency code.
     * 
     * <p>If null, includes assets in all currencies.</p>
     * 
     * <p>Example: "USD", "EUR", "GBP"</p>
     */
    private String currency;
    
    /**
     * Filter by ticker symbol (case-insensitive).
     * 
     * <p>Uses LIKE query with wildcards: %symbol%</p>
     * 
     * <p>Example: "aap" will match "AAPL", "AAP"</p>
     */
    private String symbol;
    
    /**
     * Filter assets purchased on or after this date.
     * 
     * <p>If null, no lower date bound.</p>
     */
    private LocalDate purchaseDateFrom;
    
    /**
     * Filter assets purchased on or before this date.
     * 
     * <p>If null, no upper date bound.</p>
     */
    private LocalDate purchaseDateTo;
    
    /**
     * Filter assets with total value (quantity * currentPrice) greater than or equal to this value.
     * 
     * <p>If null, no lower value bound.</p>
     */
    private BigDecimal valueMin;
    
    /**
     * Filter assets with total value (quantity * currentPrice) less than or equal to this value.
     * 
     * <p>If null, no upper value bound.</p>
     */
    private BigDecimal valueMax;
    
    /**
     * Checks if any search criteria is provided.
     * 
     * @return true if at least one filter is set, false if all fields are null
     */
    public boolean hasAnyCriteria() {
        return keyword != null
            || type != null
            || accountId != null
            || currency != null
            || symbol != null
            || purchaseDateFrom != null
            || purchaseDateTo != null
            || valueMin != null
            || valueMax != null;
    }
}
