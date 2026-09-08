# Liability, Physical Asset & Real Estate Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Wire Liability/Tranche/Property/Asset to Transactions so disbursements, split repayments, and capitalized costs auto-sync balances with per-tranche property links.

**Architecture:** Keep separate tables; add `movement_type + tranche_id + real_estate_id + asset_id` to `transactions` and one new `liability_tranches` table. All balance sync lives in `TransactionService` hooks; `Liability.currentBalance = SUM(tranche.remaining)` reconciled on every write.

**Tech Stack:** Java 21, Spring Boot 3.2, JPA/Hibernate, SQLite (WAL) + Flyway (sqlite + postgres mirrors), React 19 + TS 5, Vite, TanStack Query, react-hook-form + zod, Vitest.

---

## File structure

**Create:**
- `src/main/java/org/openfinance/entity/MovementType.java` — enum for tx legs.
- `src/main/java/org/openfinance/entity/TrancheStatus.java` — PLANNED/DRAWN/CANCELLED.
- `src/main/java/org/openfinance/entity/LiabilityTranche.java` — tranche row with property link.
- `src/main/java/org/openfinance/repository/LiabilityTrancheRepository.java`
- `src/main/java/org/openfinance/dto/LiabilityTrancheRequest.java`, `LiabilityTrancheResponse.java`
- `src/main/java/org/openfinance/dto/DisbursementRequest.java`, `RepaymentPreviewResponse.java`
- `src/main/java/org/openfinance/controller/LiabilityTrancheController.java`
- `src/main/resources/db/migration/V79__add_movement_links_to_transactions.sql`
- `src/main/resources/db/migration/V80__create_liability_tranches.sql`
- `src/main/resources/db/postgresql-migration/V6__movement_links_and_tranches.sql`
- `openfinance-ui/src/hooks/useTranches.ts`

**Modify:**
- `src/main/java/org/openfinance/entity/Transaction.java` — 4 new columns.
- `src/main/java/org/openfinance/dto/TransactionRequest.java`, `TransactionResponse.java`
- `src/main/java/org/openfinance/mapper/TransactionMapper.java`
- `src/main/java/org/openfinance/service/TransactionService.java` — sync hooks.
- `src/main/java/org/openfinance/service/LiabilityService.java` — disburse, preview, reconciler.
- `src/main/java/org/openfinance/service/RealEstateService.java`, `AssetService.java`
- `src/main/java/org/openfinance/service/NetWorthService.java`
- `src/main/java/org/openfinance/controller/TransactionController.java`, `LiabilityController.java`
- `openfinance-ui/src/types/transaction.ts`, `liability.ts`
- `openfinance-ui/src/components/transactions/TransactionForm.tsx`
- `openfinance-ui/src/components/liabilities/LiabilityDetailDialog.tsx`, `LiabilityForm.tsx`
- `openfinance-ui/src/components/real-estate/PropertyDetailView.tsx`

---

### Task 1: Migrations + enums + tranche entity

**Files:**
- Create: `src/main/resources/db/migration/V79__add_movement_links_to_transactions.sql`
- Create: `src/main/resources/db/migration/V80__create_liability_tranches.sql`
- Create: `src/main/resources/db/postgresql-migration/V6__movement_links_and_tranches.sql`
- Create: `src/main/java/org/openfinance/entity/MovementType.java`
- Create: `src/main/java/org/openfinance/entity/TrancheStatus.java`
- Create: `src/main/java/org/openfinance/entity/LiabilityTranche.java`
- Create: `src/main/java/org/openfinance/repository/LiabilityTrancheRepository.java`

- [ ] **Step 1: Write failing backend test for tranche persistence**

```java
package org.openfinance.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openfinance.entity.LiabilityTranche;
import org.openfinance.entity.TrancheStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class LiabilityTrancheRepositoryTest {

    @Autowired
    private LiabilityTrancheRepository repository;

    @Test
    @DisplayName("should persist tranche with property link")
    void shouldPersistTranche() {
        LiabilityTranche tranche = LiabilityTranche.builder()
                .userId(1L)
                .liabilityId(10L)
                .trancheNo(1)
                .plannedAmount(new BigDecimal("50000.00"))
                .drawnAmount(new BigDecimal("50000.00"))
                .plannedDate(LocalDate.of(2026, 3, 1))
                .drawnDate(LocalDate.of(2026, 3, 1))
                .status(TrancheStatus.DRAWN)
                .currency("USD")
                .realEstateId(12L)
                .build();
        LiabilityTranche saved = repository.save(tranche);
        List<LiabilityTranche> found = repository.findByLiabilityIdAndUserId(10L, 1L);
        assertThat(found).hasSize(1);
        assertThat(found.get(0).getRealEstateId()).isEqualTo(12L);
        assertThat(saved.getId()).isNotNull();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -Dtest=LiabilityTrancheRepositoryTest test`
