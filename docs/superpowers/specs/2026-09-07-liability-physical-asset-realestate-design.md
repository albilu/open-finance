# Liability, Physical Asset & Real Estate Management — Design

Date: 2026-09-07
Status: Approved (text brainstorm, no visual companion)
Scope: V1 includes FX, buy wizard, and interest-only phase (user: "Everything in V1").

## 1. Context & gaps

Open-finance keeps `Account`, `Liability`, `Asset`, `RealEstateProperty` as separate
tables with loose links:

- `Transaction.liabilityId` exists (EXPENSE only) but `TransactionService` never
  updates `Liability.currentBalance` — link is display-only.
- No `realEstateId` / `assetId` on `Transaction` — property/asset expenses unlinked.
- No disbursement concept — creating a Liability creates no Transaction.
- `RealEstateProperty.mortgageId` is a single FK — no tranche history.
- `NetWorthService.backfill` reverses `liabilityId` payments but balances drift
  because nothing auto-syncs.

Goal: disbursements tracked, repayments tracked (splitable),
property/asset expenses tracked (capitalized or not), loan↔property movements
tracked — with `Account` staying the cash ledger and entities staying separate.

## 2. Decisions (user answers)

1. Core model: **keep separate + link** (not unified ledger, not shadow accounts).
2. Disbursement: **both supported** — auto-create on Liability create + manual link later.
3. Repayment split: **auto-split rule** from amortization schedule, user-overridable.
   Principal leg reduces debt; interest/insurance legs are categories.
4. Property expense: **flag per expense** — `CAPITAL_IMPROVEMENT` vs `MAINTENANCE`.
5. Acquisition: **separate steps** as baseline (wizard added in V1 Phase 5 as option).
6. Tranches: **full tranche object + property link**.

## 3. Architecture / data model

Keep all tables. Two changes:

### 3.1 `transactions` — 4 new columns (Flyway)

- `tranche_id FK nullable → liability_tranches`
- `real_estate_id FK nullable → real_estate_properties`
- `asset_id FK nullable → assets`
- `movement_type VARCHAR`: `DISBURSEMENT | REPAYMENT | INTEREST | INSURANCE |
  FEE | CAPITAL_IMPROVEMENT | MAINTENANCE | REVALUATION`

Repayment shape: parent `EXPENSE` from cash account with `liabilityId (+trancheId)`
+ splits: principal (no category, debt-reducing) / interest (category) /
insurance (category). Example 1,200 = 800 / 300 / 100.

### 3.2 New `liability_tranches`

`id, userId, liabilityId, trancheNo, plannedAmount, drawnAmount,
plannedDate, drawnDate, fee, interestOnly BOOLEAN DEFAULT false,
interestOnlyUntil DATE nullable, status (PLANNED/DRAWN/CANCELLED),
realEstateId nullable FK, notes, currency`.

- Single mortgage 50k = one tranche `T1 DRAWN 50k → House#12`.
- Construction 200k = `T1 50k DRAWN foundation → House`,
  `T2 80k DRAWN walls → House`, `T3 70k PLANNED`.
- Invariant: `Liability.currentBalance = SUM(tranche.remaining)` where
  `remaining = drawnAmount − allocatedPrincipal`. Reconciled on every write.

### 3.3 Cost rule

- `CAPITAL_IMPROVEMENT` (15k kitchen): `Checking −15k`,
  `Property.currentValue += 15k` + `RealEstateValueHistory` insert. Net worth up.
- `MAINTENANCE` (500 leak, 2k tax): account move only, value unchanged.
- Physical assets (`Asset.type` VEHICLE/JEWELRY/COLLECTIBLE/ELECTRONICS/FURNITURE):
  same via `assetId`; improvement bumps `currentPrice` basis; depreciation
  (`getDepreciatedValue()`) stays a read-only estimate.

## 4. Sync rules

All sync lives in `TransactionService` create/update/delete hooks:

- `DISBURSEMENT → Checking`: `Liability += amount`, `Checking += amount`,
  `Tranche.drawnAmount = amount, status=DRAWN`.
- `DISBURSEMENT_DIRECT → seller/property`: `Liability += amount`, no account leg,
  `Property.purchasePrice/currentValue += amount` when `realEstateId` set.
- `REPAYMENT`: `Checking −= total`, `Liability −= principal leg only`.
- Delete/reverse of a linked Transaction reverses the same legs.
- `Liability.currentBalance` becomes read-only in the form when linked txs exist
  (show `View linked movements` instead).
