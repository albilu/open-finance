package org.openfinance.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.openfinance.dto.TransactionRequest;
import org.openfinance.entity.MovementType;
import org.openfinance.entity.Transaction;
import org.openfinance.entity.TransactionType;

@DisplayName("TransactionMapperLink Tests")
class TransactionMapperLinkTest {

    private final TransactionMapper mapper = Mappers.getMapper(TransactionMapper.class);

    @Test
    @DisplayName("Should round-trip trancheId, realEstateId and movementType")
    void shouldRoundTripMovementLinks() {
        TransactionRequest request =
                TransactionRequest.builder()
                        .accountId(1L)
                        .type(TransactionType.EXPENSE)
                        .amount(new BigDecimal("1200.00"))
                        .currency("USD")
                        .date(LocalDate.of(2026, 1, 15))
                        .liabilityId(10L)
                        .trancheId(3L)
                        .realEstateId(12L)
                        .movementType(MovementType.REPAYMENT)
                        .build();

        Transaction entity = mapper.toEntity(request);

        assertThat(entity.getTrancheId()).isEqualTo(3L);
        assertThat(entity.getRealEstateId()).isEqualTo(12L);
        assertThat(entity.getMovementType()).isEqualTo(MovementType.REPAYMENT);
    }
}