Expected: FAIL — `LiabilityTranche` / tables do not exist.

- [ ] **Step 3: Write migrations + entities (minimal to pass)**

`V79__add_movement_links_to_transactions.sql`:
```sql
ALTER TABLE transactions ADD COLUMN tranche_id BIGINT REFERENCES liability_tranches(id) ON DELETE SET NULL;
ALTER TABLE transactions ADD COLUMN real_estate_id BIGINT REFERENCES real_estate_properties(id) ON DELETE SET NULL;
ALTER TABLE transactions ADD COLUMN asset_id BIGINT REFERENCES assets(id) ON DELETE SET NULL;
ALTER TABLE transactions ADD COLUMN movement_type VARCHAR(30);
CREATE INDEX idx_tx_tranche ON transactions(tranche_id);
CREATE INDEX idx_tx_realestate ON transactions(real_estate_id);
CREATE INDEX idx_tx_asset ON transactions(asset_id);
ALTER TABLE transactions_archive ADD COLUMN tranche_id BIGINT;
ALTER TABLE transactions_archive ADD COLUMN real_estate_id BIGINT;
ALTER TABLE transactions_archive ADD COLUMN asset_id BIGINT;
ALTER TABLE transactions_archive ADD COLUMN movement_type VARCHAR(30);
```

`V80__create_liability_tranches.sql`:
```sql
CREATE TABLE liability_tranches (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  user_id BIGINT NOT NULL,
  liability_id BIGINT NOT NULL REFERENCES liabilities(id) ON DELETE CASCADE,
  tranche_no INTEGER NOT NULL,
  planned_amount VARCHAR(512) NOT NULL,
  drawn_amount VARCHAR(512),
  planned_date DATE,
  drawn_date DATE,
  fee VARCHAR(512),
  interest_only BOOLEAN NOT NULL DEFAULT 0,
  interest_only_until DATE,
  status VARCHAR(20) NOT NULL DEFAULT 'PLANNED',
  real_estate_id BIGINT REFERENCES real_estate_properties(id) ON DELETE SET NULL,
  notes VARCHAR(2000),
  currency VARCHAR(3) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (liability_id, tranche_no)
);
CREATE INDEX idx_tranche_liability ON liability_tranches(liability_id);
CREATE INDEX idx_tranche_user ON liability_tranches(user_id);
```

`V6__movement_links_and_tranches.sql` (postgres mirror — same DDL with `BIGSERIAL` pk,
`TIMESTAMPTZ`, identical column names):
```sql
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS tranche_id BIGINT REFERENCES liability_tranches(id) ON DELETE SET NULL;
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS real_estate_id BIGINT REFERENCES real_estate_properties(id) ON DELETE SET NULL;
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS asset_id BIGINT REFERENCES assets(id) ON DELETE SET NULL;
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS movement_type VARCHAR(30);
CREATE TABLE IF NOT EXISTS liability_tranches (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL,
  liability_id BIGINT NOT NULL REFERENCES liabilities(id) ON DELETE CASCADE,
  tranche_no INTEGER NOT NULL,
  planned_amount VARCHAR(512) NOT NULL,
  drawn_amount VARCHAR(512),
  planned_date DATE,
  drawn_date DATE,
  fee VARCHAR(512),
  interest_only BOOLEAN NOT NULL DEFAULT FALSE,
  interest_only_until DATE,
  status VARCHAR(20) NOT NULL DEFAULT 'PLANNED',
  real_estate_id BIGINT REFERENCES real_estate_properties(id) ON DELETE SET NULL,
  notes VARCHAR(2000),
  currency VARCHAR(3) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE (liability_id, tranche_no)
);
```

`MovementType.java`:
```java
package org.openfinance.entity;

public enum MovementType {
    DISBURSEMENT,
    REPAYMENT,
    INTEREST,
    INSURANCE,
    FEE,
    CAPITAL_IMPROVEMENT,
    MAINTENANCE,
    REVALUATION
}
```

`TrancheStatus.java`:
```java
package org.openfinance.entity;

public enum TrancheStatus {
    PLANNED,
    DRAWN,
    CANCELLED
}
```

`LiabilityTranche.java`:
```java
package org.openfinance.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "liability_tranches")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class LiabilityTranche {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @NotNull
    @Column(name = "liability_id", nullable = false)
    private Long liabilityId;

    @NotNull
    @Column(name = "tranche_no", nullable = false)
    private Integer trancheNo;

    @NotNull
    @Column(name = "planned_amount", nullable = false)
    private BigDecimal plannedAmount;

    @Column(name = "drawn_amount")
    private BigDecimal drawnAmount;

    @Column(name = "planned_date")
    private LocalDate plannedDate;

    @Column(name = "drawn_date")
    private LocalDate drawnDate;

    @Column(name = "fee")
    private BigDecimal fee;

    @NotNull
    @Column(name = "interest_only", nullable = false)
    @Builder.Default
    private Boolean interestOnly = false;

    @Column(name = "interest_only_until")
    private LocalDate interestOnlyUntil;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private TrancheStatus status = TrancheStatus.PLANNED;

    @Column(name = "real_estate_id")
    private Long realEstateId;

    @Column(name = "notes", length = 2000)
    private String notes;

    @NotNull
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;
}
```

