package org.openfinance.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openfinance.entity.MovementType;
import org.openfinance.entity.TransactionType;

@DisplayName("Movement coherence validation tests")
class MovementCoherenceTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void initValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    private static DisbursementRequest disbursement(Long trancheId, Long directRealEstateId) {
        return DisbursementRequest.builder()
                .toAccountId(1L)
                .trancheId(trancheId)
                .directRealEstateId(directRealEstateId)
                .amount(new BigDecimal("100.00"))
                .date(LocalDate.of(2026, 1, 15))
                .build();
    }

    private static TransactionRequest transaction(
            TransactionType type,
            MovementType movementType,
            Long liabilityId,
            Long realEstateId,
            Long assetId) {
        return TransactionRequest.builder()
                .accountId(1L)
                .type(type)
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .date(LocalDate.of(2026, 1, 15))
                .movementType(movementType)
                .liabilityId(liabilityId)
                .realEstateId(realEstateId)
                .assetId(assetId)
                .build();
    }

    @Test
    @DisplayName("Direct disbursement with toAccountId and directRealEstateId passes")
    void directDisbursementPasses() {
        Set<ConstraintViolation<DisbursementRequest>> violations =
                validator.validate(disbursement(null, 5L));

        assertThat(violations)
                .filteredOn(v -> v.getPropertyPath().toString().equals("targetCoherent"))
                .isEmpty();
    }

    @Test
    @DisplayName("Disbursement with both trancheId and directRealEstateId fails")
    void bothTargetsFails() {
        Set<ConstraintViolation<DisbursementRequest>> violations =
                validator.validate(disbursement(3L, 5L));

        assertThat(violations)
                .filteredOn(v -> v.getPropertyPath().toString().equals("targetCoherent"))
                .hasSize(1);
    }

    @Test
    @DisplayName("Disbursement with neither target fails")
    void neitherTargetFails() {
        Set<ConstraintViolation<DisbursementRequest>> violations =
                validator.validate(disbursement(null, null));

        assertThat(violations)
                .filteredOn(v -> v.getPropertyPath().toString().equals("targetCoherent"))
                .hasSize(1);
    }

    @Test
    @DisplayName("DISBURSEMENT without liabilityId fails")
    void disbursementWithoutLiabilityFails() {
        Set<ConstraintViolation<TransactionRequest>> violations =
                validator.validate(
                        transaction(
                                TransactionType.EXPENSE,
                                MovementType.DISBURSEMENT,
                                null,
                                null,
                                null));

        assertThat(violations)
                .filteredOn(v -> v.getPropertyPath().toString().equals("disbursementCoherent"))
                .hasSize(1);
    }

    @Test
    @DisplayName("CAPITAL_IMPROVEMENT without property or asset fails")
    void improvementWithoutLinkFails() {
        Set<ConstraintViolation<TransactionRequest>> violations =
                validator.validate(
                        transaction(
                                TransactionType.EXPENSE,
                                MovementType.CAPITAL_IMPROVEMENT,
                                null,
                                null,
                                null));

        assertThat(violations)
                .filteredOn(v -> v.getPropertyPath().toString().equals("improvementCoherent"))
                .hasSize(1);
    }

    @Test
    @DisplayName("TRANSFER with null movementType passes")
    void transferWithoutMovementPasses() {
        Set<ConstraintViolation<TransactionRequest>> violations =
                validator.validate(transaction(TransactionType.TRANSFER, null, null, null, null));

        assertThat(violations).isEmpty();
    }
}
