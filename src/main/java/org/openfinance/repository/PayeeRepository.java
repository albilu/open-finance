package org.openfinance.repository;

import org.openfinance.entity.Category;
import org.openfinance.entity.Payee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Payee entity operations.
 * 
 * <p>Requirements: Payee Management Feature</p>
 */
@Repository
public interface PayeeRepository extends JpaRepository<Payee, Long> {
    
    /**
     * Find all payees ordered by name.
     */
    List<Payee> findAllByOrderByNameAsc();
    
    /**
     * Find all active payees ordered by name.
     */
    List<Payee> findByIsActiveTrueOrderByNameAsc();
    
    /**
     * Find system payees (default merchants/providers).
     */
    List<Payee> findByIsSystemTrueOrderByNameAsc();
    
    /**
     * Find custom (user-created) payees.
     */
    List<Payee> findByIsSystemFalseOrderByNameAsc();
    
    /**
     * Find payees by default category.
     */
    List<Payee> findByDefaultCategoryOrderByNameAsc(Category defaultCategory);
    
    /**
     * Find payees by default category and active status.
     */
    List<Payee> findByDefaultCategoryAndIsActiveTrueOrderByNameAsc(Category defaultCategory);
    
    /**
     * Find payees by default category ID.
     */
    @Query("SELECT p FROM Payee p WHERE p.defaultCategory.id = :categoryId ORDER BY p.name")
    List<Payee> findByDefaultCategoryId(@Param("categoryId") Long categoryId);
    
    /**
     * Find payees by default category ID and active status.
     */
    @Query("SELECT p FROM Payee p WHERE p.defaultCategory.id = :categoryId AND p.isActive = true ORDER BY p.name")
    List<Payee> findByDefaultCategoryIdAndIsActiveTrue(@Param("categoryId") Long categoryId);
    
    /**
     * Search payees by name (case-insensitive contains).
     */
    @Query("SELECT p FROM Payee p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%')) AND p.isActive = true ORDER BY p.name")
    List<Payee> searchByName(@Param("name") String name);
    
    /**
     * Search payees by name including inactive (for management).
     */
    @Query("SELECT p FROM Payee p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%')) ORDER BY p.name")
    List<Payee> searchAllByName(@Param("name") String name);
    
    /**
     * Find payee by exact name (case-insensitive).
     */
    Payee findByNameIgnoreCase(String name);
    
    /**
     * Check if payee exists by name.
     */
    boolean existsByNameIgnoreCase(String name);
    
    /**
     * Find distinct default category names from payees.
     */
    @Query("SELECT DISTINCT p.defaultCategory.name FROM Payee p WHERE p.defaultCategory IS NOT NULL ORDER BY p.defaultCategory.name")
    List<String> findDistinctCategoryNames();
    
    /**
     * Find distinct default categories from payees.
     */
    @Query("SELECT DISTINCT p.defaultCategory FROM Payee p WHERE p.defaultCategory IS NOT NULL ORDER BY p.defaultCategory.name")
    List<Category> findDistinctCategories();
    
    /**
     * Find payees that are system default and inactive (hidden by user).
     */
    List<Payee> findByIsSystemTrueAndIsActiveFalseOrderByNameAsc();
    
    /**
     * Find active payees (both system and custom).
     */
    @Query("SELECT p FROM Payee p WHERE p.isActive = true ORDER BY p.isSystem DESC, p.name")
    List<Payee> findAllActiveOrderBySystemFirst();
}
