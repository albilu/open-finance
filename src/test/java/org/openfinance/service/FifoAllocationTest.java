package org.openfinance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
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
import org.openfinance.entity.Account;
import org.openfinance.entity.Liability;
import org.openfinance.entity.LiabilityTranche;
import org.openfinance.entity.MovementType;
import org.openfinance.entity.TrancheStatus;
import org.openfinance.entity.Transaction;
import org.openfinance.entity.TransactionType;
import org.openfinance.exception.InvalidLiabilityStateException;
import org.openfinance.exception.InvalidTransactionException;
import org.openfinance.mapper.TransactionMapper;
import org.openfinance.repository.AccountRepository;
import org.openfinance.repository.CategoryRepository;
import org.openfinance.repository.CurrencyRepository;
import org.openfinance.repository.LiabilityRepository;
import org.openfinance.repository.LiabilityTrancheRepository;
import org.openfinance.repository.NetWorthRepository;
import org.openfinance.repository.PayeeRepository;
import org.openfinance.repository.TransactionRepository;
import org.openfinance.repository.UserRepository;
import org.openfinance.security.EncryptionService;
import org.springframework.context.MessageSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for Task 7: FIFO tranche allocation of repayments and the spec §3.2 invariant
 * reconciler (Liability.currentBalance = SUM(tranche.remaining)).
 *
 * <p>V1 semantics under test: a single payment allocates to a single tranche (explicit target
 * honored, else FIFO pick of the oldest DRAWN tranche with remaining > 0). The reconciler derives
 * the balance as an absolute assignment over DRAWN tranches; allocated principal per tranche is the
 * sum of REPAYMENT principal legs capped at the drawn amount.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("FIFO tranche allocation + invariant reconciler")
class FifoAllocationTest {

    private static final Long USER_ID = 1L;
    private static final Long ACCOUNT_ID = 10L;
    private static final Long LIABILITY_ID = 200L;
    private static final Long TX_ID = 100L;
    private static final Long T1_ID = 401L;
    private static final Long T2_ID = 402L;

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

    @InjectMocks private TransactionService transactionService;

    /** In-memory transaction table so repository queries reflect save() and soft-delete state. */
    private final List<Transaction> txTable = new ArrayList<>();

    @BeforeEach
    void setUp() {
        when(userRepository.findById(any())).thenReturn(Optional.empty());
        org.openfinance.testutil.DefaultCurrencyProviderMocks.stub(
                defaultCurrencyProvider, userRepository);
        CurrencyConversionHelper helper =
                new CurrencyConversionHelper(
                        userRepository, defaultCurrencyProvider, exchangeRateService);
        ReflectionTestUtils.setField(transactionService, "currencyConversionHelper", helper);

        LiabilityTrancheService trancheService =
                new LiabilityTrancheService(
                        liabilityTrancheRepository, transactionRepository, transactionSplitService);
        ReflectionTestUtils.setField(transactionService, "liabilityTrancheService", trancheService);

        txTable.clear();
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(
                        inv -> {
                            Transaction t = inv.getArgument(0);
                            if (t.getId() == null) {
                                t.setId(TX_ID);
                            }
                            txTable.removeIf(x -> x.getId().equals(t.getId()));
                            txTable.add(t);
                            return t;
                        });
        when(transactionRepository.findByTrancheIdAndUserId(any(), any()))
                .thenAnswer(
                        inv -> {
                            Long trancheId = inv.getArgument(0);
                            Long userId = inv.getArgument(1);
                            return txTable.stream()
                                    .filter(
                                            t ->
                                                    trancheId.equals(t.getTrancheId())
                                                            && userId.equals(t.getUserId())
                                                            && !Boolean.TRUE.equals(
                                                                    t.getIsDeleted()))
                                    .sorted(
                                            Comparator.comparing(
                                                    Transaction::getDate,
                                                    Comparator.nullsLast(
                                                            Comparator.naturalOrder())))
                                    .toList();
                        });
        when(transactionSplitService.getSplitsForTransaction(any())).thenReturn(List.of());
    }

    // ---------- Helpers ----------

