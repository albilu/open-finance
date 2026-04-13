package org.openfinance.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.openfinance.dto.BudgetBulkCreateRequest;
import org.openfinance.dto.BudgetBulkCreateResponse;
import org.openfinance.dto.BudgetHistoryResponse;
import org.openfinance.dto.BudgetProgressResponse;
import org.openfinance.dto.BudgetRequest;
import org.openfinance.dto.BudgetResponse;
import org.openfinance.dto.BudgetSuggestion;
import org.openfinance.dto.BudgetSuggestionRequest;
import org.openfinance.dto.BudgetSummaryResponse;
import org.openfinance.entity.BudgetPeriod;
import org.openfinance.entity.User;
import org.openfinance.service.BudgetService;
import org.openfinance.util.EncryptionUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.crypto.SecretKey;
import java.util.List;

/**
 * REST controller for budget management endpoints.
 * 
 * <p>
 * Provides CRUD operations for budgets and budget tracking. All endpoints
 * require authentication
 * and use the user's encryption key to secure sensitive data.
 * 
 * <p>
 * <strong>Endpoints:</strong>
 * <ul>
 * <li>POST /api/v1/budgets - Create new budget</li>
 * <li>GET /api/v1/budgets - List all user budgets (optional period filter)</li>
 * <li>GET /api/v1/budgets/{id} - Get budget by ID</li>
 * <li>PUT /api/v1/budgets/{id} - Update budget</li>
 * <li>DELETE /api/v1/budgets/{id} - Delete budget</li>
 * <li>GET /api/v1/budgets/{id}/progress - Get budget progress tracking</li>
 * <li>GET /api/v1/budgets/{id}/history - Get budget spending history by
 * sub-period</li>
 * <li>GET /api/v1/budgets/summary - Get budget summary by period</li>
 * </ul>
 * 
 * <p>
 * <strong>Security:</strong>
 * <ul>
 * <li>All endpoints require JWT authentication</li>
 * <li>Encryption key must be provided via X-Encryption-Key header for amount
 * access</li>
 * <li>Users can only access their own budgets</li>
 * <li>Budget amounts are encrypted at rest</li>
 * </ul>
 * 
 * <p>
 * <strong>Requirements:</strong>
 * </p>
 * <ul>
 * <li>REQ-2.9.1.1: Budget creation and management</li>
 * <li>REQ-2.9.1.2: Budget tracking and progress calculation</li>
 * <li>REQ-2.9.1.3: Budget reports and summaries</li>
 * <li>REQ-2.18: Data encryption at rest</li>
 * <li>REQ-3.2: Authorization checks</li>
 * </ul>
 * 
 * @see BudgetService
 * @see BudgetRequest
 * @see BudgetResponse
 * @author Open-Finance Development Team
 * @version 1.0
 * @since 2026-02-02
 */
@RestController
@RequestMapping("/api/v1/budgets")
@RequiredArgsConstructor
@Slf4j
public class BudgetController {

    private static final String ENCRYPTION_KEY_HEADER = "X-Encryption-Key";

    private final BudgetService budgetService;