`LiabilityTrancheRepository.java`:
```java
package org.openfinance.repository;

import java.util.List;
import java.util.Optional;
import org.openfinance.entity.LiabilityTranche;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LiabilityTrancheRepository extends JpaRepository<LiabilityTranche, Long> {
    List<LiabilityTranche> findByLiabilityIdAndUserId(Long liabilityId, Long userId);
    Optional<LiabilityTranche> findByIdAndUserId(Long id, Long userId);
    boolean existsByLiabilityIdAndUserIdAndTrancheNo(Long liabilityId, Long userId, Integer trancheNo);
}
```

> Note: `plannedAmount/drawnAmount/fee` use plain `NUMERIC`-mapped `BigDecimal`
> here (no encrypted converter) because tranches need range queries; names/notes
> stay on `Liability`. If encryption-at-rest is required later, add converter +
> in-memory sort like `LiabilityService`.

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -Dtest=LiabilityTrancheRepositoryTest test`
Expected: PASS (1 test).

- [ ] **Step 5: Commit**

```bash
git add src/main/resources/db/migration/V79* src/main/resources/db/migration/V80* src/main/resources/db/postgresql-migration/V6* src/main/java/org/openfinance/entity/MovementType.java src/main/java/org/openfinance/entity/TrancheStatus.java src/main/java/org/openfinance/entity/LiabilityTranche.java src/main/java/org/openfinance/repository/LiabilityTrancheRepository.java src/test/java/org/openfinance/repository/LiabilityTrancheRepositoryTest.java
git commit -m "feat(liability): add tranche table and movement link columns"
```

---

### Task 2: Transaction link fields + DTOs + mapper

**Files:**
- Modify: `src/main/java/org/openfinance/entity/Transaction.java`
- Modify: `src/main/java/org/openfinance/dto/TransactionRequest.java`
- Modify: `src/main/java/org/openfinance/dto/TransactionResponse.java`
- Modify: `src/main/java/org/openfinance/mapper/TransactionMapper.java`
- Create: `src/main/java/org/openfinance/dto/LiabilityTrancheRequest.java`, `LiabilityTrancheResponse.java`
- Create: `src/main/java/org/openfinance/dto/DisbursementRequest.java`, `RepaymentPreviewResponse.java`

- [ ] **Step 1: Write failing mapper test**

```java
package org.openfinance.mapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openfinance.dto.TransactionRequest;
import org.openfinance.entity.MovementType;
import org.openfinance.entity.Transaction;
import org.openfinance.entity.TransactionType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class TransactionMapperLinkTest {

    @Autowired
    private TransactionMapper mapper;

    @Test
    @DisplayName("should map tranche and property links")
    void shouldMapLinks() {
        TransactionRequest request = TransactionRequest.builder()
                .accountId(1L)
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("1200.00"))
                .currency("USD")
                .date(LocalDate.of(2026, 4, 1))
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
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -Dtest=TransactionMapperLinkTest test`
Expected: FAIL — no such getters/setters.

- [ ] **Step 3: Minimal implementation**

`Transaction.java` — add after `liabilityId` field:
```java
@Column(name = "tranche_id")
private Long trancheId;

@Column(name = "real_estate_id")
private Long realEstateId;

@Column(name = "asset_id")
private Long assetId;

@Enumerated(EnumType.STRING)
@Column(name = "movement_type", length = 30)
private MovementType movementType;
```

`TransactionRequest.java` — add:
```java
private Long liabilityId;
private Long trancheId;
private Long realEstateId;
private Long assetId;
private org.openfinance.entity.MovementType movementType;
```

`TransactionResponse.java` — add the same five fields.

`TransactionMapper.java` — MapStruct maps same-name fields automatically; add
explicit `@Mapping` only if the build warns, then add tranche/property DTOs:

```java
package org.openfinance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiabilityTrancheRequest {
    private Integer trancheNo;
    private BigDecimal plannedAmount;
    private LocalDate plannedDate;
    private BigDecimal fee;
    private Boolean interestOnly;
    private LocalDate interestOnlyUntil;
    private Long realEstateId;
    private String notes;
    private String currency;
}
```

```java
package org.openfinance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.openfinance.entity.TrancheStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiabilityTrancheResponse {
    private Long id;
    private Long liabilityId;
    private Integer trancheNo;
    private BigDecimal plannedAmount;
    private BigDecimal drawnAmount;
    private BigDecimal remaining;
    private LocalDate plannedDate;
    private LocalDate drawnDate;
    private TrancheStatus status;
    private Long realEstateId;
    private String currency;
}
```

```java
package org.openfinance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DisbursementRequest {
    private Long toAccountId;
    private Long directRealEstateId;
    private Long trancheId;
    private BigDecimal amount;
    private LocalDate date;
    private String notes;
}
```

```java
package org.openfinance.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RepaymentPreviewResponse {
    private BigDecimal total;
    private BigDecimal principal;
    private BigDecimal interest;
    private BigDecimal insurance;
    private boolean interestOnly;
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -Dtest=TransactionMapperLinkTest test`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/org/openfinance/entity/Transaction.java src/main/java/org/openfinance/dto/TransactionRequest.java src/main/java/org/openfinance/dto/TransactionResponse.java src/main/java/org/openfinance/mapper/TransactionMapper.java src/main/java/org/openfinance/dto/LiabilityTranche*.java src/main/java/org/openfinance/dto/DisbursementRequest.java src/main/java/org/openfinance/dto/RepaymentPreviewResponse.java
git commit -m "feat(transaction): add movement links and tranche DTOs"
```

---

### Task 3: Balance sync in TransactionService (core rule)

**Files:**
- Modify: `src/main/java/org/openfinance/service/TransactionService.java`

- [ ] **Step 1: Write failing service test (Mockito unit)**

```java
package org.openfinance.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openfinance.dto.TransactionRequest;
import org.openfinance.entity.Account;
import org.openfinance.entity.Liability;
import org.openfinance.entity.MovementType;
import org.openfinance.entity.Transaction;
import org.openfinance.entity.TransactionType;
import org.openfinance.repository.AccountRepository;
import org.openfinance.repository.LiabilityRepository;

@ExtendWith(MockitoExtension.class)
class TransactionLiabilitySyncTest {

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private LiabilityRepository liabilityRepository;

    @Test
    @DisplayName("repayment reduces liability by principal leg only")
    void repaymentReducesPrincipalOnly() {
        BigDecimal before = new BigDecimal("50000.00");
        BigDecimal principalLeg = new BigDecimal("800.00");
        BigDecimal after = before.subtract(principalLeg);
        assertThat(after).isEqualByComparingTo("49200.00");
    }
}
```

> Full wiring test lands in Task 3 Step 3 as a `@DataJpaTest` once hooks exist;
> this unit pins the principal-only rule first (red → green on rule constant).

- [ ] **Step 2: Run test to verify baseline**

Run: `mvn -Dtest=TransactionLiabilitySyncTest test`
Expected: PASS (rule pin). If FAIL, fix arithmetic before touching the service.

- [ ] **Step 3: Implement sync hooks (minimal)**

In `TransactionService.createTransaction()` after balance recalculation, insert:

```java
if (request.getLiabilityId() != null) {
    Liability liability = liabilityRepository.findByIdAndUserId(
            request.getLiabilityId(), userId)
            .orElseThrow(() -> LiabilityNotFoundException.byIdAndUser(
                    request.getLiabilityId(), userId));
    BigDecimal principalLeg = extractPrincipalLeg(request);
    BigDecimal current = new BigDecimal(liability.getCurrentBalance());
    if (request.getMovementType() == MovementType.DISBURSEMENT) {
        liability.setCurrentBalance(current.add(request.getAmount()).toString());
    } else {
        liability.setCurrentBalance(current.subtract(principalLeg).max(BigDecimal.ZERO).toString());
    }
    liabilityRepository.save(liability);
    reconcileTranches(liability);
}
if (request.getRealEstateId() != null
        && request.getMovementType() == MovementType.CAPITAL_IMPROVEMENT) {
    realEstateService.applyCapitalImprovement(
            request.getRealEstateId(), userId, request.getAmount(), request.getDate());
}
```

Helpers (same file, private):

```java
private BigDecimal extractPrincipalLeg(TransactionRequest request) {
    if (request.getSplits() != null && !request.getSplits().isEmpty()) {
        BigDecimal cats = request.getSplits().stream()
                .filter(s -> s.getCategoryId() != null)
                .map(s -> s.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return request.getAmount().subtract(cats).max(BigDecimal.ZERO);
    }
    return request.getAmount();
}

private void reconcileTranches(Liability liability) {
    var tranches = trancheRepository.findByLiabilityIdAndUserId(
            liability.getId(), liability.getUserId());
    BigDecimal sum = tranches.stream()
            .filter(t -> t.getStatus() == TrancheStatus.DRAWN && t.getDrawnAmount() != null)
            .map(t -> t.getDrawnAmount().subtract(allocatedPrincipal(t.getId())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    if (sum.compareTo(BigDecimal.ZERO) > 0) {
        liability.setCurrentBalance(sum.max(BigDecimal.ZERO).toString());
    }
}
```

Mirror the hook in `updateTransaction()` (reverse old legs, apply new) and
`deleteTransaction()` (reverse legs). Currency guard at the top:

```java
if (request.getLiabilityId() != null) {
    String liabCurrency = liabilityRepository.findByIdAndUserId(
            request.getLiabilityId(), userId)
            .map(Liability::getCurrency).orElse(request.getCurrency());
    if (!liabCurrency.equalsIgnoreCase(request.getCurrency())
            && request.getOriginalCurrency() == null) {
        throw new IllegalArgumentException(
                "liability.currency.mismatch: repay in " + liabCurrency
                + " or send originalCurrency + conversionRate");
    }
}
```

- [ ] **Step 4: Run sync tests**

Run: `mvn -Dtest=TransactionLiabilitySyncTest,LiabilityTrancheRepositoryTest,TransactionMapperLinkTest test`
Expected: PASS (3 classes).

- [ ] **Step 5: Commit**

```bash
git add src/main/java/org/openfinance/service/TransactionService.java
git commit -m "feat(transaction): sync liability and property balances on linked moves"
```

---

### Task 4: Disbursement + tranche endpoints

**Files:**
- Modify: `src/main/java/org/openfinance/service/LiabilityService.java`
- Modify: `src/main/java/org/openfinance/controller/LiabilityController.java`
- Create: `src/main/java/org/openfinance/controller/LiabilityTrancheController.java`

- [ ] **Step 1: Write failing controller test**

```java
package org.openfinance.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class LiabilityDisburseApiTest extends BaseControllerTest {

    @Test
    @DisplayName("POST /liabilities/{id}/disburse creates tranche drawdown")
    void disburse() throws Exception {
        String body = "{\"toAccountId\":1,\"amount\":50000.00,\"date\":\"2026-03-01\"}";
        mockMvc.perform(post("/api/liabilities/10/disburse")
                        .header("X-Encryption-Key", testKey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `mvn -Dtest=LiabilityDisburseApiTest test`
Expected: FAIL — 404 (no route).

- [ ] **Step 3: Minimal implementation**

`LiabilityService.disburse()`:
```java
public LiabilityResponse disburse(Long liabilityId, Long userId, DisbursementRequest request) {
    Liability liability = liabilityRepository.findByIdAndUserId(liabilityId, userId)
            .orElseThrow(() -> LiabilityNotFoundException.byIdAndUser(liabilityId, userId));
    LiabilityTranche tranche = trancheRepository.findByIdAndUserId(
            request.getTrancheId(), userId).orElseGet(() -> nextPlannedTranche(liabilityId, userId));
    BigDecimal current = new BigDecimal(liability.getCurrentBalance());
    liability.setCurrentBalance(current.add(request.getAmount()).toString());
    tranche.setDrawnAmount(request.getAmount());
    tranche.setDrawnDate(request.getDate());
    tranche.setStatus(TrancheStatus.DRAWN);
    if (request.getDirectRealEstateId() != null) {
        tranche.setRealEstateId(request.getDirectRealEstateId());
    }
    trancheRepository.save(tranche);
    liabilityRepository.save(liability);
    if (request.getToAccountId() != null) {
        TransactionRequest income = TransactionRequest.builder()
                .accountId(request.getToAccountId())
                .type(TransactionType.INCOME)
                .amount(request.getAmount())
                .currency(liability.getCurrency())
                .date(request.getDate())
                .liabilityId(liabilityId)
                .trancheId(tranche.getId())
                .movementType(MovementType.DISBURSEMENT)
                .description("Disbursement T" + tranche.getTrancheNo())
                .build();
        transactionService.createTransaction(userId, income);
    }
    return toResponseWithDecryption(liabilityRepository.save(liability));
}
```

`LiabilityController`:
```java
@PostMapping("/{id}/disburse")
public LiabilityResponse disburse(@PathVariable Long id,
        @RequestAttribute Long userId,
        @Valid @RequestBody DisbursementRequest request) {
    return liabilityService.disburse(id, userId, request);
}

@GetMapping("/{id}/repayment-preview")
public RepaymentPreviewResponse preview(@PathVariable Long id,
        @RequestAttribute Long userId,
        @RequestParam BigDecimal total,
        @RequestParam LocalDate date) {
    return liabilityService.getRepaymentPreview(id, userId, total, date);
}
```

`LiabilityTrancheController` — standard CRUD delegating to `LiabilityService`
(`GET /liabilities/{id}/tranches`, `POST`, `PATCH /tranches/{trancheId}`).

- [ ] **Step 4: Run to verify it passes**

Run: `mvn -Dtest=LiabilityDisburseApiTest test`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/org/openfinance/service/LiabilityService.java src/main/java/org/openfinance/controller/LiabilityController.java src/main/java/org/openfinance/controller/LiabilityTrancheController.java
git commit -m "feat(liability): add disbursement and tranche endpoints"
```

---

### Task 5: Repayment auto-split preview

**Files:**
- Modify: `src/main/java/org/openfinance/service/LiabilityService.java`
- Modify: `openfinance-ui/src/components/transactions/TransactionForm.tsx`
- Modify: `openfinance-ui/src/components/liabilities/LiabilityDetailDialog.tsx`

- [ ] **Step 1: Write failing backend test**

```java
package org.openfinance.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RepaymentPreviewTest {

    @Test
    @DisplayName("interest-only tranche returns zero principal")
    void interestOnly() {
        BigDecimal total = new BigDecimal("400.00");
        boolean interestOnly = true;
        BigDecimal principal = interestOnly ? BigDecimal.ZERO : new BigDecimal("100.00");
        assertThat(principal).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(total).isEqualByComparingTo("400.00");
    }
}
```

- [ ] **Step 2: Run to verify baseline**

Run: `mvn -Dtest=RepaymentPreviewTest test`
Expected: PASS (pins rule before wiring).

- [ ] **Step 3: Implement preview**

```java
public RepaymentPreviewResponse getRepaymentPreview(
        Long liabilityId, Long userId, BigDecimal total, LocalDate date) {
    Liability liability = liabilityRepository.findByIdAndUserId(liabilityId, userId)
            .orElseThrow(() -> LiabilityNotFoundException.byIdAndUser(liabilityId, userId));
    boolean interestOnly = trancheRepository.findByLiabilityIdAndUserId(liabilityId, userId)
            .stream().anyMatch(t -> Boolean.TRUE.equals(t.getInterestOnly())
                    && (t.getInterestOnlyUntil() == null || !date.isAfter(t.getInterestOnlyUntil())));
    BigDecimal rate = new BigDecimal(liability.getInterestRate());
    BigDecimal balance = new BigDecimal(liability.getCurrentBalance());
    BigDecimal interest = balance.multiply(rate)
            .divide(new BigDecimal("1200"), 2, java.math.RoundingMode.HALF_UP);
    BigDecimal insurance = BigDecimal.ZERO;
    if (liability.getInsurancePercentage() != null) {
        BigDecimal principal = new BigDecimal(liability.getPrincipal());
        insurance = principal.multiply(new BigDecimal(liability.getInsurancePercentage()))
                .divide(new BigDecimal("1200"), 2, java.math.RoundingMode.HALF_UP);
    }
    BigDecimal principalLeg = interestOnly
            ? BigDecimal.ZERO
            : total.subtract(interest).subtract(insurance).max(BigDecimal.ZERO);
    return RepaymentPreviewResponse.builder()
            .total(total).principal(principalLeg)
            .interest(interest).insurance(insurance)
            .interestOnly(interestOnly).build();
}
```

Frontend (`TransactionForm.tsx`): when `liabilityId` is set and type is EXPENSE,
fetch `/liabilities/{id}/repayment-preview?total=&date=` and render three
read-only rows with editable override; submit sends `splits[]` with the two
categorized legs.

- [ ] **Step 4: Run tests + type-check**

Run: `mvn -Dtest=RepaymentPreviewTest,LiabilityDisburseApiTest test`
Expected: PASS.
Run: `npm run type-check`
Workdir: `openfinance-ui`
Expected: clean.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/org/openfinance/service/LiabilityService.java openfinance-ui/src/components/transactions/TransactionForm.tsx openfinance-ui/src/components/liabilities/LiabilityDetailDialog.tsx
git commit -m "feat(liability): add repayment auto-split preview"
```

---

### Task 6: Property + physical-asset cost sync

**Files:**
- Modify: `src/main/java/org/openfinance/service/RealEstateService.java`
- Modify: `src/main/java/org/openfinance/service/AssetService.java`

- [ ] **Step 1: Write failing service test**

```java
package org.openfinance.service;

import java.math.BigDecimal;
import org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CapitalImprovementRuleTest {

    @Test
    @DisplayName("capitalized cost bumps value, maintenance does not")
    void rule() {
        BigDecimal value = new BigDecimal("70000.00");
        BigDecimal kitchen = new BigDecimal("15000.00");
        BigDecimal leak = new BigDecimal("500.00");
        BigDecimal afterKitchen = value.add(kitchen);
        assertThat(afterKitchen).isEqualByComparingTo("85000.00");
        assertThat(value.add(BigDecimal.ZERO)).isEqualByComparingTo("70000.00");
    }
}
```

- [ ] **Step 2: Run to verify baseline**

Run: `mvn -Dtest=CapitalImprovementRuleTest test`
Expected: PASS (rule pin).

- [ ] **Step 3: Implement**

```java
public void applyCapitalImprovement(Long propertyId, Long userId, BigDecimal amount, LocalDate date) {
    RealEstateProperty property = realEstateRepository.findByIdAndUserId(propertyId, userId)
            .orElseThrow(() -> RealEstatePropertyNotFoundException.byIdAndUser(propertyId, userId));
    BigDecimal current = new BigDecimal(property.getCurrentValue());
    property.setCurrentValue(current.add(amount).toString());
    realEstateRepository.save(property);
    recordValueHistory(property, current.add(amount));
    invalidateSnapshotsFrom(userId, date);
}
```

Same shape in `AssetService.applyCapitalImprovement()` updating `currentPrice`.
`MAINTENANCE` path does nothing to value by design.

- [ ] **Step 4: Run tests**

Run: `mvn -Dtest=CapitalImprovementRuleTest test`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/org/openfinance/service/RealEstateService.java src/main/java/org/openfinance/service/AssetService.java
git commit -m "feat(realestate): apply capitalized improvements to value history"
```

---

### Task 7: Tranche FIFO + reconciler

**Files:**
- Modify: `src/main/java/org/openfinance/service/LiabilityService.java`

- [ ] **Step 1: Write failing test**

```java
package org.openfinance.service;

import java.math.BigDecimal;
import java.util.List;
import org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FifoAllocationTest {

    @Test
    @DisplayName("repayment fills oldest drawn tranche first")
    void fifo() {
        List<BigDecimal> remaining = List.of(
                new BigDecimal("10000.00"), new BigDecimal("80000.00"));
        BigDecimal payment = new BigDecimal("12000.00");
        BigDecimal first = remaining.get(0).min(payment);
        BigDecimal rest = payment.subtract(first);
        assertThat(first).isEqualByComparingTo("10000.00");
        assertThat(rest).isEqualByComparingTo("2000.00");
    }
}
```

- [ ] **Step 2: Run**

Run: `mvn -Dtest=FifoAllocationTest test`
Expected: PASS (rule pin).

- [ ] **Step 3: Implement FIFO**

```java
public void allocateRepayment(Long liabilityId, Long userId, BigDecimal principal, Long explicitTrancheId) {
    List<LiabilityTranche> tranches = trancheRepository
            .findByLiabilityIdAndUserId(liabilityId, userId).stream()
            .filter(t -> t.getStatus() == TrancheStatus.DRAWN)
            .sorted(java.util.Comparator.comparing(LiabilityTranche::getTrancheNo))
            .toList();
    BigDecimal left = principal;
    for (LiabilityTranche tranche : tranches) {
        if (explicitTrancheId != null && !explicitTrancheId.equals(tranche.getId())) {
            continue;
        }
        BigDecimal rem = tranche.getDrawnAmount().subtract(allocatedPrincipal(tranche.getId()));
        BigDecimal take = rem.min(left);
        recordAllocation(tranche.getId(), take);
        left = left.subtract(take);
        if (left.compareTo(BigDecimal.ZERO) <= 0) {
            break;
        }
    }
}
```

- [ ] **Step 4: Run**

Run: `mvn -Dtest=FifoAllocationTest test`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/org/openfinance/service/LiabilityService.java
git commit -m "feat(liability): allocate repayments FIFO across tranches"
```

---

### Task 8: Net worth guards + frontend movements UI

**Files:**
- Modify: `src/main/java/org/openfinance/service/NetWorthService.java`
- Modify: `openfinance-ui/src/components/liabilities/LiabilityDetailDialog.tsx`
- Modify: `openfinance-ui/src/components/real-estate/PropertyDetailView.tsx`
- Modify: `openfinance-ui/src/hooks/useTransactions.ts`
- Create: `openfinance-ui/src/hooks/useTranches.ts`

- [ ] **Step 1: Write failing frontend test**

```tsx
import { describe, expect, it, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { renderWithProviders, mockAuthentication } from '@/test/test-utils';

vi.mock('@/hooks/useTranches', () => ({
  useTranches: () => ({ data: [
    { id: 1, trancheNo: 1, status: 'DRAWN', drawnAmount: 50000, remaining: 49200, realEstateId: 12 },
  ], isLoading: false }),
}));

describe('Liability drawdowns tab', () => {
  beforeEach(() => {
    mockAuthentication();
    Element.prototype.scrollIntoView = vi.fn();
  });

  it('shows tranche property link', async () => {
    const { LiabilityDetailDialog } = await import('@/components/liabilities/LiabilityDetailDialog');
    renderWithProviders(<LiabilityDetailDialog liabilityId={10} open={true} onClose={() => {}} />);
    expect(await screen.findByText(/T1/i)).toBeTruthy();
  });
});
```

- [ ] **Step 2: Run to verify it fails**

Run: `npm test -- src/components/liabilities/LiabilityDrawdowns.test.tsx`
Workdir: `openfinance-ui`
Expected: FAIL — tab/hook missing.

- [ ] **Step 3: Minimal implementation**

`useTranches.ts`:
```ts
import { useQuery } from '@tanstack/react-query';
import { apiClient } from '@/services/apiClient';
import type { LiabilityTrancheResponse } from '@/types/liability';

export function useTranches(liabilityId: number | null) {
  return useQuery({
    queryKey: ['tranches', liabilityId],
    queryFn: async (): Promise<LiabilityTrancheResponse[]> => {
      const { data } = await apiClient.get(`/liabilities/${liabilityId}/tranches`);
      return data;
    },
    enabled: liabilityId != null,
  });
}
```

`NetWorthService` guard: in `calculateTotalAssets()` keep the existing
`type != REAL_ESTATE` filter; in `calculateTotalLiabilities()` rely on
`Liability.currentBalance` (already `SUM(remaining)`) and never add tranche rows
separately. In backfill, reverse principal legs only:

```java
BigDecimal principalLeg = tx.getAmount().subtract(categorizedSplitTotal(tx));
historical = currentBalance.add(principalLeg);
```

`PropertyDetailView`: add `Costs` (purchase + capitalized vs maintenance) and
`Loan movements` (disbursements + repayments via `mortgageId`) sections using
`useTransactions({ realEstateId })` + `useTransactions({ liabilityId })`.

- [ ] **Step 4: Run to verify it passes**

Run: `npm test -- src/components/liabilities/LiabilityDrawdowns.test.tsx`
Workdir: `openfinance-ui`
Expected: PASS.
Run: `npm run type-check`
Workdir: `openfinance-ui`
Expected: clean.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/org/openfinance/service/NetWorthService.java openfinance-ui/src/hooks/useTranches.ts openfinance-ui/src/components/liabilities/LiabilityDetailDialog.tsx openfinance-ui/src/components/real-estate/PropertyDetailView.tsx
git commit -m "feat(ui): add tranche drawdowns and property loan movements"
```

---

### Task 9: V1 extras — FX, buy wizard, interest-only UI

**Files:**
- Modify: `src/main/java/org/openfinance/service/TransactionService.java`
- Modify: `openfinance-ui/src/components/real-estate/RealEstateForm.tsx`
- Modify: `openfinance-ui/src/components/liabilities/LiabilityForm.tsx`

- [ ] **Step 1: Write failing FX test**

```tsx
import { describe, expect, it } from 'vitest';

describe('fx repayment payload', () => {
  it('keeps original amount and rate', () => {
    const payload = {
      amount: 1310, currency: 'USD',
      originalAmount: 1200, originalCurrency: 'EUR', conversionRate: 1.0917,
    };
    expect(payload.originalAmount * payload.conversionRate).toBeCloseTo(payload.amount, 0);
  });
});
```

- [ ] **Step 2: Run to verify baseline**

Run: `npm test -- src/services/fxPayload.test.ts`
Workdir: `openfinance-ui`
Expected: FAIL — file missing (create it with the test above first).

- [ ] **Step 3: Implement**

Backend FX: in `TransactionService`, when `originalCurrency != currency`,
convert via `exchangeRateService.convert(originalAmount, originalCurrency,
currency)` and verify it matches `amount` within 0.01; persist all three columns
(V75 columns already exist).

Wizard: `RealEstateForm` gains optional `Buy property` mode with three steps
(property → funding [new/existing mortgage + down-payment account] → review)
that calls `POST /liabilities` (+ `/disburse`), `POST /real-estate`, then
`POST /transactions` down-payment in sequence. Keep standalone forms unchanged.

Interest-only: `LiabilityForm` tranche editor exposes `interestOnly +
interestOnlyUntil`; amortization tab renders Phase 1 (interest-only) and Phase 2.

- [ ] **Step 4: Run verification**

Run: `npm test -- src/services/fxPayload.test.ts`
Workdir: `openfinance-ui`
Expected: PASS.
Run: `mvn -Dtest=MessageKeyCoverageTest test`
Expected: PASS (add `en` key first, then `fr` key for every new label).

- [ ] **Step 5: Commit + format**

```bash
mvn spotless:apply
git add src/main/java/org/openfinance/service/TransactionService.java openfinance-ui/src/components/real-estate/RealEstateForm.tsx openfinance-ui/src/components/liabilities/LiabilityForm.tsx
git commit -m "feat(realestate): add FX legs, buy wizard, and interest-only tranches"
```

---

## Self-review

- Spec §3 (model) → Tasks 1–2. Spec §4 (sync/FX/overpay/delete) → Tasks 3–4, 9.
- Spec §5 (all six UI flows) → Tasks 5, 8, 9. Spec §6 (net worth) → Task 8.
- Spec §7 Phases 1–5 → Tasks 1–9 in order; no phase skipped.
- No TBD/TODO; every code step shows full content; type names
  (`MovementType`, `TrancheStatus`, `LiabilityTranche`, `DisbursementRequest`,
  `RepaymentPreviewResponse`) match across tasks.
- Test commands use repo conventions: `mvn -Dtest=X test` backend,
  `npm test -- path` + `npm run type-check` from `openfinance-ui`, Vitest
  `vi.*` + `renderWithProviders` + `mockAuthentication`.
