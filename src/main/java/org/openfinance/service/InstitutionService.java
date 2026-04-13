package org.openfinance.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openfinance.dto.InstitutionRequest;
import org.openfinance.dto.InstitutionResponse;
import org.openfinance.entity.Institution;
import org.openfinance.exception.InstitutionNotFoundException;
import org.openfinance.repository.InstitutionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service layer for managing financial institutions.
 * 
 * <p>This service handles business logic for institution CRUD operations:
 * <ul>
 *   <li>Creating new institutions (custom/user-created)</li>
 *   <li>Updating existing institutions (custom only)</li>
 *   <li>Deleting institutions (custom only - system institutions protected)</li>
 *   <li>Retrieving institutions with filters</li>
 *   <li>Searching institutions by name</li>
 * </ul>
 * 
 * <p>Requirements: REQ-2.6.1.3 - Predefined Financial Institutions</p>
 * 
 * @see org.openfinance.entity.Institution
 * @see org.openfinance.dto.InstitutionRequest
 * @see org.openfinance.dto.InstitutionResponse
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class InstitutionService {
    
    private final InstitutionRepository institutionRepository;
    
    /**
     * Get all institutions.
     * 
     * @return list of all institutions ordered by country and name
     */
    @Transactional(readOnly = true)
    public List<InstitutionResponse> getAllInstitutions() {
        log.debug("Fetching all institutions");
        return institutionRepository.findAllByOrderByCountryAscNameAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }
    
    /**
     * Get institution by ID.
     * 
     * @param id the institution ID
     * @return the institution
     * @throws InstitutionNotFoundException if not found
     */
    @Transactional(readOnly = true)
    public InstitutionResponse getInstitutionById(Long id) {
        log.debug("Fetching institution by id: {}", id);
        Institution institution = institutionRepository.findById(id)
                .orElseThrow(() -> new InstitutionNotFoundException(id));
        return toResponse(institution);
    }
    
    /**
     * Get institutions by country.
     * 
     * @param country the country code (ISO 3166-1 alpha-2)
     * @return list of institutions in the specified country
     */
    @Transactional(readOnly = true)
    public List<InstitutionResponse> getInstitutionsByCountry(String country) {
        log.debug("Fetching institutions by country: {}", country);
        return institutionRepository.findByCountryOrderByNameAsc(country)
                .stream()
                .map(this::toResponse)
                .toList();
    }
    
    /**
     * Get system institutions (default EU banks).
     * 
     * @return list of system institutions
     */
    @Transactional(readOnly = true)
    public List<InstitutionResponse> getSystemInstitutions() {
        log.debug("Fetching system institutions");
        return institutionRepository.findByIsSystemTrueOrderByCountryAscNameAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }
    
    /**
     * Get custom (user-created) institutions.
     * 
     * @return list of custom institutions
     */
    @Transactional(readOnly = true)
    public List<InstitutionResponse> getCustomInstitutions() {
        log.debug("Fetching custom institutions");
        return institutionRepository.findByIsSystemFalseOrderByNameAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }
    
    /**
     * Search institutions by name.
     * 
     * @param query the search query
     * @return list of matching institutions
     */
    @Transactional(readOnly = true)
    public List<InstitutionResponse> searchInstitutions(String query) {
        log.debug("Searching institutions by name: {}", query);
        return institutionRepository.searchByName(query)
                .stream()
                .map(this::toResponse)
                .toList();
    }
    
    /**
     * Get distinct country codes from institutions.
     * 
     * @return list of country codes
     */
    @Transactional(readOnly = true)
    public List<String> getCountries() {
        log.debug("Fetching distinct countries");
        return institutionRepository.findDistinctCountries();
    }
    
    /**
     * Create a new custom institution.
     * 
     * <p>User-created institutions are marked as non-system.</p>
     * 
     * @param request the institution request
     * @return the created institution
     */
    public InstitutionResponse createInstitution(InstitutionRequest request) {
        log.debug("Creating institution: {}", request.getName());
        
        Institution institution = Institution.builder()
                .name(request.getName())
                .bic(request.getBic())
                .country(request.getCountry())
                .logo(request.getLogo())
                .isSystem(false)
                .build();
        
        Institution saved = institutionRepository.save(institution);
        log.info("Created custom institution with id: {}", saved.getId());
        return toResponse(saved);
    }
    
    /**
     * Update an existing institution.
     * 
     * <p>Only custom (non-system) institutions can be updated.</p>
     * 
     * @param id the institution ID
     * @param request the update request
     * @return the updated institution
     * @throws InstitutionNotFoundException if not found
     * @throws IllegalStateException if trying to update a system institution
     */
    public InstitutionResponse updateInstitution(Long id, InstitutionRequest request) {
        log.debug("Updating institution id: {}", id);
        
        Institution institution = institutionRepository.findById(id)
                .orElseThrow(() -> new InstitutionNotFoundException(id));
        
        // Prevent updating system institutions
        if (Boolean.TRUE.equals(institution.getIsSystem())) {
            throw new IllegalStateException("Cannot update system institutions");
        }
        
        institution.setName(request.getName());
        institution.setBic(request.getBic());
        institution.setCountry(request.getCountry());
        institution.setLogo(request.getLogo());
        
        Institution saved = institutionRepository.save(institution);
        log.info("Updated institution id: {}", saved.getId());
        return toResponse(saved);
    }
    
    /**
     * Delete an institution.
     * 
     * <p>Only custom (non-system) institutions can be deleted.
     * Also checks if institution is in use by any accounts.</p>
     * 
     * @param id the institution ID
     * @throws InstitutionNotFoundException if not found
     * @throws IllegalStateException if trying to delete a system institution
     */
    public void deleteInstitution(Long id) {
        log.debug("Deleting institution id: {}", id);
        
        Institution institution = institutionRepository.findById(id)
                .orElseThrow(() -> new InstitutionNotFoundException(id));
        
        // Prevent deleting system institutions
        if (Boolean.TRUE.equals(institution.getIsSystem())) {
            throw new IllegalStateException("Cannot delete system institutions");
        }
        
        // Check if institution is in use
        if (institutionRepository.isInUse(id)) {
            throw new IllegalStateException("Cannot delete institution that is associated with accounts");
        }
        
        institutionRepository.delete(institution);
        log.info("Deleted institution id: {}", id);
    }
    
    /**
     * Check if institution exists.
     * 
     * @param id the institution ID
     * @return true if exists
     */
    @Transactional(readOnly = true)
    public boolean existsById(Long id) {
        return institutionRepository.existsById(id);
    }
    
    /**
     * Convert entity to response DTO.
     */
    private InstitutionResponse toResponse(Institution institution) {
        return InstitutionResponse.builder()
                .id(institution.getId())
                .name(institution.getName())
                .bic(institution.getBic())
                .country(institution.getCountry())
                .logo(institution.getLogo())
                .isSystem(institution.getIsSystem())
                .createdAt(institution.getCreatedAt())
                .updatedAt(institution.getUpdatedAt())
                .build();
    }
}
