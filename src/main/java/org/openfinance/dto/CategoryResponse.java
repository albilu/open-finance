package org.openfinance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.openfinance.entity.CategoryType;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for category responses.
 * 
 * <p>This DTO is returned to clients when retrieving category information.
 * It contains decrypted values and includes parent/subcategory information.</p>
 * 
 * <p>Requirement REQ-2.4.1: Users can view their categories</p>
 * 
 * @see org.openfinance.entity.Category
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryResponse {
    
    /**
     * Unique identifier of the category.
     */
    private Long id;
    
    /**
     * Name of the category (decrypted).
     * 
     * <p>Requirement REQ-2.4.1: Display category name</p>
     */
    private String name;
    
    /**
     * Type of category: INCOME or EXPENSE.
     * 
     * <p>Requirement REQ-2.4.1: Display category type</p>
     */
    private CategoryType type;
    
    /**
     * ID of the parent category (null for root categories).
     * 
     * <p>Requirement REQ-2.4.1: Support hierarchical structure</p>
     */
    private Long parentId;
    
    /**
     * Name of the parent category (decrypted, null for root categories).
     * 
     * <p>Included for convenient display without additional lookups.</p>
     */
    private String parentName;
    
    /**
     * Icon identifier for UI display (optional).
     */
    private String icon;
    
    /**
     * Color code for UI display (optional).
     */
    private String color;
    
    /**
     * Flag indicating whether this is a system-provided category.
     * 
     * <p>System categories (created during user registration) cannot be deleted,
     * but users can create their own custom categories.</p>
     * 
     * <p>Requirement REQ-2.4.1: Distinguish between default and user-created categories</p>
     */
    private Boolean isSystem;
    
    /**
     * Number of direct subcategories under this category.
     * 
     * <p>Used for UI indicators (e.g., expand/collapse icons).</p>
     */
    private Integer subcategoryCount;
    
    /**
     * Timestamp when the category was created.
     */
    private LocalDateTime createdAt;
    
    /**
     * Timestamp when the category was last updated.
     */
    private LocalDateTime updatedAt;
}
