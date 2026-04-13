package org.openfinance.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Response DTO for global search operations.
 * 
 * <p>Contains search results grouped by type with metadata about the search operation.</p>
 * 
 * <p>Task: TASK-12.4.2</p>
 * <p>Requirement: REQ-2.3.5 - Global search response with grouped results</p>
 * 
 * @see org.openfinance.service.SearchService
 */
@Data
@Builder
public class GlobalSearchResponse {
    
    /**
     * Search query that was executed
     */
    private String query;
    
    /**
     * Total number of results across all types
     */
    private Integer totalResults;
    
    /**
     * Results grouped by type (TRANSACTION, ACCOUNT, ASSET, etc.)
     */
    private Map<String, List<SearchResultDto>> resultsByType;
    
    /**
     * Number of results per type
     */
    private Map<String, Integer> countsPerType;
    
    /**
     * Time taken to execute search (in milliseconds)
     */
    private Long executionTimeMs;
    
    /**
     * Flag indicating if results were truncated due to limit
     */
    private Boolean hasMore;
    
    /**
     * Maximum number of results returned
     */
    private Integer limit;
}
