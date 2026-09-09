package org.openfinance.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.openfinance.entity.Account;
import org.openfinance.entity.Transaction;

public interface AccountCurrencyService {
    void change(Account account, String previousCurrency, Long userId, LocalDate effectiveDate);

    void book(Transaction transaction, Long userId);

    Position historicalPosition(
            Account account, BigDecimal currentUnits, LocalDate date, Long userId);

    record Position(BigDecimal amount, String currency) {}
}