- FX (V1): same-currency fast path; cross-currency uses
  `ExchangeRateService.convert()` and stores `originalAmount/originalCurrency/
  conversionRate` (columns already on `Transaction`). Block only when rate missing.
- Overpay clamps to remaining with `final payment adjusted` warning.
- Delete Liability with DRAWN tranches or linked txs is blocked until reassigned;
  delete Property nulls `realEstateId` on txs (audit preserved).

Repayment preview: `LiabilityService.getRepaymentPreview(liabilityId, date, total)`
wraps existing `calculateAmortizationSchedule()`; interest-only tranches return
`principal 0` before `interestOnlyUntil`.

## 5. UI flows (separate steps; wizard optional in §7)

Existing pages only — no new top-level pages.

**Case 1 — 50k via Checking:**
1. `Liabilities → New → MORTGAGE, Principal 50k, [x] Disburse now → To: Checking,
   Date, T1` → `Liability 50k`, `Checking +50k`, `T1 DRAWN`.
2. `Real Estate → New → House, Price 50k, Value 50k, Mortgage: [50k loan]`.
3. `Transactions → New EXPENSE → From: Checking 50k, Property: House,
   CAPITAL_IMPROVEMENT (purchase)` → `Checking −50k`. Net worth 0
   (50k house − 50k loan).

**Case 2 — 50k direct to seller:**
1. Same Liability form with `Disburse → Direct to seller/property →
   Property: [new House]`. No Checking move. `Liability 50k`, `Property 50k`,
   `T1 DRAWN → House`.

**+20k down-payment → 70k:**
`Transactions → New EXPENSE → From: Checking 20k, Property: House,
CAPITAL_IMPROVEMENT` → value `50k → 70k`, equity `70k − 50k = 20k`.

**15k kitchen vs 500 leak:**
`15k + CAPITAL_IMPROVEMENT → 70k → 85k`; `500 + MAINTENANCE → stays 85k`,
listed under `Property → Costs (non-capitalized)`.

**1,200 repayment:**
`Transactions → New EXPENSE → From: Checking, Liability: Home Mortgage,
[Auto-split ✓ 800/300/100] → Save` → `Liability 50k → 49,200`.
Visible in `Liability → Linked Payments` and `Property → Loan movements`
(via `mortgageId`).

**Movements views:**
- `LiabilityDetailDialog`: `Overview / Amortization / Linked Payments
  (date, account, tranche, P/I split, remaining) / Drawdowns
  (T1 DRAWN → House, T2 PLANNED)`.
- `PropertyDetailView → Costs`: purchase + capitalized vs maintenance split +
  loan moves. `AssetDetailModal → Costs`: same via `assetId`.

## 6. Net worth & reporting

- Keep exclusion of `Asset(type=REAL_ESTATE)` when a property row exists.
- Sum `tranche.remaining`, never `principal + drawnAmount` together.
- Backfill reverses principal legs only; interest/insurance ignored.
- `DashboardService` borrowing-capacity uses drawn, not approved.

## 7. Work breakdown (V1 build order)

Phase 1 — links: migration for tx columns + `liability_tranches`; entities +
repositories; `TransactionService` sync + read-only balance guard;
`POST /liabilities/{id}/disburse`; tx accepts
`liabilityId+trancheId+movement_type+splits[]`.
Phase 2 — repayments: `getRepaymentPreview()`; editable split preview in
`TransactionForm`; Linked Payments tab.
Phase 3 — costs: capitalized vs maintenance in `RealEstateService`/`AssetService`
+ `RealEstateValueHistory` inserts; `?realEstateId=&assetId=` filters; Costs and
Loan-movements lists.
Phase 4 — tranches + net worth: tranche CRUD, FIFO allocation with explicit
override, reconciler + repair job, net-worth guards, dashboard drawn-vs-approved.
Phase 5 — V1 extras: FX via `ExchangeRateService` + original-amount columns;
`Real Estate → Buy property` wizard (property + funding + review, forms stay);
interest-only flag + two-phase amortization tab.
Cross-cutting: i18n keys (en+fr) + `MessageKeyCoverageTest`; Vitest for preview
and capitalized-vs-maintenance; `@DataJpaTest` for sync/reversal;
`mvn spotless:apply`.

## 8. Self-review

- No TBDs; FIFO + explicit override resolves allocation ambiguity.
- `currentBalance = SUM(remaining)` is consistent with §4 read-only guard.
- Single new table keeps scope to one plan; wizard/FX are additive, not forks.
- `DISBURSEMENT_DIRECT` has no account leg by definition — stated explicitly.
