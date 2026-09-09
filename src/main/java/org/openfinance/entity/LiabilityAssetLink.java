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

/** Financing allocates economic debt; collateral records security without another debt charge. */
@Entity
@Table(name = "liability_asset_links")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiabilityAssetLink {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "liability_id", nullable = false)
    private Long liabilityId;

    @Column(name = "asset_id", nullable = false)
    private Long assetId;

    @Column(name = "relationship", nullable = false)
    private String relationship;

    @Convert(converter = EncryptedBigDecimalConverter.class)
    @Column(name = "allocation_percentage", columnDefinition = "TEXT", nullable = false)
    private BigDecimal allocationPercentage;
}
