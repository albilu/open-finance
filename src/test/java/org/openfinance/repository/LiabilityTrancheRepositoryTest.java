package org.openfinance.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openfinance.entity.Account;
import org.openfinance.entity.AccountType;
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
import org.springframework.dao.DataAccessException;
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
    @DisplayName("Should return empty when tranche belongs to another user")
    void shouldReturnEmptyForOtherUsersTranche() {
        // Given a tranche owned by testUser
        LiabilityTranche saved =
                liabilityTrancheRepository.save(
                        LiabilityTranche.builder()
                                .userId(testUser.getId())
                                .liabilityId(testLiability.getId())
                                .trancheNo(1)
                                .plannedAmount(new BigDecimal("50000.00"))
                                .currency("USD")
                                .build());
        entityManager.flush();

        User otherUser =
                User.builder()
                        .username("othertrancheuser")
                        .email("othertranche@example.com")
                        .passwordHash("hash")
                        .masterPasswordSalt("salt")
                        .baseCurrency("USD")
                        .build();
        entityManager.persist(otherUser);
        entityManager.flush();

        // When queried as the other user, nothing is returned (ownership guard)
        Optional<LiabilityTranche> found =
                liabilityTrancheRepository.findByIdAndUserId(saved.getId(), otherUser.getId());

        // Then
        assertThat(found).isEmpty();
        assertThat(liabilityTrancheRepository.findByIdAndUserId(saved.getId(), testUser.getId()))
                .isPresent();
    }

    @Test
    @DisplayName("Should reject duplicate tranche number per liability")
    void shouldRejectDuplicateTrancheNo() {
        // Given one tranche with number 1 for the liability
        liabilityTrancheRepository.saveAndFlush(
                LiabilityTranche.builder()
                        .userId(testUser.getId())
                        .liabilityId(testLiability.getId())
                        .trancheNo(1)
                        .plannedAmount(new BigDecimal("50000.00"))
                        .currency("USD")
                        .build());

        // When a second tranche reuses the same (liability_id, tranche_no)
        LiabilityTranche duplicate =
                LiabilityTranche.builder()
                        .userId(testUser.getId())
                        .liabilityId(testLiability.getId())
                        .trancheNo(1)
                        .plannedAmount(new BigDecimal("10000.00"))
                        .currency("USD")
                        .build();

        // Then the DB-level UNIQUE(liability_id, tranche_no) rejects it.
        // Asserts on the common DataAccessException base class (rather than
        // DataIntegrityViolationException) since SQLite's driver translates a unique-constraint
        // violation into JpaSystemException here — same precedent as UserRepositoryTest.
        assertThrows(
                DataAccessException.class,
                () -> liabilityTrancheRepository.saveAndFlush(duplicate));
    }

    @Test
    @DisplayName(
            "Should null transactions.tranche_id when the linked tranche is deleted (ON DELETE SET NULL)")
    void shouldNullTransactionTrancheIdWhenTrancheDeleted() {
        // Given a tranche and a transaction row referencing it.
        // Transaction has no mapped entity field yet (Task 2), so the link row is inserted
        // via native SQL. Slice tests run against SQLite with foreign_keys=on (see
        // application-test.yml), so the DB-level ON DELETE SET NULL declared in
        // V80__add_movement_link_columns_to_transactions.sql is enforced here.
        LiabilityTranche saved =
                liabilityTrancheRepository.save(
                        LiabilityTranche.builder()
                                .userId(testUser.getId())
                                .liabilityId(testLiability.getId())
                                .trancheNo(1)
                                .plannedAmount(new BigDecimal("50000.00"))
                                .currency("USD")
                                .build());
        entityManager.flush();

        Account account =
                Account.builder()
                        .userId(testUser.getId())
                        .name("Tranche link checking")
                        .type(AccountType.CHECKING)
                        .currency("USD")
                        .balance(new BigDecimal("1000.00"))
                        .isActive(true)
                        .build();
        entityManager.persist(account);
        entityManager.flush();

        entityManager
                .getEntityManager()
                .createNativeQuery(
                        "INSERT INTO transactions (user_id, account_id, transaction_type,"
                                + " amount, currency, transaction_date, created_at, tranche_id)"
                                + " VALUES (:userId, :accountId, 'EXPENSE', 100.0, 'USD',"
                                + " '2024-02-01', '2024-02-01T10:00:00', :trancheId)")
                .setParameter("userId", testUser.getId())
                .setParameter("accountId", account.getId())
                .setParameter("trancheId", saved.getId())
                .executeUpdate();

        Long transactionId =
                ((Number)
                                entityManager
                                        .getEntityManager()
                                        .createNativeQuery(
                                                "SELECT id FROM transactions WHERE tranche_id ="
                                                        + " :trancheId")
                                        .setParameter("trancheId", saved.getId())
                                        .getSingleResult())
                        .longValue();

        // When the parent tranche is deleted
        LiabilityTranche managed = entityManager.find(LiabilityTranche.class, saved.getId());
        entityManager.remove(managed);
        entityManager.flush();
        entityManager.clear();

        // Then the transaction survives with a nulled tranche link
        Object trancheId =
                entityManager
                        .getEntityManager()
                        .createNativeQuery("SELECT tranche_id FROM transactions WHERE id = :id")
                        .setParameter("id", transactionId)
                        .getSingleResult();
        assertThat(trancheId).isNull();
    }

    @Test
    @DisplayName(
            "Should null realEstateId when the linked property is deleted (ON DELETE SET NULL)")
    void shouldNullRealEstateIdWhenPropertyDeleted() {
        // Given a tranche linked to a real persisted property.
        // Slice tests run against SQLite with foreign_keys=on (see application-test.yml),
        // so the DB-level ON DELETE SET NULL declared in
        // V79__create_liability_tranches_table.sql is enforced here. If this
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
