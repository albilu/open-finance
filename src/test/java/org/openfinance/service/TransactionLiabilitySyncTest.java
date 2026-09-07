package org.openfinance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
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
import org.openfinance.dto.TransactionRequest;
import org.openfinance.dto.TransactionResponse;
import org.openfinance.dto.TransactionSplitRequest;
import org.openfinance.dto.TransactionSplitResponse;
import org.openfinance.entity.Account;
import org.openfinance.entity.Liability;
import org.openfinance.entity.LiabilityTranche;
import org.openfinance.entity.MovementType;
import org.openfinance.entity.RealEstateProperty;
import org.openfinance.entity.RealEstateValueHistory;
import org.openfinance.entity.TrancheStatus;
import org.openfinance.entity.Transaction;
import org.openfinance.entity.TransactionType;
import org.openfinance.exception.InvalidTransactionException;
import org.openfinance.mapper.TransactionMapper;
import org.openfinance.repository.AccountRepository;
import org.openfinance.repository.CategoryRepository;
import org.openfinance.repository.CurrencyRepository;
import org.openfinance.repository.LiabilityRepository;
import org.openfinance.repository.LiabilityTrancheRepository;
import org.openfinance.repository.NetWorthRepository;
import org.openfinance.repository.PayeeRepository;
import org.openfinance.repository.RealEstateRepository;
import org.openfinance.repository.RealEstateValueHistoryRepository;
import org.openfinance.repository.TransactionRepository;
import org.openfinance.repository.UserRepository;
import org.openfinance.security.EncryptionService;
import org.springframework.context.MessageSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for Task 3: liability / property balance synchronization on linked transactions.
 *
 * <p>Covers DISBURSEMENT increases, REPAYMENT principal-only reduction via categorized splits,
 * reversal on delete/update, capital improvements on real estate, currency guard and tranche
 * clamping.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Transaction ↔ Liability/Property balance sync")
class TransactionLiabilitySyncTest {

    private static final Long USER_ID = 1L;
    private static final Long ACCOUNT_ID = 10L;
    private static final Long LIABILITY_ID = 200L;
    private static final Long PROPERTY_ID = 300L;
    private static final Long TX_ID = 100L;
    private static final Long TRANCHE_ID = 400L;

    @Mock private TransactionRepository transactionRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private PayeeRepository payeeRepository;
    @Mock private CurrencyRepository currencyRepository;
    @Mock private TransactionMapper transactionMapper;
    @Mock private EncryptionService encryptionService;
    @Mock private BudgetAlertService budgetAlertService;
    @Mock private TransactionSplitService transactionSplitService;
    @Mock private UserRepository userRepository;
    @Mock private ExchangeRateService exchangeRateService;
    @Mock private JdbcTemplate jdbcTemplate;
    @Mock private MessageSource messageSource;
    @Mock private NetWorthRepository netWorthRepository;
    @Mock private OperationHistoryService operationHistoryService;
    @Mock private SearchTokenService searchTokenService;
    @Mock private DefaultCurrencyProvider defaultCurrencyProvider;
    @Mock private CurrencyConversionHelper currencyConversionHelperField;
    @Mock private LiabilityRepository liabilityRepository;
    @Mock private LiabilityTrancheRepository liabilityTrancheRepository;
    @Mock private RealEstateRepository realEstateRepository;
    @Mock private RealEstateValueHistoryRepository realEstateValueHistoryRepository;
    @Mock private org.openfinance.mapper.RealEstateMapper realEstateMapper;
    @Mock private AssetService assetService;

