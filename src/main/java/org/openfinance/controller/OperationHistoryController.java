package org.openfinance.controller;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openfinance.dto.OperationHistoryResponse;
import org.openfinance.entity.EntityType;
import org.openfinance.entity.OperationHistory;
import org.openfinance.entity.User;
import org.openfinance.service.OperationHistoryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for the Operation History (Undo/Redo) feature.
 *
 * <p>Endpoints:
 *
 * <ul>
 *   <li>GET /api/v1/history — list history for the authenticated user (paged)
 *   <li>POST /api/v1/history/{id}/undo — undo a recorded operation
 *   <li>POST /api/v1/history/{id}/redo — redo a previously undone operation
 * </ul>
 *
 * <p>Supported creation operations can be undone through their domain services. Unsupported
 * reversal operations are rejected without changing data or history status.
 */
@RestController
@RequestMapping("/api/v1/history")
@RequiredArgsConstructor
@Slf4j
public class OperationHistoryController {

    private final OperationHistoryService historyService;

    // Lazy-inject domain services to avoid circular Spring dependency
    private final org.openfinance.service.AccountService accountService;
    private final org.openfinance.service.TransactionService transactionService;
    private final org.openfinance.service.AssetService assetService;
    private final org.openfinance.service.LiabilityService liabilityService;
    private final org.openfinance.service.RealEstateService realEstateService;
    private final org.openfinance.service.BudgetService budgetService;

    /**
     * Returns a page of operation history entries for the authenticated user, newest first.
     *
     * @param entityType optional filter by entity type
     * @param since optional ISO instant; only entries created after this time are returned
     * @param pageable pagination params (default: 20 per page, sorted by createdAt DESC)
     */
    @GetMapping
    public ResponseEntity<Page<OperationHistoryResponse>> getHistory(
            @RequestParam(required = false) EntityType entityType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant since,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
                    Pageable pageable,
            Authentication authentication) {

        User user = (User) authentication.getPrincipal();
        log.info("Fetching operation history for user {}", user.getId());

        LocalDateTime sinceLocal =
                since != null ? LocalDateTime.ofInstant(since, ZoneOffset.UTC) : null;

        Page<OperationHistoryResponse> page =
                historyService.getHistory(user.getId(), entityType, sinceLocal, pageable);
        return ResponseEntity.ok(page);
    }

    /** Undo a supported creation and mark history in the same transaction. */
    @PostMapping("/{id}/undo")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<OperationHistoryResponse> undo(
            @PathVariable("id") Long historyId, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        OperationHistory entry = historyService.getEntry(historyId, user.getId());
        if (!entry.canUndo())
            throw new IllegalStateException("Undo is unavailable for this operation");
        try {
            historyService.suppressRecording();
            dispatchDelete(entry.getEntityType(), entry.getEntityId(), user.getId());
            return ResponseEntity.ok(historyService.markUndone(historyId, user.getId()));
        } finally {
            historyService.resumeRecording();
        }
    }

    /** Redo remains unavailable until domain restoration is implemented. */
    @PostMapping("/{id}/redo")
    public ResponseEntity<OperationHistoryResponse> redo(
            @PathVariable("id") Long historyId, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        historyService.getEntry(historyId, user.getId());
        throw new IllegalStateException("Redo is unavailable for this operation");
    }

    private void dispatchDelete(EntityType entityType, Long entityId, Long userId) {
        switch (entityType) {
            case ACCOUNT -> accountService.deleteAccount(entityId, userId);
            case ASSET -> assetService.deleteAsset(entityId, userId);
            case LIABILITY -> liabilityService.deleteLiability(entityId, userId);
            case REAL_ESTATE -> realEstateService.deleteProperty(entityId, userId);
            case BUDGET -> budgetService.deleteBudget(entityId, userId);
            case TRANSACTION -> transactionService.deleteTransaction(entityId, userId);
            default -> throw new IllegalStateException("Undo is unavailable for this entity type");
        }
    }
}
