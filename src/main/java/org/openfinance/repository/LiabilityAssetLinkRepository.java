package org.openfinance.repository;

import java.util.List;
import org.openfinance.entity.LiabilityAssetLink;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LiabilityAssetLinkRepository extends JpaRepository<LiabilityAssetLink, Long> {
    List<LiabilityAssetLink> findByLiabilityIdAndUserId(Long liabilityId, Long userId);

    List<LiabilityAssetLink> findByAssetIdAndUserId(Long assetId, Long userId);

    void deleteByLiabilityIdAndUserId(Long liabilityId, Long userId);
}
