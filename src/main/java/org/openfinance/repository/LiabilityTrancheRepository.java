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

    /** Check whether a tranche number already exists for a liability and user. */
    boolean existsByLiabilityIdAndUserIdAndTrancheNo(
            Long liabilityId, Long userId, Integer trancheNo);
}
