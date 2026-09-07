package org.openfinance.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openfinance.entity.Liability;
import org.openfinance.entity.LiabilityTranche;
import org.openfinance.entity.LiabilityType;
import org.openfinance.entity.PropertyType;
import org.openfinance.entity.RealEstateProperty;
import org.openfinance.entity.TrancheStatus;
import org.openfinance.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

/** Slice tests for LiabilityTrancheRepository covering CRUD, round-trip and user isolation. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@DisplayName("LiabilityTrancheRepository Tests")
class LiabilityTrancheRepositoryTest {

    @Autowired private TestEntityManager entityManager;

    @Autowired private LiabilityTrancheRepository liabilityTrancheRepository;

    private User testUser;
    private Liability testLiability;
    private RealEstateProperty testProperty;

    @BeforeEach
    void setUp() {
        testUser =
                User.builder()
                        .username("trancheuser")
                        .email("tranche@example.com")
                        .passwordHash("hash")
                        .masterPasswordSalt("salt")
                        .baseCurrency("USD")
                        .build();
        entityManager.persist(testUser);

        testLiability = new Liability();
        testLiability.setUser(testUser);
        testLiability.setUserId(testUser.getId());
        testLiability.setName("Primary Mortgage");
        testLiability.setType(LiabilityType.MORTGAGE);
        testLiability.setPrincipal("encrypted_300000");
        testLiability.setCurrentBalance("encrypted_250000");
        testLiability.setStartDate(LocalDate.of(2020, 1, 1));
        testLiability.setCurrency("USD");
        entityManager.persist(testLiability);

        testProperty =
                RealEstateProperty.builder()
                        .userId(testUser.getId())
                        .name("encrypted-MainResidence")
                        .address("encrypted-123MainSt")
                        .propertyType(PropertyType.RESIDENTIAL)
                        .purchasePrice("encrypted-400000")
                        .purchaseDate(LocalDate.of(2020, 1, 15))
                        .currentValue("encrypted-500000")
                        .currency("USD")
                        .isActive(true)
                        .build();
        entityManager.persist(testProperty);

        entityManager.flush();
    }

    @Test
    @DisplayName("Should persist and round-trip a DRAWN tranche")
    void shouldPersistAndRoundTripDrawnTranche() {
        // Given
        LiabilityTranche tranche =
                LiabilityTranche.builder()
                        .userId(testUser.getId())
                        .liabilityId(testLiability.getId())
                        .trancheNo(1)
                        .plannedAmount(new BigDecimal("50000.00"))
                        .drawnAmount(new BigDecimal("50000.00"))
                        .plannedDate(LocalDate.of(2024, 1, 15))
                        .drawnDate(LocalDate.of(2024, 1, 20))
                        .status(TrancheStatus.DRAWN)
                        .realEstateId(testProperty.getId())
                        .currency("USD")
                        .build();

        // When
        LiabilityTranche saved = liabilityTrancheRepository.save(tranche);
        entityManager.flush();
        entityManager.clear();

        Optional<LiabilityTranche> found = liabilityTrancheRepository.findById(saved.getId());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getStatus()).isEqualTo(TrancheStatus.DRAWN);
        assertThat(found.get().getRealEstateId()).isEqualTo(testProperty.getId());
        assertThat(found.get().getPlannedAmount()).isEqualByComparingTo(new BigDecimal("50000.00"));
        assertThat(found.get().getDrawnAmount()).isEqualByComparingTo(new BigDecimal("50000.00"));
        assertThat(found.get().getLiabilityId()).isEqualTo(testLiability.getId());
        assertThat(found.get().getUserId()).isEqualTo(testUser.getId());
        assertThat(found.get().getTrancheNo()).isEqualTo(1);
        assertThat(found.get().getCurrency()).isEqualTo("USD");
    }

    @Test
    @DisplayName("Should find tranches by liability and user")
    void shouldFindByLiabilityIdAndUserId() {
        // Given
        liabilityTrancheRepository.save(
                LiabilityTranche.builder()
                        .userId(testUser.getId())
                        .liabilityId(testLiability.getId())
                        .trancheNo(1)
                        .plannedAmount(new BigDecimal("50000.00"))
                        .status(TrancheStatus.DRAWN)
                        .realEstateId(testProperty.getId())
                        .currency("USD")
                        .build());
        entityManager.flush();

        // When
        List<LiabilityTranche> tranches =
                liabilityTrancheRepository.findByLiabilityIdAndUserId(
                        testLiability.getId(), testUser.getId());

        // Then
        assertThat(tranches).hasSize(1);
        assertThat(tranches.get(0).getTrancheNo()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should check existence by liability, user and tranche number")
    void shouldCheckExistsByLiabilityIdAndUserIdAndTrancheNo() {
        // Given
        liabilityTrancheRepository.save(
                LiabilityTranche.builder()
                        .userId(testUser.getId())
                        .liabilityId(testLiability.getId())
                        .trancheNo(1)
                        .plannedAmount(new BigDecimal("50000.00"))
                        .currency("USD")
                        .build());
        entityManager.flush();

        // When & Then
        assertThat(
                        liabilityTrancheRepository.existsByLiabilityIdAndUserIdAndTrancheNo(
                                testLiability.getId(), testUser.getId(), 1))
                .isTrue();
        assertThat(
                        liabilityTrancheRepository.existsByLiabilityIdAndUserIdAndTrancheNo(
                                testLiability.getId(), testUser.getId(), 2))
                .isFalse();
    }

    @Test
    @DisplayName(
            "Should null realEstateId when the linked property is deleted (ON DELETE SET NULL)")
    void shouldNullRealEstateIdWhenPropertyDeleted() {
        // Given a tranche linked to a real persisted property.
        // Slice tests run against SQLite with foreign_keys=on (see application-test.yml),
        // so the DB-level ON DELETE SET NULL declared in V79 is enforced here. If this
        // ever runs on a store without FK enforcement, equivalent service-level nulling
        // must cover the same semantics.
        LiabilityTranche saved =
                liabilityTrancheRepository.save(
                        LiabilityTranche.builder()
                                .userId(testUser.getId())
                                .liabilityId(testLiability.getId())
                                .trancheNo(1)
                                .plannedAmount(new BigDecimal("50000.00"))
                                .status(TrancheStatus.DRAWN)
                                .realEstateId(testProperty.getId())
                                .currency("USD")
                                .build());
        entityManager.flush();

        // When the parent property is deleted
        RealEstateProperty managed =
                entityManager.find(RealEstateProperty.class, testProperty.getId());
        entityManager.remove(managed);
        entityManager.flush();
        entityManager.clear();

        // Then the tranche survives with a nulled property link
        Optional<LiabilityTranche> found = liabilityTrancheRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getRealEstateId()).isNull();
    }
}
