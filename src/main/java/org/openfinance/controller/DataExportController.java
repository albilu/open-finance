package org.openfinance.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openfinance.dto.DataExportRequest;
import org.openfinance.dto.DataExportResponse;
import org.openfinance.entity.User;
import org.openfinance.service.DataExportService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for data export operations.
 *
 * <p>Provides endpoints for backing up user financial data in JSON or CSV format.
 *
 * <p><strong>Note:</strong> Import functionality is intentionally not implemented. Exported data is
 * intended for backup, archival, and external analysis purposes only.
 *
 * <p>Requirement: REQ-3.4 - Data Export and Backup
 *
 * @author Open Finance Development Team
 */
@RestController
@RequestMapping("/api/v1/data")
@RequiredArgsConstructor
@Slf4j
public class DataExportController {

    private final DataExportService dataExportService;

    /**
     * Export all user data.
     *
     * <p><b>Example Request:</b>
     *
     * <pre>
     * POST /api/v1/data/export
     * Headers:
     *   Authorization: Bearer {jwt-token}
     *   X-Encryption-Session: {base64-encoded-key}
     *
     * Body:
     * {
     *   "format": "JSON",
     *   "includeAccounts": true,
     *   "includeTransactions": true,
     *   "includeAssets": true,
     *   "includeLiabilities": true,
     *   "includeBudgets": true,
     *   "includeCategories": true,
     *   "includeRealEstate": true,
     *   "startDate": "2024-01-01",
     *   "endDate": "2024-12-31",
     *   "includeDeleted": false
     * }
     * </pre>
     *
     * <p>Returns the generated file as an attachment.
     *
     * @param request Export request with format and inclusion options
     * @param authentication Spring Security authentication
     * @return Downloadable JSON or CSV content
     */
    @PostMapping("/export")
    public ResponseEntity<byte[]> exportData(
            @Valid @RequestBody DataExportRequest request, Authentication authentication) {

        log.info("Export data request received for format: {}", request.getFormat());

        // Get user ID from authentication
        User user = (User) authentication.getPrincipal();
        Long userId = user.getId();

        // Perform export
        DataExportService.DataExportFile export = dataExportService.exportUserData(userId, request);
        DataExportResponse response = export.metadata();

        log.info("Export completed for user {}. Export ID: {}", userId, response.getExportId());

        return ResponseEntity.ok()
                .contentType(
                        org.springframework.http.MediaType.parseMediaType(
                                "JSON".equalsIgnoreCase(response.getFormat())
                                        ? "application/json"
                                        : "text/csv;charset=UTF-8"))
                .header(
                        org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        org.springframework.http.ContentDisposition.attachment()
                                .filename(response.getFilename())
                                .build()
                                .toString())
                .contentLength(export.content().length)
                .body(export.content());
    }

    /**
     * Get export statistics for user.
     *
     * <p>Returns information about the user's exportable data without actually performing the
     * export.
     *
     * @param authentication Spring Security authentication
     * @return Statistics response with entity counts
     */
    @GetMapping("/statistics")
    public ResponseEntity<DataExportResponse> getStatistics(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        Long userId = user.getId();

        log.info("Statistics request for user {}", userId);

        // TODO: Implement actual statistics calculation
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }
}
