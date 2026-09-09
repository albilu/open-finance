package org.openfinance.repository;

import java.util.List;
import org.openfinance.entity.AccountCurrencyChange;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountCurrencyChangeRepository
        extends JpaRepository<AccountCurrencyChange, Long> {
    List<AccountCurrencyChange> findByAccountIdAndUserIdOrderByEffectiveDateAscIdAsc(
            Long accountId, Long userId);
}