    private Account accountFixture() {
        return Account.builder()
                .id(ACCOUNT_ID)
                .userId(USER_ID)
                .currency("USD")
                .name("Checking")
                .balance(new BigDecimal("1000000.00"))
                .build();
    }

    private Liability liabilityFixture(String currentBalance) {
        Liability liability = new Liability();
        liability.setId(LIABILITY_ID);
        liability.setUserId(USER_ID);
        liability.setCurrency("USD");
        liability.setCurrentBalance(currentBalance);
        return liability;
    }

    private LiabilityTranche tranche(
            Long id, Integer trancheNo, BigDecimal drawnAmount, LocalDate drawnDate) {
        return LiabilityTranche.builder()
                .id(id)
                .liabilityId(LIABILITY_ID)
                .userId(USER_ID)
                .trancheNo(trancheNo)
                .plannedAmount(drawnAmount)
                .drawnAmount(drawnAmount)
                .drawnDate(drawnDate)
                .status(TrancheStatus.DRAWN)
                .currency("USD")
                .build();
    }

    private TransactionRequest repaymentRequest(BigDecimal amount) {
        return org.openfinance.dto.TransactionRequest.builder()
                .accountId(ACCOUNT_ID)
                .type(TransactionType.EXPENSE)
                .amount(amount)
                .currency("USD")
                .date(LocalDate.now())
                .movementType(MovementType.REPAYMENT)
                .liabilityId(LIABILITY_ID)
                .build();
    }

    private Transaction repaymentEntity(Long id, BigDecimal amount, Long trancheId) {
        return Transaction.builder()
                .id(id)
                .userId(USER_ID)
                .accountId(ACCOUNT_ID)
                .type(TransactionType.EXPENSE)
                .amount(amount)
                .currency("USD")
                .date(LocalDate.now())
                .movementType(MovementType.REPAYMENT)
                .liabilityId(LIABILITY_ID)
                .trancheId(trancheId)
                .build();
    }

    /** Stubs the shared create-transaction path; the mapped entity flows through save() twice. */
    private Transaction stubCreate(
            TransactionRequest request, Transaction mapped, String startingBalance) {
        when(accountRepository.findByIdAndUserId(ACCOUNT_ID, USER_ID))
                .thenReturn(Optional.of(accountFixture()));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
        when(transactionMapper.toEntity(request)).thenReturn(mapped);
        when(transactionMapper.toResponse(any(Transaction.class)))
                .thenReturn(new TransactionResponse());
        when(liabilityRepository.save(any(Liability.class))).thenAnswer(inv -> inv.getArgument(0));
        when(liabilityRepository.findByIdAndUserId(LIABILITY_ID, USER_ID))
                .thenReturn(Optional.of(liabilityFixture(startingBalance)));
        return mapped;
    }

    private List<LiabilityTranche> twoDrawnTranches() {
        LiabilityTranche t1 =
                tranche(T1_ID, 1, new BigDecimal("10000.00"), LocalDate.now().minusDays(10));
        LiabilityTranche t2 =
                tranche(T2_ID, 2, new BigDecimal("80000.00"), LocalDate.now().minusDays(5));
        when(liabilityTrancheRepository.findByLiabilityIdAndUserId(LIABILITY_ID, USER_ID))
                .thenReturn(List.of(t1, t2));
        return List.of(t1, t2);
    }

