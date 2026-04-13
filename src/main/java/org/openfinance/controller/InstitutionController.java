package org.openfinance.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openfinance.dto.InstitutionRequest;
import org.openfinance.dto.InstitutionResponse;
import org.openfinance.service.InstitutionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing financial institutions.
 * 
 * <p>Provides CRUD operations for institutions and search functionality.
 * System institutions (default EU banks) cannot be modified.</p>
 * 
 * <p>Requirements: REQ-2.6.1.3 - Predefined Financial Institutions</p>
 */
@RestController
@RequestMapping("/api/v1/institutions")
@RequiredArgsConstructor
@Slf4j
public class InstitutionController {
    
    private final InstitutionService institutionService;
    
    /**
     * Get all institutions.
     * 
     * @return list of all institutions
     */
    @GetMapping
    public ResponseEntity<List<InstitutionResponse>> getAllInstitutions() {
        log.debug("GET /api/v1/institutions");
        List<InstitutionResponse> institutions = institutionService.getAllInstitutions();
        return ResponseEntity.ok(institutions);
    }
    
    /**
     * Get institution by ID.
     * 
     * @param id the institution ID
     * @return the institution
     */
    @GetMapping("/{id}")
    public ResponseEntity<InstitutionResponse> getInstitution(@PathVariable Long id) {
        log.debug("GET /api/v1/institutions/{}", id);
        InstitutionResponse institution = institutionService.getInstitutionById(id);
        return ResponseEntity.ok(institution);
    }
    
    /**
     * Get institutions by country.
     * 
     * @param country the country code
     * @return list of institutions in the country
     */
    @GetMapping("/country/{country}")
    public ResponseEntity<List<InstitutionResponse>> getInstitutionsByCountry(@PathVariable String country) {
        log.debug("GET /api/v1/institutions/country/{}", country);
        List<InstitutionResponse> institutions = institutionService.getInstitutionsByCountry(country);
        return ResponseEntity.ok(institutions);
    }
    
    /**
     * Get system institutions (default EU banks).
     * 
     * @return list of system institutions
     */
    @GetMapping("/system")
    public ResponseEntity<List<InstitutionResponse>> getSystemInstitutions() {
        log.debug("GET /api/v1/institutions/system");
        List<InstitutionResponse> institutions = institutionService.getSystemInstitutions();
        return ResponseEntity.ok(institutions);
    }
    
    /**
     * Get custom (user-created) institutions.
     * 
     * @return list of custom institutions
     */
    @GetMapping("/custom")
    public ResponseEntity<List<InstitutionResponse>> getCustomInstitutions() {
        log.debug("GET /api/v1/institutions/custom");
        List<InstitutionResponse> institutions = institutionService.getCustomInstitutions();
        return ResponseEntity.ok(institutions);
    }
    
    /**
     * Search institutions by name.
     * 
     * @param query the search query
     * @return list of matching institutions
     */
    @GetMapping("/search")
    public ResponseEntity<List<InstitutionResponse>> searchInstitutions(@RequestParam String query) {
        log.debug("GET /api/v1/institutions/search?q={}", query);
        List<InstitutionResponse> institutions = institutionService.searchInstitutions(query);
        return ResponseEntity.ok(institutions);
    }
    
    /**
     * Get distinct country codes.
     * 
     * @return list of country codes
     */
    @GetMapping("/countries")
    public ResponseEntity<List<String>> getCountries() {
        log.debug("GET /api/v1/institutions/countries");
        List<String> countries = institutionService.getCountries();
        return ResponseEntity.ok(countries);
    }
    
    /**
     * Create a new custom institution.
     * 
     * @param request the institution request
     * @return the created institution
     */
    @PostMapping
    public ResponseEntity<InstitutionResponse> createInstitution(@Valid @RequestBody InstitutionRequest request) {
        log.debug("POST /api/v1/institutions");
        InstitutionResponse created = institutionService.createInstitution(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    /**
     * Update an institution.
     * 
     * <p>Only custom institutions can be updated. System institutions return 403.</p>
     * 
     * @param id the institution ID
     * @param request the update request
     * @return the updated institution
     */
    @PutMapping("/{id}")
    public ResponseEntity<InstitutionResponse> updateInstitution(
            @PathVariable Long id,
            @Valid @RequestBody InstitutionRequest request) {
        log.debug("PUT /api/v1/institutions/{}", id);
        InstitutionResponse updated = institutionService.updateInstitution(id, request);
        return ResponseEntity.ok(updated);
    }
    
    /**
     * Delete an institution.
     * 
     * <p>Only custom institutions can be deleted. System institutions return 403.</p>
     * 
     * @param id the institution ID
     * @return no content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInstitution(@PathVariable Long id) {
        log.debug("DELETE /api/v1/institutions/{}", id);
        institutionService.deleteInstitution(id);
        return ResponseEntity.noContent().build();
    }
}
