package org.openfinance.mapper;

import org.mapstruct.*;
import org.openfinance.dto.BudgetRequest;
import org.openfinance.dto.BudgetResponse;
import org.openfinance.entity.Budget;

/**
 * MapStruct mapper for converting between Budget entity and DTOs.
 * 
 * <p>This mapper handles bidirectional mapping between:
 * <ul>
 *   <li>{@link BudgetRequest} DTO → {@link Budget} entity (for create/update)</li>
 *   <li>{@link Budget} entity → {@link BudgetResponse} DTO (for read)</li>
 * </ul>
 * 
 * <p><strong>Note:</strong> The following fields are handled in the service layer, not in this mapper:
 * <ul>
 *   <li>Encryption/decryption of the amount field</li>
 *   <li>Setting categoryName and categoryType (denormalized from Category entity)</li>
 *   <li>Setting userId from authenticated user</li>
 * </ul>
 * 
 * <p>Requirement REQ-2.9.1.1: Budget creation - DTO conversions for CRUD operations</p>
 * 
 * @see org.openfinance.entity.Budget
 * @see org.openfinance.dto.BudgetRequest
 * @see org.openfinance.dto.BudgetResponse
 * @author Open-Finance Development Team
 * @version 1.0
 * @since 2026-02-02
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface BudgetMapper {
    
    /**
     * Converts a BudgetRequest DTO to a Budget entity.
     * 
     * <p>Used when creating a new budget. The following fields must be set separately
     * by the service layer:
     * <ul>
     *   <li>userId - from authenticated user context</li>
     *   <li>amount - encrypted before persisting</li>
     * </ul>
     * 
     * <p>The categoryId, period, startDate, endDate, rollover, notes, and currency
     * are mapped directly from the request.</p>
     * 
     * @param request the budget creation request
     * @return a new Budget entity (without id, userId, timestamps)
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Budget toEntity(BudgetRequest request);
    
    /**
     * Updates an existing Budget entity from a BudgetRequest DTO.
     * 
     * <p>This method updates only the fields that are present in the request,
     * preserving the existing values of id, userId, and timestamps.</p>
     * 
     * <p>The amount field must be encrypted by the service layer before calling
     * this method.</p>
     * 
     * <p>Null values in the request are ignored (existing entity values are preserved).</p>
     * 
     * @param request the budget update request
     * @param budget the existing budget entity to update
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromRequest(BudgetRequest request, @MappingTarget Budget budget);
    
    /**
     * Converts a Budget entity to a BudgetResponse DTO.
     * 
     * <p>Used when returning budget information to clients. The following fields
     * are handled by the service layer after calling this method:
     * <ul>
     *   <li>amount - must be decrypted</li>
     *   <li>categoryName - populated from Category entity lookup</li>
     *   <li>categoryType - populated from Category entity lookup</li>
     * </ul>
     * 
     * <p>This method maps the basic fields (id, categoryId, period, dates, rollover, etc.)
     * directly from the entity.</p>
     * 
     * @param budget the budget entity
     * @return the budget response DTO (amount still encrypted, categoryName/Type null)
     */
    @Mapping(target = "amount", ignore = true)
    @Mapping(target = "categoryName", ignore = true)
    @Mapping(target = "categoryType", ignore = true)
    BudgetResponse toResponse(Budget budget);
}