    /**
     * Creates a new budget for the authenticated user.
     * 
     * <p><strong>Request Headers:</strong>
     * <ul>
     * <li>Authorization: Bearer {jwt_token}</li>
     * <li>X-Encryption-Key: {base64_encoded_key}</li>
     * </ul>
     * 
     * <p><strong>Request Body:</strong>
     * <pre>{@code
     * {
     * "categoryId": 1,
     * "amount": 500.00,
     * "currency": "USD",
     * "period": "MONTHLY",
     * "startDate": "2026-02-01",
     * "endDate": "2026-02-28",
     * "rollover": false,
     * "notes": "Monthly grocery budget"
     * }
     * }</pre>
     * 
     * <p><strong>Success Response (HTTP 201 Created):</strong>
     * <pre>{@code
     * {
     * "id": 1,
     * "categoryId": 1,
     * "categoryName": "Groceries",
     * "categoryType": "EXPENSE",
     * "amount": 500.00,
     * "currency": "USD",
     * "period": "MONTHLY",
     * "startDate": "2026-02-01",
     * "endDate": "2026-02-28",
     * "rollover": false,
     * "notes": "Monthly grocery budget",
     * "createdAt": "2026-02-02T10:00:00",
     * "updatedAt": "2026-02-02T10:00:00"
     * }
     * }</pre>
     * 
     * <p>Requirement REQ-2.9.1.1: Create budget</p>
     * 
     * @param request budget creation request
     * @param encodedKey Base64-encoded encryption key from header
     * @param authentication Spring Security authentication object
     * @return HTTP 201 Created with BudgetResponse
     * @throws IllegalArgumentException if encryption key is missing or invalid
     */
    @PostMapping
    public ResponseEntity<BudgetResponse> createBudget(
            @Valid @RequestBody BudgetRequest request,
            @RequestHeader(value = ENCRYPTION_KEY_HEADER, required = false) String encodedKey,
            Authentication authentication) {

        log.info("Creating budget for user");

        if (encodedKey == null || encodedKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Encryption key header is required");
        }

        User user = (User) authentication.getPrincipal();
        SecretKey encryptionKey = EncryptionUtil.decodeEncryptionKey(encodedKey);

        BudgetResponse response = budgetService.createBudget(request, user.getId(), encryptionKey);

        log.info("Budget created successfully: id={}, category={}, period={}",
                response.getId(), response.getCategoryName(), response.getPeriod());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieves all budgets for the authenticated user.
     * 
     * <p>Optionally filter by budget period type using the period query parameter.
     * 
     * <p><strong>Request Headers:</strong>
     * <ul>
     * <li>Authorization: Bearer {jwt_token}</li>
     * <li>X-Encryption-Key: {base64_encoded_key}</li>
     * </ul>
     * 
     * <p><strong>Query Parameters:</strong>
     * <ul>
     * <li>period (optional): Filter by period type (WEEKLY, MONTHLY, QUARTERLY,
     * YEARLY)</li>
     * </ul>
     * 
     * <p><strong>Success Response (HTTP 200 OK):</strong>
     * <pre>{@code
     * [
     * {
     * "id": 1,
     * "categoryId": 1,
     * "categoryName": "Groceries",
     * "categoryType": "EXPENSE",
     * "amount": 500.00,
     * "currency": "USD",
     * "period": "MONTHLY",
     * "startDate": "2026-02-01",
     * "endDate": "2026-02-28",
     * "rollover": false,
     * "notes": "Monthly grocery budget",
     * "createdAt": "2026-02-02T10:00:00",
     * "updatedAt": "2026-02-02T10:00:00"
     * }
     * ]
     * }</pre>
     * 
     * <p>Requirement REQ-2.9.1.1: List budgets with optional filtering</p>
     * 
     * @param period optional period filter
     * @param encodedKey Base64-encoded encryption key from header
     * @param authentication Spring Security authentication object
     * @return HTTP 200 OK with list of BudgetResponse
     */
    @GetMapping
    public ResponseEntity<List<BudgetResponse>> getAllBudgets(
            @RequestParam(required = false) BudgetPeriod period,
            @RequestHeader(value = ENCRYPTION_KEY_HEADER, required = false) String encodedKey,
            Authentication authentication) {

        log.info("Retrieving budgets for user{}", period != null ? " with period=" + period : "");

        if (encodedKey == null || encodedKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Encryption key header is required");
        }

        User user = (User) authentication.getPrincipal();
        SecretKey encryptionKey = EncryptionUtil.decodeEncryptionKey(encodedKey);

        List<BudgetResponse> budgets;
        if (period != null) {
            budgets = budgetService.getBudgetsByPeriod(user.getId(), period, encryptionKey);
        } else {
            budgets = budgetService.getBudgetsByUser(user.getId(), encryptionKey);
        }

        log.info("Retrieved {} budgets for user", budgets.size());

        return ResponseEntity.ok(budgets);
    }

    /**
     * Retrieves a specific budget by ID.
     * 
     * <p>
     * <strong>Request Headers:</strong>
     * <ul>
     * <li>Authorization: Bearer {jwt_token}</li>
     * <li>X-Encryption-Key: {base64_encoded_key}</li>
     * </ul>
     * 
     * <p>
     * <strong>Success Response (HTTP 200 OK):</strong>
     * Returns a single BudgetResponse object.
     * 
     * <p>
     * <strong>Error Responses:</strong>
     * <ul>
     * <li>HTTP 404 Not Found - Budget not found or doesn't belong to user</li>
     * </ul>
     * 
     * <p>
     * Requirement REQ-2.9.1.1: Retrieve budget details
     * </p>
     * <p>
     * Requirement REQ-3.2: Authorization - verify ownership
     * </p>
     * 
     * @param budgetId       the budget ID
     * @param encodedKey     Base64-encoded encryption key from header
     * @param authentication Spring Security authentication object
     * @return HTTP 200 OK with BudgetResponse
     */
    @GetMapping("/{id}")
    public ResponseEntity<BudgetResponse> getBudgetById(
            @PathVariable("id") Long budgetId,
            @RequestHeader(value = ENCRYPTION_KEY_HEADER, required = false) String encodedKey,
            Authentication authentication) {

        log.info("Retrieving budget: id={}", budgetId);

        if (encodedKey == null || encodedKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Encryption key header is required");
        }

        User user = (User) authentication.getPrincipal();
        SecretKey encryptionKey = EncryptionUtil.decodeEncryptionKey(encodedKey);

        BudgetResponse response = budgetService.getBudgetById(budgetId, user.getId(), encryptionKey);

        log.info("Budget retrieved successfully: id={}", budgetId);

        return ResponseEntity.ok(response);
    }

    /**
     * Updates an existing budget.
     * 
     * <p>
     * <strong>Request Headers:</strong>
     * <ul>
     * <li>Authorization: Bearer {jwt_token}</li>
     * <li>X-Encryption-Key: {base64_encoded_key}</li>
     * </ul>
     * 
     * <p>
     * <strong>Request Body:</strong>
     * Same format as create budget. All fields are optional; only provided fields
     * are updated.
     * 
     * <p>
     * <strong>Success Response (HTTP 200 OK):</strong>
     * Returns updated BudgetResponse object.
     * 
     * <p>
     * <strong>Error Responses:</strong>
     * <ul>
     * <li>HTTP 404 Not Found - Budget not found or doesn't belong to user</li>
     * <li>HTTP 400 Bad Request - Validation errors or duplicate budget</li>
     * </ul>
     * 
     * <p>
     * Requirement REQ-2.9.1.1: Update budget
     * </p>
     * <p>
     * Requirement REQ-3.2: Authorization - verify ownership
     * </p>
     * 
     * @param budgetId       the budget ID
     * @param request        budget update request
     * @param encodedKey     Base64-encoded encryption key from header
     * @param authentication Spring Security authentication object
     * @return HTTP 200 OK with BudgetResponse
     */
    @PutMapping("/{id}")
    public ResponseEntity<BudgetResponse> updateBudget(
            @PathVariable("id") Long budgetId,
            @Valid @RequestBody BudgetRequest request,
            @RequestHeader(value = ENCRYPTION_KEY_HEADER, required = false) String encodedKey,
            Authentication authentication) {

        log.info("Updating budget: id={}", budgetId);

        if (encodedKey == null || encodedKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Encryption key header is required");
        }

        User user = (User) authentication.getPrincipal();
        SecretKey encryptionKey = EncryptionUtil.decodeEncryptionKey(encodedKey);

        BudgetResponse response = budgetService.updateBudget(budgetId, request, user.getId(), encryptionKey);

        log.info("Budget updated successfully: id={}", budgetId);

        return ResponseEntity.ok(response);
    }

    /**
     * Deletes a budget (hard delete).
     * 
     * <p>
     * The budget is permanently removed from the database.
     * 
     * <p>
     * <strong>Request Headers:</strong>
     * <ul>
     * <li>Authorization: Bearer {jwt_token}</li>
     * </ul>
     * 
     * <p>
     * <strong>Error Responses:</strong>
     * <ul>
     * <li>HTTP 404 Not Found - Budget not found or doesn't belong to user</li>
     * </ul>
     * 
     * <p>
     * Requirement REQ-2.9.1.1: Delete budget
     * </p>
     * <p>
     * Requirement REQ-3.2: Authorization - verify ownership
     * </p>
     * 
     * @param budgetId       the budget ID
     * @param authentication Spring Security authentication object
     * @return HTTP 204 No Content on success
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBudget(
            @PathVariable("id") Long budgetId,
            @RequestHeader(value = ENCRYPTION_KEY_HEADER, required = false) String encodedKey,
            Authentication authentication) {

        log.info("Deleting budget: id={}", budgetId);

        User user = (User) authentication.getPrincipal();
        SecretKey encryptionKey = encodedKey != null && !encodedKey.trim().isEmpty()
                ? EncryptionUtil.decodeEncryptionKey(encodedKey)
                : null;

        budgetService.deleteBudget(budgetId, user.getId(), encryptionKey);

        log.info("Budget deleted successfully: id={}", budgetId);

        return ResponseEntity.noContent().build();
    }

    /**
     * Retrieves budget progress tracking information.
     * 
     * <p>Calculates spending progress including:
     * <ul>
     * <li>Budgeted amount</li>
     * <li>Amount spent so far</li>
     * <li>Remaining amount</li>
     * <li>Percentage spent</li>
     * <li>Days remaining in period</li>
     * <li>Status indicator (ON_TRACK, WARNING, EXCEEDED)</li>
     * </ul>
     * 
     * <p><strong>Request Headers:</strong>
     * <ul>
     * <li>Authorization: Bearer {jwt_token}</li>
     * <li>X-Encryption-Key: {base64_encoded_key}</li>
     * </ul>
     * 
     * <p><strong>Success Response (HTTP 200 OK):</strong>
     * <pre>{@code
     * {
     * "budgetId": 1,
     * "categoryName": "Groceries",
     * "budgeted": 500.00,
     * "spent": 350.25,
     * "remaining": 149.75,
     * "percentageSpent": 70.05,
     * "currency": "USD",
     * "period": "MONTHLY",
     * "startDate": "2026-02-01",
     * "endDate": "2026-02-28",
     * "daysRemaining": 26,
     * "status": "ON_TRACK"
     * }
     * }</pre>
     * 
     * <p>Requirement REQ-2.9.1.2: Budget tracking with spent/remaining
     * calculations</p>
     * 
     * @param budgetId the budget ID
     * @param encodedKey Base64-encoded encryption key from header
     * @param authentication Spring Security authentication object
     * @return HTTP 200 OK with BudgetProgressResponse
     */
    @GetMapping("/{id}/progress")
    public ResponseEntity<BudgetProgressResponse> getBudgetProgress(
            @PathVariable("id") Long budgetId,
            @RequestHeader(value = ENCRYPTION_KEY_HEADER, required = false) String encodedKey,
            Authentication authentication) {

        log.info("Retrieving budget progress: id={}", budgetId);

        if (encodedKey == null || encodedKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Encryption key header is required");
        }

        User user = (User) authentication.getPrincipal();
        SecretKey encryptionKey = EncryptionUtil.decodeEncryptionKey(encodedKey);

        BudgetProgressResponse progress = budgetService.calculateBudgetProgress(
                budgetId, user.getId(), encryptionKey);

        log.info("Budget progress retrieved: id={}, status={}, percentageSpent={}%",
                budgetId, progress.getStatus(), progress.getPercentageSpent());

        return ResponseEntity.ok(progress);
    }

    /**
     * Retrieves the per-sub-period spending history for a budget.
     *
     * <p>For example, a yearly "Food" budget spanning Jan–Dec 2024 with MONTHLY
     * period
     * returns 12 rows — one per calendar month — each showing budgeted, spent,
     * remaining,
     * percentage spent, and a status indicator.
     *
     * <p><strong>Request Headers:</strong>
     * <ul>
     * <li>Authorization: Bearer {jwt_token}</li>
     * <li>X-Encryption-Key: {base64_encoded_key}</li>
     * </ul>
     *
     * <p><strong>Success Response (HTTP 200 OK):</strong>
     * <pre>{@code
     * {
     * "budgetId": 1,
     * "categoryName": "Food",
     * "amount": 500.00,
     * "currency": "EUR",
     * "period": "YEARLY",
     * "startDate": "2024-01-01",
     * "endDate": "2024-12-31",
     * "history": [
     * {
     * "label": "Jan 2024",
     * "periodStart": "2024-01-01",
     * "periodEnd": "2024-01-31",
     * "budgeted": 500.00,
     * "spent": 420.50,
     * "remaining": 79.50,
     * "percentageSpent": 84.10,
     * "status": "WARNING"
     * },
     * ...
     * ],
     * "totalSpent": 4950.00,
     * "totalBudgeted": 6000.00
     * }
     * }</pre>
     *
     * <p>Requirement REQ-2.9.1.4: Budget history per sub-period breakdown</p>
     *
     * @param budgetId the budget ID
     * @param encodedKey Base64-encoded encryption key from header
     * @param authentication Spring Security authentication object
     * @return HTTP 200 OK with BudgetHistoryResponse
     */
    @GetMapping("/{id}/history")
    public ResponseEntity<BudgetHistoryResponse> getBudgetHistory(
            @PathVariable("id") Long budgetId,
            @RequestHeader(value = ENCRYPTION_KEY_HEADER, required = false) String encodedKey,
            Authentication authentication) {

        log.info("Retrieving budget history: id={}", budgetId);

        if (encodedKey == null || encodedKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Encryption key header is required");
        }

        User user = (User) authentication.getPrincipal();
        SecretKey encryptionKey = EncryptionUtil.decodeEncryptionKey(encodedKey);

        BudgetHistoryResponse history = budgetService.getBudgetHistory(
                budgetId, user.getId(), encryptionKey);

        log.info("Budget history retrieved: id={}, subPeriods={}", budgetId, history.getHistory().size());

        return ResponseEntity.ok(history);
    }

    /**
     * Retrieves aggregate budget summary for a period.
     * 
     * <p>Provides summary statistics across all budgets of a specific period type:
     * <ul>
     * <li>Total number of budgets</li>
     * <li>Number of active budgets</li>
     * <li>Total budgeted amount</li>
     * <li>Total spent amount</li>
     * <li>Total remaining amount</li>
     * <li>Average percentage spent</li>
     * <li>Individual budget progress details</li>
     * </ul>
     * 
     * <p><strong>Request Headers:</strong>
     * <ul>
     * <li>Authorization: Bearer {jwt_token}</li>
     * <li>X-Encryption-Key: {base64_encoded_key}</li>
     * </ul>
     * 
     * <p><strong>Query Parameters:</strong>
     * <ul>
     * <li>period (required): Budget period type (WEEKLY, MONTHLY, QUARTERLY,
     * YEARLY)</li>
     * </ul>
     * 
     * <p><strong>Success Response (HTTP 200 OK):</strong>
     * <pre>{@code
     * {
     * "period": "MONTHLY",
     * "totalBudgets": 5,
     * "activeBudgets": 3,
     * "totalBudgeted": 2500.00,
     * "totalSpent": 1750.50,
     * "totalRemaining": 749.50,
     * "averageSpentPercentage": 70.02,
     * "currency": "USD",
     * "budgets": [ ... ]
     * }
     * }</pre>
     * 
     * <p>Requirement REQ-2.9.1.3: Budget reports with aggregate statistics</p>
     * 
     * @param period budget period type (required)
     * @param encodedKey Base64-encoded encryption key from header
     * @param authentication Spring Security authentication object
     * @return HTTP 200 OK with BudgetSummaryResponse
     */
    @GetMapping("/summary")
    public ResponseEntity<BudgetSummaryResponse> getBudgetSummary(
            @RequestParam(required = false) BudgetPeriod period,
            @RequestHeader(value = ENCRYPTION_KEY_HEADER, required = false) String encodedKey,
            Authentication authentication) {

        log.info("Retrieving budget summary: period={}", period);

        if (encodedKey == null || encodedKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Encryption key header is required");
        }

        User user = (User) authentication.getPrincipal();
        SecretKey encryptionKey = EncryptionUtil.decodeEncryptionKey(encodedKey);

        BudgetSummaryResponse summary = period != null
                ? budgetService.getBudgetSummary(user.getId(), period, encryptionKey)
                : budgetService.getAllBudgetsSummary(user.getId(), encryptionKey);

        log.info("Budget summary retrieved: period={}, totalBudgets={}, totalSpent={}",
                period, summary.getTotalBudgets(), summary.getTotalSpent());

        return ResponseEntity.ok(summary);
    }

    /**
     * Analyses past EXPENSE transactions and returns automatic budget suggestions.
     *
     * <p>The service scans the user's transaction history over the specified
     * lookback
     * window, groups EXPENSE transactions by category, computes per-period
     * averages,
     * and flags categories that already have a budget for the requested period.
     *
     * <p><strong>Request Headers:</strong>
     * <ul>
     * <li>Authorization: Bearer {jwt_token}</li>
     * <li>X-Encryption-Key: {base64_encoded_key}</li>
     * </ul>
     *
     * <p><strong>Request Body:</strong>
     * <pre>{@code
     * {
     * "period": "MONTHLY",
     * "lookbackMonths": 6,
     * "currency": "EUR",
     * "categoryIds": null
     * }
     * }</pre>
     *
     * <p><strong>Success Response (HTTP 200 OK):</strong>
     * Returns a list of {@link BudgetSuggestion} objects.
     *
     * <p>Requirement REQ-2.9.1.5: Automatic budget creation from transaction
     * history analysis
     *
     * @param request the suggestion analysis request
     * @param encodedKey Base64-encoded encryption key from header
     * @param authentication Spring Security authentication object
     * @return HTTP 200 OK with list of BudgetSuggestion
     */
    @PostMapping("/suggestions")
    public ResponseEntity<List<BudgetSuggestion>> getBudgetSuggestions(
            @Valid @RequestBody BudgetSuggestionRequest request,
            @RequestHeader(value = ENCRYPTION_KEY_HEADER, required = false) String encodedKey,
            Authentication authentication) {

        log.info("Analysing spending for suggestions: period={}, lookbackMonths={}",
                request.getPeriod(), request.getLookbackMonths());

        if (encodedKey == null || encodedKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Encryption key header is required");
        }

        User user = (User) authentication.getPrincipal();
        SecretKey encryptionKey = EncryptionUtil.decodeEncryptionKey(encodedKey);

        List<BudgetSuggestion> suggestions = budgetService.analyzeCategorySpending(
                user.getId(),
                request.getPeriod(),
                request.getLookbackMonths(),
                request.getCategoryIds(),
                encryptionKey);

        // Override currency if specified in the request
        if (request.getCurrency() != null && !request.getCurrency().isBlank()) {
            suggestions = suggestions.stream()
                    .peek(s -> s.setCurrency(request.getCurrency()))
                    .collect(java.util.stream.Collectors.toList());
        }

        log.info("Suggestion analysis complete: userId={}, suggestions={}", user.getId(), suggestions.size());

        return ResponseEntity.ok(suggestions);
    }

    /**
     * Bulk-creates multiple budgets from user-confirmed suggestions.
     *
     * <p>Each {@link BudgetRequest} in the list is processed independently.
     * Duplicate category+period combinations are silently skipped and counted in
     * {@code skippedCount}. Other failures are collected in the {@code errors}
     * list.
     *
     * <p><strong>Request Headers:</strong>
     * <ul>
     * <li>Authorization: Bearer {jwt_token}</li>
     * <li>X-Encryption-Key: {base64_encoded_key}</li>
     * </ul>
     *
     * <p><strong>Request Body:</strong>
     * <pre>{@code
     * {
     * "budgets": [
     * { "categoryId": 1, "amount": 300.00, "currency": "EUR",
     * "period": "MONTHLY", "startDate": "2026-03-01", "endDate": "2026-03-31",
     * "rollover": false }
     * ]
     * }
     * }</pre>
     *
     * <p><strong>Success Response (HTTP 201 Created):</strong>
     * Returns a {@link BudgetBulkCreateResponse} with created list, counts and
     * errors.
     *
     * <p>Requirement REQ-2.9.1.5: Bulk budget creation from user-confirmed
     * suggestions
     *
     * @param request the bulk creation request
     * @param encodedKey Base64-encoded encryption key from header
     * @param authentication Spring Security authentication object
     * @return HTTP 201 Created with BudgetBulkCreateResponse
     */
    @PostMapping("/bulk")
    public ResponseEntity<BudgetBulkCreateResponse> bulkCreateBudgets(
            @Valid @RequestBody BudgetBulkCreateRequest request,
            @RequestHeader(value = ENCRYPTION_KEY_HEADER, required = false) String encodedKey,
            Authentication authentication) {

        log.info("Bulk-creating {} budgets", request.getBudgets().size());

        if (encodedKey == null || encodedKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Encryption key header is required");
        }

        User user = (User) authentication.getPrincipal();
        SecretKey encryptionKey = EncryptionUtil.decodeEncryptionKey(encodedKey);

        BudgetBulkCreateResponse response = budgetService.bulkCreateBudgets(
                user.getId(), request.getBudgets(), encryptionKey);

        log.info("Bulk create complete: created={}, skipped={}, errors={}",
                response.getSuccessCount(), response.getSkippedCount(), response.getErrors().size());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
