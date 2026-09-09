package org.openfinance.controller;

import jakarta.validation.Valid;
import jakarta.validation.groups.Default;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openfinance.dto.LiabilityTrancheRequest;
import org.openfinance.dto.LiabilityTrancheResponse;
import org.openfinance.entity.User;
import org.openfinance.service.LiabilityService;
import org.openfinance.validation.OnCreate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for liability tranche management endpoints (Task 4).
 *
 * <p>A tranche is a single planned drawdown of a liability disbursed in stages (e.g. a construction
 * loan). Tranches are created PLANNED, transition to DRAWN via the disbursement endpoint, and may
 * be toggled between PLANNED and CANCELLED while not yet drawn.
 *
 * <p><strong>Endpoints:</strong>
 *
 * <ul>
 *   <li>POST /api/v1/liabilities/{id}/tranches - Create a planned tranche
 *   <li>PATCH /api/v1/tranches/{trancheId} - Update a tranche's planned fields
 * </ul>
 *
 * <p><strong>Security:</strong> all endpoints require JWT authentication and the user can only
 * access their own tranches (verified by userId scoping).
 *
 * @see LiabilityService
 * @see LiabilityTrancheRequest
 * @see LiabilityTrancheResponse
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class LiabilityTrancheController {

    private final LiabilityService liabilityService;

    /**
     * Creates a planned tranche on a liability.
     *
     * <p>{@code trancheNo} is auto-assigned (max existing + 1) when absent; {@code currency}
     * defaults to the liability's currency (a provided value must match it).
     *
     * <p><strong>Example Request:</strong>
     *
     * <pre>POST /api/v1/liabilities/1/tranches
     * {
     *   "plannedAmount": 50000.00,
     *   "plannedDate": "2026-04-01",
     *   "currency": "USD"
     * }</pre>
     *
     * <p><strong>Success Response (HTTP 201 Created):</strong> the created LiabilityTrancheResponse
     *
     * @param liabilityId liability ID to attach the tranche to
     * @param request tranche creation request
     * @param authentication Spring Security authentication object
     * @return HTTP 201 Created with LiabilityTrancheResponse
     */
    @PostMapping("/liabilities/{id}/tranches")
    public ResponseEntity<LiabilityTrancheResponse> createTranche(
            @PathVariable("id") Long liabilityId,
            @Validated({Default.class, OnCreate.class}) @RequestBody
                    LiabilityTrancheRequest request,
            Authentication authentication) {

        log.info(
                "Creating tranche on liability {}: plannedAmount={}",
                liabilityId,
                request.getPlannedAmount());
        User user = (User) authentication.getPrincipal();
        LiabilityTrancheResponse response =
                liabilityService.createTranche(user.getId(), liabilityId, request);

        log.info(
                "Tranche created successfully: id={}, trancheNo={}",
                response.getId(),
                response.getTrancheNo());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Updates a tranche.
     *
     * <p>PLANNED/CANCELLED tranches accept planned-field updates ({@code plannedAmount}, {@code
     * plannedDate}, {@code fee}, {@code interestOnly}, {@code interestOnlyUntil}, {@code notes},
     * {@code realEstateId}) and PLANNED&#8596;CANCELLED status transitions. DRAWN tranches are
     * immutable except for {@code realEstateId} and {@code notes}. The update is partial: fields
     * omitted (null) from the request are left unchanged; an empty string clears a String field
     * ({@code notes}).
     *
     * <p><strong>Example Request:</strong>
     *
     * <pre>PATCH /api/v1/tranches/3
     * {
     *   "plannedAmount": 45000.00,
     *   "notes": "Foundation work"
     * }</pre>
     *
     * <p><strong>Success Response (HTTP 200 OK):</strong> the updated LiabilityTrancheResponse
     *
     * @param trancheId tranche ID to update
     * @param request tranche update request
     * @param authentication Spring Security authentication object
     * @return HTTP 200 OK with the updated LiabilityTrancheResponse
     */
    @PatchMapping("/tranches/{trancheId}")
    public ResponseEntity<LiabilityTrancheResponse> updateTranche(
            @PathVariable("trancheId") Long trancheId,
            @Valid @RequestBody LiabilityTrancheRequest request,
            Authentication authentication) {

        log.info("Updating tranche: id={}", trancheId);
        User user = (User) authentication.getPrincipal();
        LiabilityTrancheResponse response =
                liabilityService.updateTranche(user.getId(), trancheId, request);

        log.info("Tranche updated successfully: id={}, status={}", trancheId, response.getStatus());

        return ResponseEntity.ok(response);
    }

    public record ReversalRequest(
            @jakarta.validation.constraints.NotNull java.time.LocalDate date) {}

    @PostMapping("/tranches/{trancheId}/reverse")
    public LiabilityTrancheResponse reverse(
            @PathVariable Long trancheId,
            @Valid @RequestBody ReversalRequest request,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return liabilityService.reverseDirectDraw(user.getId(), trancheId, request.date());
    }

    @org.springframework.web.bind.annotation.GetMapping("/real-estate/{propertyId}/drawdowns")
    public java.util.List<LiabilityTrancheResponse> propertyDrawdowns(
            @PathVariable Long propertyId, Authentication authentication) {
        return liabilityService.getPropertyDrawdowns(
                ((User) authentication.getPrincipal()).getId(), propertyId);
    }

    @GetMapping("/real-estate/{propertyId}/loan-movements")
    public java.util.List<org.openfinance.dto.TransactionResponse> propertyLoanMovements(
            @PathVariable Long propertyId, Authentication authentication) {
        return liabilityService.getPropertyLoanMovements(
                ((User) authentication.getPrincipal()).getId(), propertyId);
    }
}
