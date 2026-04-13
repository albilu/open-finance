package org.openfinance.specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.openfinance.dto.AccountSearchCriteria;
import org.openfinance.entity.Account;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Predicate;

/**
 * JPA Specification builder for dynamic account queries.
 * 
 * <p>
 * This class creates type-safe JPA Criteria queries based on search criteria.
 * It builds dynamic WHERE clauses by combining multiple predicates with AND
 * logic.
 * </p>
 * 
 * <p>
 * <strong>Note on Encrypted Fields:</strong> The keyword search operates on
 * encrypted
 * name field, so it can only match exact encrypted values. For proper keyword
 * search,
 * the service layer should decrypt fields before comparison (not ideal for
 * large datasets).
 * This implementation provides basic database-level filtering; full-text search
 * would
 * require decryption in application layer or use of searchable encryption.
 * </p>
 * 
 * @see AccountSearchCriteria
 * @see org.openfinance.entity.Account
 */
public class AccountSpecification {

    /**
     * Builds a JPA Specification from search criteria.
     * 
     * <p>
     * Combines all non-null criteria with AND logic. If criteria has no filters,
     * returns a specification that matches all accounts for the given user.
     * </p>
     * 
     * <p>
     * <strong>Generated Predicates:</strong>
     * <ul>
     * <li>userId = ? (always included for security)</li>
     * <li>keyword - LIKE search on name (if provided)</li>
     * <li>type = ? (if provided)</li>
     * <li>currency = ? (if provided)</li>
     * <li>isActive = ? (if provided)</li>
     * <li>balance >= ? (if balanceMin provided)</li>
     * <li>balance <= ? (if balanceMax provided)</li>
     * <li>institution.name LIKE ? (if provided)</li>
     * <li>balance < 1000 (if lowBalance = true)</li>
     * </ul>
     * 
     * @param userId   the user ID (required for security)
     * @param criteria the search criteria
     * @return JPA Specification for dynamic query building
     */
    public static Specification<Account> buildSpecification(Long userId, AccountSearchCriteria criteria) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Always filter by userId (security)
            predicates.add(criteriaBuilder.equal(root.get("userId"), userId));

            // Keyword search on account name is intentionally omitted here because
            // the name field is encrypted.
            // A LIKE search on ciphertext will never match the plaintext keyword.
            // This is handled in-memory in AccountService.searchAccounts.

            // Filter by account type
            if (criteria.getType() != null) {
                predicates.add(criteriaBuilder.equal(root.get("type"), criteria.getType()));
            }

            // Filter by currency
            if (criteria.getCurrency() != null && !criteria.getCurrency().trim().isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("currency"), criteria.getCurrency()));
            }

            // Filter by active status
            if (criteria.getIsActive() != null) {
                predicates.add(criteriaBuilder.equal(root.get("isActive"), criteria.getIsActive()));
            }

            // Filter by minimum balance
            if (criteria.getBalanceMin() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("balance"), criteria.getBalanceMin()));
            }

            // Filter by maximum balance
            if (criteria.getBalanceMax() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("balance"), criteria.getBalanceMax()));
            }

            // Filter by institution name (join with institution)
            if (criteria.getInstitution() != null && !criteria.getInstitution().trim().isEmpty()) {
                String institution = "%" + criteria.getInstitution().toLowerCase() + "%";
                // Join with institution and filter by name
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("institution").get("name")), institution));
            }

            // Filter by low balance — matches the threshold used in NotificationService (<
            // 1000)
            if (Boolean.TRUE.equals(criteria.getLowBalance())) {
                predicates.add(criteriaBuilder.lessThan(
                        root.get("balance"), new BigDecimal("1000")));
            }

            // Combine all predicates with AND logic
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
