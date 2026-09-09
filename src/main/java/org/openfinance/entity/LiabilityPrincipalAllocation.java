package org.openfinance.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.openfinance.converter.EncryptedBigDecimalConverter;

/** The exact principal applied by one payment, in the liability's currency. */
@Entity
@Table(name = "liability_principal_allocations")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiabilityPrincipalAllocation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "liability_id", nullable = false)
    private Long liabilityId;

    @Column(name = "transaction_id", nullable = false)
    private Long transactionId;

    /** Null denotes principal borrowed before tranche tracking began. */
    @Column(name = "tranche_id")
    private Long trancheId;

    @Convert(converter = EncryptedBigDecimalConverter.class)
    @Column(name = "amount", nullable = false, columnDefinition = "TEXT")
    private BigDecimal amount;
}
