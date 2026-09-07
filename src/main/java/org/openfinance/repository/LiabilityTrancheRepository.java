package org.openfinance.repository;

import java.util.List;
import java.util.Optional;
import org.openfinance.entity.LiabilityTranche;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Repository for LiabilityTranche entity. */
@Repository
public interface LiabilityTrancheRepository extends JpaRepository<LiabilityTranche, Long> {

    /** Find all tranches of a liability for a specific user. */
    List<LiabilityTranche> findByLiabilityIdAndUserId(Long liabilityId, Long userId);

    /** Find a tranche by ID and user ID (for authorization). */
    Optional<LiabilityTranche> findByIdAndUserId(Long id, Long userId);

    /**
     * Check whether a tranche number already exists for a liability and user.
     *
     * <p>The {@code userId} parameter is an ownership guard (scopes the check to the caller's
     * rows). Uniqueness itself is per-liability, enforced by {@code UNIQUE(liability_id,
     * tranche_no)}.
     */
    boolean existsByLiabilityIdAndUserIdAndTrancheNo(
            Long liabilityId, Long userId, Integer trancheNo);
}