    private String lastSavedLiabilityBalance() {
        ArgumentCaptor<Liability> captor = ArgumentCaptor.forClass(Liability.class);
        verify(liabilityRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        return captor.getValue().getCurrentBalance();
    }

    // ---------- (a) FIFO pick + overpay clamp on the picked tranche ----------

    @Test
    @DisplayName(
            "FIFO repayment 12000 picks T1 (remaining 10000) and clamps: T1 fully allocated, "
                    + "balance 90000 -> 80000, T2 untouched")
    void fifoRepaymentClampsToPickedTrancheRemaining() {
        twoDrawnTranches();
        TransactionRequest request = repaymentRequest(new BigDecimal("12000.00"));
        Transaction tx =
                stubCreate(
                        request,
                        repaymentEntity(null, new BigDecimal("12000.00"), null),
                        "90000.00");

        transactionService.createTransaction(USER_ID, request);

        assertThat(tx.getTrancheId()).isEqualTo(T1_ID);
        assertThat(lastSavedLiabilityBalance()).isEqualTo("80000.00");
    }

    // ---------- (b) explicit trancheId target ----------

    @Test
    @DisplayName("Explicit trancheId targets only that tranche: T2 -12000, T1 untouched")
    void explicitTrancheTargetIsHonored() {
        twoDrawnTranches();
        TransactionRequest request = repaymentRequest(new BigDecimal("12000.00"));
        request.setTrancheId(T2_ID);
        // Contradictory starting balance: only the derived reconciler lands on 78000
        Transaction tx =
                stubCreate(
                        request,
                        repaymentEntity(null, new BigDecimal("12000.00"), T2_ID),
                        "85000.00");

        transactionService.createTransaction(USER_ID, request);

        assertThat(tx.getTrancheId()).isEqualTo(T2_ID);
        // T1 remaining 10000 + T2 remaining (80000 - 12000)
        assertThat(lastSavedLiabilityBalance()).isEqualTo("78000.00");
    }

    // ---------- (c) overpay beyond remaining floors at zero ----------

    @Test
    @DisplayName("Overpay 12000 on a tranche with remaining 5000 clamps to remaining: balance 0")
    void overpayBeyondRemainingFloorsAtZero() {
        LiabilityTranche t1 =
                tranche(T1_ID, 1, new BigDecimal("5000.00"), LocalDate.now().minusDays(3));
        when(liabilityTrancheRepository.findByLiabilityIdAndUserId(LIABILITY_ID, USER_ID))
                .thenReturn(List.of(t1));
        when(liabilityRepository.findByIdAndUserId(LIABILITY_ID, USER_ID))
                .thenReturn(Optional.of(liabilityFixture("5000.00")));
        TransactionRequest request = repaymentRequest(new BigDecimal("12000.00"));
        when(accountRepository.findByIdAndUserId(ACCOUNT_ID, USER_ID))
                .thenReturn(Optional.of(accountFixture()));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
        when(transactionMapper.toResponse(any(Transaction.class)))
                .thenReturn(new TransactionResponse());
        when(liabilityRepository.save(any(Liability.class))).thenAnswer(inv -> inv.getArgument(0));
        Transaction tx = repaymentEntity(null, new BigDecimal("12000.00"), null);
        when(transactionMapper.toEntity(request)).thenReturn(tx);

        transactionService.createTransaction(USER_ID, request);

        assertThat(tx.getTrancheId()).isEqualTo(T1_ID);
        assertThat(lastSavedLiabilityBalance()).isEqualTo("0.00");
    }

    // ---------- (d) reconciler invariant: balance == SUM(drawn - allocated) exactly ----------

    @Test
    @DisplayName(
            "Reconciler assigns balance = SUM(drawn - allocated) exactly; FIFO skips fully "
                    + "allocated tranches")
    void reconcilerAssignsSumOfRemainingAndSkipsFullyAllocatedTranches() {
        twoDrawnTranches();
        // Prior repayments: T1 allocated 4000 + 7000 (capped at drawn 10000), T2 allocated 30000
        txTable.add(repaymentEntity(501L, new BigDecimal("4000.00"), T1_ID));
        txTable.add(repaymentEntity(502L, new BigDecimal("7000.00"), T1_ID));
        txTable.add(repaymentEntity(503L, new BigDecimal("30000.00"), T2_ID));

        TransactionRequest request = repaymentRequest(new BigDecimal("5000.00"));
        Transaction tx =
                stubCreate(
                        request,
                        repaymentEntity(null, new BigDecimal("5000.00"), null),
                        "70000.00");

        transactionService.createTransaction(USER_ID, request);

        // T1 remaining 0 -> skipped; FIFO targets T2; allocated(T2) = 35000 -> remaining 45000
        assertThat(tx.getTrancheId()).isEqualTo(T2_ID);
        assertThat(lastSavedLiabilityBalance()).isEqualTo("45000.00");
    }

    // ---------- (e) reversal: deleting a repayment drops allocations, balance restored ----------

    @Test
    @DisplayName("Deleting a REPAYMENT drops its allocation and restores the balance")
    void deletingRepaymentDropsAllocationAndRestoresBalance() {
        LiabilityTranche t1 =
                tranche(T1_ID, 1, new BigDecimal("10000.00"), LocalDate.now().minusDays(2));
        when(liabilityTrancheRepository.findByLiabilityIdAndUserId(LIABILITY_ID, USER_ID))
                .thenReturn(List.of(t1));
        Transaction existing = repaymentEntity(500L, new BigDecimal("10000.00"), T1_ID);
        txTable.add(existing);

        when(transactionRepository.findByIdAndUserId(500L, USER_ID))
                .thenReturn(Optional.of(existing));
        when(accountRepository.findByIdAndUserId(ACCOUNT_ID, USER_ID))
                .thenReturn(Optional.of(accountFixture()));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
        when(liabilityRepository.findByIdAndUserId(LIABILITY_ID, USER_ID))
                .thenReturn(Optional.of(liabilityFixture("7000.00")));
        when(liabilityRepository.save(any(Liability.class))).thenAnswer(inv -> inv.getArgument(0));

        transactionService.deleteTransaction(500L, USER_ID);

        assertThat(existing.getIsDeleted()).isTrue();
        // Allocated drops to zero (tx soft-deleted) -> remaining 10000 -> balance re-derived
        assertThat(lastSavedLiabilityBalance()).isEqualTo("10000.00");
    }

    // ---------- (f) explicit target must belong to the liability ----------

    @Test
    @DisplayName("An explicit trancheId from another liability is rejected")
    void explicitForeignTrancheTargetIsRejected() {
        twoDrawnTranches();
        TransactionRequest request = repaymentRequest(new BigDecimal("12000.00"));
        request.setTrancheId(999L);
        Transaction tx =
                stubCreate(
                        request,
                        repaymentEntity(null, new BigDecimal("12000.00"), 999L),
                        "90000.00");

        assertThatThrownBy(() -> transactionService.createTransaction(USER_ID, request))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessageContaining("999");

        assertThat(tx.getTrancheId()).isEqualTo(999L);
    }

    // ---------- (g) explicit target must be DRAWN ----------

    @Test
    @DisplayName("An explicit trancheId targeting a PLANNED tranche is rejected")
    void explicitTargetMustBeDrawn() {
        LiabilityTranche planned = tranche(T1_ID, 1, new BigDecimal("10000.00"), null);
        planned.setStatus(TrancheStatus.PLANNED);
        planned.setDrawnAmount(null);
        when(liabilityTrancheRepository.findByLiabilityIdAndUserId(LIABILITY_ID, USER_ID))
                .thenReturn(List.of(planned));
        TransactionRequest request = repaymentRequest(new BigDecimal("12000.00"));
        request.setTrancheId(T1_ID);
        stubCreate(request, repaymentEntity(null, new BigDecimal("12000.00"), T1_ID), "90000.00");

        assertThatThrownBy(() -> transactionService.createTransaction(USER_ID, request))
                .isInstanceOf(InvalidLiabilityStateException.class)
                .hasMessageContaining("DRAWN");
    }

    // ---------- (h) single-pass reconcile: exactly one balance write per repayment ----------

    @Test
    @DisplayName(
            "Healthy FIFO repayment create is a single-pass reconcile: exactly one liability save")
    void healthyFifoRepaymentSavesLiabilityExactlyOnce() {
        twoDrawnTranches();
        TransactionRequest request = repaymentRequest(new BigDecimal("2000.00"));
        stubCreate(request, repaymentEntity(null, new BigDecimal("2000.00"), null), "90000.00");

        transactionService.createTransaction(USER_ID, request);

        // One write = one reconcile: the allocator links the tranche first, then the single
        // reconcile inside the balance adjustment lands on the final invariant directly —
        // no intermediate re-derivations that would WARN about non-existent drift.
        verify(liabilityRepository, times(1)).save(any(Liability.class));
        assertThat(lastSavedLiabilityBalance()).isEqualTo("88000.00");
    }
}