    @InjectMocks private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        when(userRepository.findById(any())).thenReturn(Optional.empty());
        org.openfinance.testutil.DefaultCurrencyProviderMocks.stub(
                defaultCurrencyProvider, userRepository);
        CurrencyConversionHelper helper =
                new CurrencyConversionHelper(
                        userRepository, defaultCurrencyProvider, exchangeRateService);
        ReflectionTestUtils.setField(transactionService, "currencyConversionHelper", helper);
        RealEstateService realEstateService =
                new RealEstateService(
                        realEstateRepository,
                        realEstateValueHistoryRepository,
                        liabilityRepository,
                        currencyRepository,
                        realEstateMapper,
                        encryptionService,
                        assetService,
                        userRepository,
                        exchangeRateService,
                        netWorthRepository,
                        operationHistoryService,
                        searchTokenService,
                        defaultCurrencyProvider,
                        helper);
        ReflectionTestUtils.setField(transactionService, "realEstateService", realEstateService);
    }

    // ---------- Helpers ----------

    private Account accountFixture(String currency) {
        return Account.builder()
                .id(ACCOUNT_ID)
                .userId(USER_ID)
                .currency(currency)
                .name("Checking")
                .balance(new BigDecimal("1000.00"))
                .build();
    }

    private Liability liabilityFixture(String currentBalance, String currency) {
        Liability liability = new Liability();
        liability.setId(LIABILITY_ID);
        liability.setUserId(USER_ID);
        liability.setCurrency(currency);
        liability.setCurrentBalance(currentBalance);
        return liability;
    }

    private RealEstateProperty propertyFixture(String currentValue) {
        return RealEstateProperty.builder()
                .id(PROPERTY_ID)
                .userId(USER_ID)
                .currency("USD")
                .currentValue(currentValue)
                .build();
    }

    private TransactionRequest linkedRequest(
            BigDecimal amount, MovementType movementType, String currency) {
        return TransactionRequest.builder()
                .accountId(ACCOUNT_ID)
                .type(TransactionType.EXPENSE)
                .amount(amount)
                .currency(currency)
                .date(LocalDate.now())
                .movementType(movementType)
                .liabilityId(movementType == MovementType.CAPITAL_IMPROVEMENT ? null : LIABILITY_ID)
                .realEstateId(movementType == MovementType.CAPITAL_IMPROVEMENT ? PROPERTY_ID : null)
                .build();
    }

    private Transaction linkedEntity(
            Long id, BigDecimal amount, MovementType movementType, String currency) {
        return Transaction.builder()
                .id(id)
                .userId(USER_ID)
                .accountId(ACCOUNT_ID)
                .type(TransactionType.EXPENSE)
                .amount(amount)
                .currency(currency)
                .date(LocalDate.now())
                .movementType(movementType)
                .liabilityId(movementType == MovementType.CAPITAL_IMPROVEMENT ? null : LIABILITY_ID)
                .realEstateId(movementType == MovementType.CAPITAL_IMPROVEMENT ? PROPERTY_ID : null)
                .build();
    }

    private TransactionSplitRequest split(BigDecimal amount, Long categoryId) {
        return TransactionSplitRequest.builder().amount(amount).categoryId(categoryId).build();
    }

    private TransactionSplitResponse storedSplit(BigDecimal amount, Long categoryId) {
        return TransactionSplitResponse.builder().amount(amount).categoryId(categoryId).build();
    }

    private void stubCreate(TransactionRequest request, Transaction mapped, Transaction saved) {
        when(accountRepository.findByIdAndUserId(ACCOUNT_ID, USER_ID))
                .thenReturn(Optional.of(accountFixture(request.getCurrency())));
        when(transactionMapper.toEntity(request)).thenReturn(mapped);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(saved);
        when(transactionMapper.toResponse(any(Transaction.class)))
                .thenReturn(new TransactionResponse());
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
        when(liabilityRepository.save(any(Liability.class))).thenAnswer(inv -> inv.getArgument(0));
        when(realEstateRepository.save(any(RealEstateProperty.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(realEstateValueHistoryRepository.save(any(RealEstateValueHistory.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    // ---------- (a) DISBURSEMENT increases liability balance ----------

    @Test
    @DisplayName("DISBURSEMENT increases Liability.currentBalance by full amount")
    void disbursementIncreasesLiabilityBalanceByFullAmount() {
        TransactionRequest request =
                linkedRequest(new BigDecimal("2000.00"), MovementType.DISBURSEMENT, "USD");
        stubCreate(
                request,
                linkedEntity(null, new BigDecimal("2000.00"), MovementType.DISBURSEMENT, "USD"),
                linkedEntity(TX_ID, new BigDecimal("2000.00"), MovementType.DISBURSEMENT, "USD"));
        when(liabilityRepository.findByIdAndUserId(LIABILITY_ID, USER_ID))
                .thenReturn(Optional.of(liabilityFixture("5000.00", "USD")));

        transactionService.createTransaction(USER_ID, request);

        ArgumentCaptor<Liability> captor = ArgumentCaptor.forClass(Liability.class);
        verify(liabilityRepository).save(captor.capture());
        assertThat(captor.getValue().getCurrentBalance()).isEqualTo("7000.00");
    }

    // ---------- (b) REPAYMENT reduces by principal leg only ----------

    @Test
    @DisplayName(
            "REPAYMENT 1200 with categorized splits 300+100 reduces liability by 800 principal only")
    void repaymentReducesLiabilityByPrincipalLegOnly() {
        TransactionRequest request =
                linkedRequest(new BigDecimal("1200.00"), MovementType.REPAYMENT, "USD");
        request.setSplits(
                List.of(
                        split(new BigDecimal("800.00"), null),
                        split(new BigDecimal("300.00"), 5L),
                        split(new BigDecimal("100.00"), 6L)));
        stubCreate(
                request,
                linkedEntity(null, new BigDecimal("1200.00"), MovementType.REPAYMENT, "USD"),
                linkedEntity(TX_ID, new BigDecimal("1200.00"), MovementType.REPAYMENT, "USD"));
        when(liabilityRepository.findByIdAndUserId(LIABILITY_ID, USER_ID))
                .thenReturn(Optional.of(liabilityFixture("5000.00", "USD")));

        transactionService.createTransaction(USER_ID, request);

        ArgumentCaptor<Liability> captor = ArgumentCaptor.forClass(Liability.class);
        verify(liabilityRepository).save(captor.capture());
        assertThat(captor.getValue().getCurrentBalance()).isEqualTo("4200.00");
    }

    // ---------- (c) reversal on delete restores balance ----------

    @Test
    @DisplayName("Deleting a REPAYMENT restores the principal previously deducted")
    void deleteRestoresLiabilityBalance() {
        Transaction existing =
                linkedEntity(TX_ID, new BigDecimal("1200.00"), MovementType.REPAYMENT, "USD");
        when(transactionRepository.findByIdAndUserId(TX_ID, USER_ID))
                .thenReturn(Optional.of(existing));
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(accountRepository.findByIdAndUserId(ACCOUNT_ID, USER_ID))
                .thenReturn(Optional.of(accountFixture("USD")));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
        when(transactionSplitService.getSplitsForTransaction(TX_ID))
                .thenReturn(
                        List.of(
                                storedSplit(new BigDecimal("800.00"), null),
                                storedSplit(new BigDecimal("300.00"), 5L),
                                storedSplit(new BigDecimal("100.00"), 6L)));
        when(liabilityRepository.findByIdAndUserId(LIABILITY_ID, USER_ID))
                .thenReturn(Optional.of(liabilityFixture("4200.00", "USD")));
        when(liabilityRepository.save(any(Liability.class))).thenAnswer(inv -> inv.getArgument(0));

        transactionService.deleteTransaction(TX_ID, USER_ID);

        ArgumentCaptor<Liability> captor = ArgumentCaptor.forClass(Liability.class);
        verify(liabilityRepository).save(captor.capture());
        assertThat(captor.getValue().getCurrentBalance()).isEqualTo("5000.00");
    }

    // ---------- update reverses old legs then applies new ----------

    @Test
    @DisplayName("Update reverses the old principal leg then applies the new one")
    void updateReversesOldAndAppliesNewLiabilityMovement() {
        Transaction existing =
                linkedEntity(TX_ID, new BigDecimal("1200.00"), MovementType.REPAYMENT, "USD");
        when(transactionRepository.findByIdAndUserId(TX_ID, USER_ID))
                .thenReturn(Optional.of(existing));
        when(accountRepository.findByIdAndUserId(ACCOUNT_ID, USER_ID))
                .thenReturn(Optional.of(accountFixture("USD")));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(transactionMapper.toResponse(any(Transaction.class)))
                .thenReturn(new TransactionResponse());
        when(transactionSplitService.getSplitsForTransaction(TX_ID))
                .thenReturn(
                        List.of(
                                storedSplit(new BigDecimal("800.00"), null),
                                storedSplit(new BigDecimal("300.00"), 5L),
                                storedSplit(new BigDecimal("100.00"), 6L)));
        when(liabilityRepository.findByIdAndUserId(LIABILITY_ID, USER_ID))
                .thenReturn(Optional.of(liabilityFixture("4200.00", "USD")));
        when(liabilityRepository.save(any(Liability.class))).thenAnswer(inv -> inv.getArgument(0));

        TransactionRequest update =
                linkedRequest(new BigDecimal("900.00"), MovementType.REPAYMENT, "USD");
        update.setSplits(
                List.of(
                        split(new BigDecimal("600.00"), null),
                        split(new BigDecimal("300.00"), 5L)));

        transactionService.updateTransaction(TX_ID, USER_ID, update);

        // Reverse +800 (4200 → 5000) then apply −600 principal (5000 → 4400)
        ArgumentCaptor<Liability> captor = ArgumentCaptor.forClass(Liability.class);
        verify(liabilityRepository, times(2)).save(captor.capture());
        assertThat(captor.getValue().getCurrentBalance()).isEqualTo("4400.00");
    }

    // ---------- capital improvement bumps property value + history ----------

    @Test
    @DisplayName("CAPITAL_IMPROVEMENT increases property value and records value history")
    void capitalImprovementBumpsPropertyValueAndRecordsHistory() {
        TransactionRequest request =
                linkedRequest(new BigDecimal("5000.00"), MovementType.CAPITAL_IMPROVEMENT, "USD");
        stubCreate(
                request,
                linkedEntity(
                        null, new BigDecimal("5000.00"), MovementType.CAPITAL_IMPROVEMENT, "USD"),
                linkedEntity(
                        TX_ID, new BigDecimal("5000.00"), MovementType.CAPITAL_IMPROVEMENT, "USD"));
        when(realEstateRepository.findByIdAndUserId(PROPERTY_ID, USER_ID))
                .thenReturn(Optional.of(propertyFixture("200000.00")));

        transactionService.createTransaction(USER_ID, request);

        ArgumentCaptor<RealEstateProperty> propertyCaptor =
                ArgumentCaptor.forClass(RealEstateProperty.class);
        verify(realEstateRepository).save(propertyCaptor.capture());
        assertThat(propertyCaptor.getValue().getCurrentValue()).isEqualTo("205000.00");

        ArgumentCaptor<RealEstateValueHistory> historyCaptor =
                ArgumentCaptor.forClass(RealEstateValueHistory.class);
        verify(realEstateValueHistoryRepository).save(historyCaptor.capture());
        assertThat(historyCaptor.getValue().getRecordedValue()).isEqualTo("205000.00");
        assertThat(historyCaptor.getValue().getPropertyId()).isEqualTo(PROPERTY_ID);
    }

    // ---------- currency guard ----------

    @Test
    @DisplayName("Liability currency mismatch is rejected with InvalidTransactionException")
    void liabilityCurrencyMismatchIsRejected() {
        TransactionRequest request =
                linkedRequest(new BigDecimal("100.00"), MovementType.REPAYMENT, "EUR");
        stubCreate(
                request,
                linkedEntity(null, new BigDecimal("100.00"), MovementType.REPAYMENT, "EUR"),
                linkedEntity(TX_ID, new BigDecimal("100.00"), MovementType.REPAYMENT, "EUR"));
        when(liabilityRepository.findByIdAndUserId(LIABILITY_ID, USER_ID))
                .thenReturn(Optional.of(liabilityFixture("5000.00", "USD")));

        assertThatThrownBy(() -> transactionService.createTransaction(USER_ID, request))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessageContaining("EUR")
                .hasMessageContaining("USD");
    }

    // ---------- tranche clamp guard ----------

    @Test
    @DisplayName("DISBURSEMENT balance is clamped to SUM(drawn) of DRAWN tranches")
    void disbursementClampedToDrawnTrancheSum() {
        TransactionRequest request =
                linkedRequest(new BigDecimal("2000.00"), MovementType.DISBURSEMENT, "USD");
        stubCreate(
                request,
                linkedEntity(null, new BigDecimal("2000.00"), MovementType.DISBURSEMENT, "USD"),
                linkedEntity(TX_ID, new BigDecimal("2000.00"), MovementType.DISBURSEMENT, "USD"));
        when(liabilityRepository.findByIdAndUserId(LIABILITY_ID, USER_ID))
                .thenReturn(Optional.of(liabilityFixture("5000.00", "USD")));
        LiabilityTranche drawn =
                LiabilityTranche.builder()
                        .id(1L)
                        .liabilityId(LIABILITY_ID)
                        .userId(USER_ID)
                        .trancheNo(1)
                        .plannedAmount(new BigDecimal("6000.00"))
                        .drawnAmount(new BigDecimal("6000.00"))
                        .status(TrancheStatus.DRAWN)
                        .currency("USD")
                        .build();
        LiabilityTranche planned =
                LiabilityTranche.builder()
                        .id(2L)
                        .liabilityId(LIABILITY_ID)
                        .userId(USER_ID)
                        .trancheNo(2)
                        .plannedAmount(new BigDecimal("4000.00"))
                        .status(TrancheStatus.PLANNED)
                        .currency("USD")
                        .build();
        when(liabilityTrancheRepository.findByLiabilityIdAndUserId(LIABILITY_ID, USER_ID))
                .thenReturn(List.of(drawn, planned));

        transactionService.createTransaction(USER_ID, request);

        ArgumentCaptor<Liability> captor = ArgumentCaptor.forClass(Liability.class);
        verify(liabilityRepository).save(captor.capture());
        assertThat(captor.getValue().getCurrentBalance()).isEqualTo("6000.00");
    }

    // ---------- DISBURSEMENT tranche lifecycle on delete/update ----------

    private Transaction disbursementEntity(BigDecimal amount) {
        Transaction tx = linkedEntity(TX_ID, amount, MovementType.DISBURSEMENT, "USD");
        tx.setType(TransactionType.INCOME);
        tx.setTrancheId(TRANCHE_ID);
        return tx;
    }

    private LiabilityTranche drawnTrancheFixture(BigDecimal drawnAmount) {
        return LiabilityTranche.builder()
                .id(TRANCHE_ID)
                .liabilityId(LIABILITY_ID)
                .userId(USER_ID)
                .trancheNo(1)
                .plannedAmount(new BigDecimal("50000.00"))
                .drawnAmount(drawnAmount)
                .drawnDate(LocalDate.now().minusDays(1))
                .status(TrancheStatus.DRAWN)
                .currency("USD")
                .build();
    }

    @Test
    @DisplayName(
            "Deleting a DISBURSEMENT tx reverts its tranche to PLANNED with drawn fields cleared")
    void deleteDisbursementTxRevertsTrancheToPlanned() {
        when(transactionRepository.findByIdAndUserId(TX_ID, USER_ID))
                .thenReturn(Optional.of(disbursementEntity(new BigDecimal("40000.00"))));
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(accountRepository.findByIdAndUserId(ACCOUNT_ID, USER_ID))
                .thenReturn(Optional.of(accountFixture("USD")));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
        when(transactionSplitService.getSplitsForTransaction(TX_ID)).thenReturn(List.of());
        when(liabilityRepository.findByIdAndUserId(LIABILITY_ID, USER_ID))
                .thenReturn(Optional.of(liabilityFixture("40000.00", "USD")));
        when(liabilityRepository.save(any(Liability.class))).thenAnswer(inv -> inv.getArgument(0));
        when(liabilityTrancheRepository.findByIdAndUserId(TRANCHE_ID, USER_ID))
                .thenReturn(Optional.of(drawnTrancheFixture(new BigDecimal("40000.00"))));
        when(liabilityTrancheRepository.save(any(LiabilityTranche.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        transactionService.deleteTransaction(TX_ID, USER_ID);

        ArgumentCaptor<LiabilityTranche> trancheCaptor =
                ArgumentCaptor.forClass(LiabilityTranche.class);
        verify(liabilityTrancheRepository).save(trancheCaptor.capture());
        assertThat(trancheCaptor.getValue().getStatus()).isEqualTo(TrancheStatus.PLANNED);
        assertThat(trancheCaptor.getValue().getDrawnAmount()).isNull();
        assertThat(trancheCaptor.getValue().getDrawnDate()).isNull();
    }

    @Test
    @DisplayName("Updating a DISBURSEMENT tx amount re-marks the tranche DRAWN with the new amount")
    void updateDisbursementTxAmountUpdatesTrancheDrawnAmount() {
        when(transactionRepository.findByIdAndUserId(TX_ID, USER_ID))
                .thenReturn(Optional.of(disbursementEntity(new BigDecimal("40000.00"))));
        when(accountRepository.findByIdAndUserId(ACCOUNT_ID, USER_ID))
                .thenReturn(Optional.of(accountFixture("USD")));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(transactionMapper.toResponse(any(Transaction.class)))
                .thenReturn(new TransactionResponse());
        when(transactionSplitService.getSplitsForTransaction(TX_ID)).thenReturn(List.of());
        when(liabilityRepository.findByIdAndUserId(LIABILITY_ID, USER_ID))
                .thenReturn(Optional.of(liabilityFixture("40000.00", "USD")));
        when(liabilityRepository.save(any(Liability.class))).thenAnswer(inv -> inv.getArgument(0));
        when(liabilityTrancheRepository.findByIdAndUserId(TRANCHE_ID, USER_ID))
                .thenAnswer(inv -> Optional.of(drawnTrancheFixture(new BigDecimal("40000.00"))));
        when(liabilityTrancheRepository.save(any(LiabilityTranche.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        TransactionRequest update =
                linkedRequest(new BigDecimal("45000.00"), MovementType.DISBURSEMENT, "USD");
        update.setTrancheId(TRANCHE_ID);

        transactionService.updateTransaction(TX_ID, USER_ID, update);

        // First save reverts the tranche to PLANNED, the second re-marks it DRAWN at 45000
        ArgumentCaptor<LiabilityTranche> trancheCaptor =
                ArgumentCaptor.forClass(LiabilityTranche.class);
        verify(liabilityTrancheRepository, times(2)).save(trancheCaptor.capture());
        assertThat(trancheCaptor.getAllValues().get(0).getStatus())
                .isEqualTo(TrancheStatus.PLANNED);
        assertThat(trancheCaptor.getValue().getStatus()).isEqualTo(TrancheStatus.DRAWN);
        assertThat(trancheCaptor.getValue().getDrawnAmount())
                .isEqualByComparingTo(new BigDecimal("45000.00"));
    }

    @Test
    @DisplayName(
            "Updating a DISBURSEMENT amount keeps the liability balance at the new amount "
                    + "(tranche re-marked DRAWN before the reconcile clamp)")
    void updateDisbursementAmountKeepsLiabilityBalanceAfterRedraw() {
        // Shared mutable tranche so the reconcile sees the state persisted so far
        LiabilityTranche tranche = drawnTrancheFixture(new BigDecimal("40000.00"));
        when(transactionRepository.findByIdAndUserId(TX_ID, USER_ID))
                .thenReturn(Optional.of(disbursementEntity(new BigDecimal("40000.00"))));
        when(accountRepository.findByIdAndUserId(ACCOUNT_ID, USER_ID))
                .thenReturn(Optional.of(accountFixture("USD")));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(transactionMapper.toResponse(any(Transaction.class)))
                .thenReturn(new TransactionResponse());
        when(transactionSplitService.getSplitsForTransaction(TX_ID)).thenReturn(List.of());
        when(liabilityRepository.findByIdAndUserId(LIABILITY_ID, USER_ID))
                .thenReturn(Optional.of(liabilityFixture("40000.00", "USD")));
        when(liabilityRepository.save(any(Liability.class))).thenAnswer(inv -> inv.getArgument(0));
        when(liabilityTrancheRepository.findByIdAndUserId(TRANCHE_ID, USER_ID))
                .thenReturn(Optional.of(tranche));
        when(liabilityTrancheRepository.findByLiabilityIdAndUserId(LIABILITY_ID, USER_ID))
                .thenReturn(List.of(tranche));
        when(liabilityTrancheRepository.save(any(LiabilityTranche.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        TransactionRequest update =
                linkedRequest(new BigDecimal("45000.00"), MovementType.DISBURSEMENT, "USD");
        update.setTrancheId(TRANCHE_ID);

        transactionService.updateTransaction(TX_ID, USER_ID, update);

        // Reverse 40000 (balance → 0, tranche PLANNED), re-draw 45000: the clamp must count the
        // re-drawn tranche, so the final balance is 45000 — not clamped down to 0.
        ArgumentCaptor<Liability> liabilityCaptor = ArgumentCaptor.forClass(Liability.class);
        verify(liabilityRepository, times(2)).save(liabilityCaptor.capture());
        assertThat(liabilityCaptor.getValue().getCurrentBalance()).isEqualTo("45000.00");

        ArgumentCaptor<LiabilityTranche> trancheCaptor =
                ArgumentCaptor.forClass(LiabilityTranche.class);
        verify(liabilityTrancheRepository, times(2)).save(trancheCaptor.capture());
        assertThat(trancheCaptor.getValue().getStatus()).isEqualTo(TrancheStatus.DRAWN);
        assertThat(trancheCaptor.getValue().getDrawnAmount())
                .isEqualByComparingTo(new BigDecimal("45000.00"));
    }

    @Test
    @DisplayName(
            "Creating a DISBURSEMENT with trancheId marks the tranche DRAWN before the balance "
                    + "reconcile, so the balance equals the amount")
    void createDisbursementWithTrancheIdMarksDrawnThenAddsBalance() {
        LiabilityTranche planned =
                LiabilityTranche.builder()
                        .id(TRANCHE_ID)
                        .liabilityId(LIABILITY_ID)
                        .userId(USER_ID)
                        .trancheNo(1)
                        .plannedAmount(new BigDecimal("50000.00"))
                        .status(TrancheStatus.PLANNED)
                        .currency("USD")
                        .build();
        TransactionRequest request =
                linkedRequest(new BigDecimal("45000.00"), MovementType.DISBURSEMENT, "USD");
        request.setTrancheId(TRANCHE_ID);
        Transaction mapped =
                linkedEntity(null, new BigDecimal("45000.00"), MovementType.DISBURSEMENT, "USD");
        mapped.setTrancheId(TRANCHE_ID);
        Transaction saved =
                linkedEntity(TX_ID, new BigDecimal("45000.00"), MovementType.DISBURSEMENT, "USD");
        saved.setTrancheId(TRANCHE_ID);
        stubCreate(request, mapped, saved);
        when(liabilityRepository.findByIdAndUserId(LIABILITY_ID, USER_ID))
                .thenReturn(Optional.of(liabilityFixture("0.00", "USD")));
        when(liabilityTrancheRepository.findByIdAndUserId(TRANCHE_ID, USER_ID))
                .thenReturn(Optional.of(planned));
        when(liabilityTrancheRepository.findByLiabilityIdAndUserId(LIABILITY_ID, USER_ID))
                .thenReturn(List.of(planned));
        when(liabilityTrancheRepository.save(any(LiabilityTranche.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        transactionService.createTransaction(USER_ID, request);

        // The tranche is marked DRAWN before the reconcile, so drawnSum counts it and the
        // balance is the full amount instead of being clamped down to 0.
        ArgumentCaptor<Liability> liabilityCaptor = ArgumentCaptor.forClass(Liability.class);
        verify(liabilityRepository).save(liabilityCaptor.capture());
        assertThat(liabilityCaptor.getValue().getCurrentBalance()).isEqualTo("45000.00");

        ArgumentCaptor<LiabilityTranche> trancheCaptor =
                ArgumentCaptor.forClass(LiabilityTranche.class);
        verify(liabilityTrancheRepository).save(trancheCaptor.capture());
        assertThat(trancheCaptor.getValue().getStatus()).isEqualTo(TrancheStatus.DRAWN);
        assertThat(trancheCaptor.getValue().getDrawnAmount())
                .isEqualByComparingTo(new BigDecimal("45000.00"));
    }
}
