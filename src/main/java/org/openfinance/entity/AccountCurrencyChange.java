package org.openfinance.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.openfinance.converter.EncryptedBigDecimalConverter;
import org.openfinance.util.LocalDateConverter;

@Entity
@Table(name = "account_currency_changes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountCurrencyChange {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "effective_date", nullable = false)
    @Convert(converter = LocalDateConverter.class)
    private LocalDate effectiveDate;

    @Column(name = "from_currency", nullable = false)
    private String fromCurrency;

    @Column(name = "to_currency", nullable = false)
    private String toCurrency;

    @Column(name = "rate", nullable = false)
    @Convert(converter = EncryptedBigDecimalConverter.class)
    private BigDecimal rate;
}
