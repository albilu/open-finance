package org.openfinance.repository;

import java.util.List;
import org.openfinance.entity.LiabilityPrincipalAllocation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LiabilityPrincipalAllocationRepository
        extends JpaRepository<LiabilityPrincipalAllocation, Long> {
    List<LiabilityPrincipalAllocation> findByLiabilityIdAndUserId(Long liabilityId, Long userId);

    List<LiabilityPrincipalAllocation> findByTrancheIdAndUserId(Long trancheId, Long userId);

    List<LiabilityPrincipalAllocation> findByTransactionIdAndUserId(
            Long transactionId, Long userId);

    void deleteByTransactionIdAndUserId(Long transactionId, Long userId);
}
