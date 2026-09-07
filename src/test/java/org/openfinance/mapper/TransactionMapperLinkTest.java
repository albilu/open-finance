package org.openfinance.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.openfinance.dto.TransactionRequest;
import org.openfinance.dto.TransactionResponse;
import org.openfinance.entity.MovementType;
import org.openfinance.entity.Transaction;
import org.openfinance.entity.TransactionType;

@DisplayName("TransactionMapperLink Tests")
class TransactionMapperLinkTest {

    private final TransactionMapper mapper = Mappers.getMapper(TransactionMapper.class);

    @Test
    @DisplayName("Should round-trip trancheId, realEstateId, assetId and movementType")
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
                        .assetId(7L)
                        .movementType(MovementType.REPAYMENT)
                        .build();

        Transaction entity = mapper.toEntity(request);

        assertThat(entity.getLiabilityId()).isEqualTo(10L);
        assertThat(entity.getTrancheId()).isEqualTo(3L);
        assertThat(entity.getRealEstateId()).isEqualTo(12L);
        assertThat(entity.getAssetId()).isEqualTo(7L);
        assertThat(entity.getMovementType()).isEqualTo(MovementType.REPAYMENT);

        TransactionResponse response = mapper.toResponse(entity);

        assertThat(response.getLiabilityId()).isEqualTo(10L);
        assertThat(response.getTrancheId()).isEqualTo(3L);
        assertThat(response.getRealEstateId()).isEqualTo(12L);
        assertThat(response.getAssetId()).isEqualTo(7L);
        assertThat(response.getMovementType()).isEqualTo(MovementType.REPAYMENT);
    }
}
