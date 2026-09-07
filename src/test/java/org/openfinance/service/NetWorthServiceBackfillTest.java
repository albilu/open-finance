package org.openfinance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openfinance.entity.Liability;
import org.openfinance.entity.NetWorth;
import org.openfinance.entity.Transaction;
import org.openfinance.entity.TransactionSplit;
import org.openfinance.entity.TransactionType;
import org.openfinance.repository.AccountRepository;
import org.openfinance.repository.AssetRepository;
import org.openfinance.repository.CurrencyRepository;
import org.openfinance.repository.LiabilityRepository;
import org.openfinance.repository.NetWorthRepository;
import org.openfinance.repository.RealEstateRepository;
import org.openfinance.repository.RealEstateValueHistoryRepository;
import org.openfinance.repository.TransactionRepository;
import org.openfinance.repository.TransactionSplitRepository;
import org.openfinance.security.EncryptionService;
import org.openfinance.testutil.DefaultCurrencyProviderMocks;

/**
 * Unit tests for the liability-reversal logic of {@link NetWorthService#backfillNetWorthHistory}.
 *
 * <p>Historical liability balances are reconstructed by reversing repayment payments made after
 * each target date. Since Task 6 a repayment can carry categorized splits (interest / insurance
 * legs); only the <em>principal leg</em> (total − categorized splits) reduced the outstanding
 * balance, so only that leg may be added back. Reversing the full payment total would inflate
 * historical debt.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("NetWorthService backfill — principal-only liability reversal")
class NetWorthServiceBackfillTest {

    @Mock private NetWorthRepository netWorthRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private AssetRepository assetRepository;
    @Mock private LiabilityRepository liabilityRepository;
    @Mock private RealEstateRepository realEstateRepository;
    @Mock private RealEstateValueHistoryRepository realEstateValueHistoryRepository;
    @Mock private EncryptionService encryptionService;
    @Mock private ExchangeRateService exchangeRateService;
    @Mock private TransactionRepository transactionRepository;
    @Mock private TransactionSplitRepository transactionSplitRepository;
    @Mock private CurrencyRepository currencyRepository;
    @Mock private DefaultCurrencyProvider defaultCurrencyProvider;
    @Mock private NetWorthSnapshotWriter snapshotWriter;

    @InjectMocks private NetWorthService netWorthService;

    private static final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        when(accountRepository.findByUserIdAndIsActive(USER_ID, true)).thenReturn(List.of());
        when(assetRepository.findByUserId(USER_ID)).thenReturn(List.of());
        when(realEstateRepository.findByUserIdAndIsActive(USER_ID, true)).thenReturn(List.of());
        when(realEstateValueHistoryRepository.findByUserId(USER_ID)).thenReturn(List.of());
        when(netWorthRepository.findByUserIdAndSnapshotDate(any(), any()))
                .thenReturn(Optional.empty());
        when(exchangeRateService.convert(any(BigDecimal.class), any(), any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        DefaultCurrencyProviderMocks.stub(defaultCurrencyProvider);
    }

    private Liability mortgage() {
        Liability liability = new Liability();
        liability.setId(10L);
        liability.setUserId(USER_ID);
        liability.setPrincipal("200000");
        liability.setCurrentBalance("99200");
        liability.setCurrency("USD");
        liability.setStartDate(LocalDate.of(2025, 6, 1));
        return liability;
    }

    private Transaction repayment(Long id, BigDecimal amount, LocalDate date) {
        return Transaction.builder()
                .id(id)
                .userId(USER_ID)
                .accountId(1L)
                .type(TransactionType.EXPENSE)
                .amount(amount)
                .currency("USD")
                .date(date)
                .liabilityId(10L)
                .build();
    }

    private TransactionSplit split(long transactionId, Long categoryId, BigDecimal amount) {
        return TransactionSplit.builder()
                .transactionId(transactionId)
                .categoryId(categoryId)
                .amount(amount)
                .build();
    }

    private List<NetWorth> runBackfill(List<Transaction> transactions) {
        when(transactionRepository.findByUserId(USER_ID)).thenReturn(transactions);
        when(liabilityRepository.findByUserIdOrderByCreatedAtDesc(USER_ID))
                .thenReturn(List.of(mortgage()));

        netWorthService.backfillNetWorthHistory(
                USER_ID, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 10), "USD", false);

        ArgumentCaptor<NetWorth> captor = ArgumentCaptor.forClass(NetWorth.class);
        verify(netWorthRepository, atLeastOnce()).save(captor.capture());
        return captor.getAllValues();
    }

    @Test
    @DisplayName(
            "repayment with categorized splits adds back only the principal leg (800, not 1200)")
    void backfillReversesOnlyPrincipalLegOfSplitRepayment() {
        when(transactionSplitRepository.findByTransactionIdIn(anyList()))
                .thenReturn(
                        List.of(
                                split(100L, 5L, new BigDecimal("300")),
                                split(100L, 6L, new BigDecimal("100"))));

        List<NetWorth> snapshots =
                runBackfill(
                        List.of(
                                repayment(
                                        100L, new BigDecimal("1200"), LocalDate.of(2026, 2, 15))));

        assertThat(snapshots).hasSize(3);
        NetWorth january = snapshots.get(0);
        NetWorth february = snapshots.get(1);
        assertThat(snapshots.get(2).getSnapshotDate()).isEqualTo(LocalDate.of(2026, 3, 1));

        // Payment (2026-02-15) is after the Jan and Feb targets: the principal leg 1200 − 400
        // = 800 is added back. The interest/insurance legs (300 + 100) must NOT inflate debt.
        assertThat(january.getTotalLiabilities()).isEqualByComparingTo("100000");
        assertThat(february.getTotalLiabilities()).isEqualByComparingTo("100000");
        // After the payment date the balance is simply the current balance.
        assertThat(snapshots.get(2).getTotalLiabilities()).isEqualByComparingTo("99200");
    }

    @Test
    @DisplayName("plain repayment without splits adds back the full amount")
    void backfillReversesFullPlainRepayment() {
        when(transactionSplitRepository.findByTransactionIdIn(anyList())).thenReturn(List.of());

        List<NetWorth> snapshots =
                runBackfill(
                        List.of(
                                repayment(
                                        100L, new BigDecimal("1200"), LocalDate.of(2026, 2, 15))));

        assertThat(snapshots).hasSize(3);
        assertThat(snapshots.get(0).getTotalLiabilities()).isEqualByComparingTo("100400");
        assertThat(snapshots.get(1).getTotalLiabilities()).isEqualByComparingTo("100400");
        assertThat(snapshots.get(2).getTotalLiabilities()).isEqualByComparingTo("99200");
    }

    @Test
    @DisplayName("fully categorized (interest-only) repayment adds back zero")
    void backfillReversesNothingForInterestOnlyRepayment() {
        when(transactionSplitRepository.findByTransactionIdIn(anyList()))
                .thenReturn(List.of(split(100L, 7L, new BigDecimal("300"))));

        List<NetWorth> snapshots =
                runBackfill(
                        List.of(repayment(100L, new BigDecimal("300"), LocalDate.of(2026, 2, 15))));

        assertThat(snapshots).hasSize(3);
        // 300 − 300 categorized = 0 principal leg → historical balance equals current balance.
        assertThat(snapshots.get(0).getTotalLiabilities()).isEqualByComparingTo("99200");
        assertThat(snapshots.get(1).getTotalLiabilities()).isEqualByComparingTo("99200");
    }
}
