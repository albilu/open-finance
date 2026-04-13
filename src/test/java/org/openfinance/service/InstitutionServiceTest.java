package org.openfinance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.openfinance.service.OperationHistoryService;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openfinance.dto.InstitutionRequest;
import org.openfinance.dto.InstitutionResponse;
import org.openfinance.entity.Institution;
import org.openfinance.exception.InstitutionNotFoundException;
import org.openfinance.repository.InstitutionRepository;

/**
 * Unit tests for InstitutionService.
 *
 * <p>
 * Tests business logic for institution CRUD operations, including
 * system institution protection and validation.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class InstitutionServiceTest {

    @Mock
    private InstitutionRepository institutionRepository;

    @Mock
    private OperationHistoryService operationHistoryService;

    @InjectMocks
    private InstitutionService institutionService;

    private Institution systemInstitution;
    private Institution customInstitution;
    private InstitutionRequest validRequest;

    @BeforeEach
    void setUp() {
        systemInstitution = Institution.builder()
                .id(1L)
                .name("BNP Paribas")
                .bic("BNPAFRPP")
                .country("FR")
                .logo("base64logo")
                .isSystem(true)
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .build();

        customInstitution = Institution.builder()
                .id(2L)
                .name("My Custom Bank")
                .bic("CUSTFRPP")
                .country("FR")
                .logo("customlogo")
                .isSystem(false)
                .createdAt(LocalDateTime.now().minusHours(1))
                .updatedAt(LocalDateTime.now().minusHours(1))
                .build();

        validRequest = InstitutionRequest.builder()
                .name("New Bank")
                .bic("NEWBFRPP")
                .country("FR")
                .logo("newlogo")
                .build();
    }

    @Test
    void shouldGetAllInstitutions() {
        // Arrange
        List<Institution> institutions = List.of(systemInstitution, customInstitution);
        when(institutionRepository.findAllByOrderByCountryAscNameAsc()).thenReturn(institutions);

        // Act
        List<InstitutionResponse> result = institutionService.getAllInstitutions();

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(1).getId()).isEqualTo(2L);
        verify(institutionRepository).findAllByOrderByCountryAscNameAsc();
    }

    @Test
    void shouldGetInstitutionByIdWhenFound() {
        // Arrange
        when(institutionRepository.findById(1L)).thenReturn(Optional.of(systemInstitution));

        // Act
        InstitutionResponse result = institutionService.getInstitutionById(1L);

        // Assert
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("BNP Paribas");
        assertThat(result.getIsSystem()).isTrue();
        verify(institutionRepository).findById(1L);
    }

    @Test
    void shouldThrowWhenInstitutionNotFound() {
        // Arrange
        when(institutionRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(InstitutionNotFoundException.class,
                () -> institutionService.getInstitutionById(999L));
        verify(institutionRepository).findById(999L);
    }

    @Test
    void shouldGetInstitutionsByCountry() {
        // Arrange
        List<Institution> frenchInstitutions = List.of(systemInstitution, customInstitution);
        when(institutionRepository.findByCountryOrderByNameAsc("FR")).thenReturn(frenchInstitutions);

        // Act
        List<InstitutionResponse> result = institutionService.getInstitutionsByCountry("FR");

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getCountry()).isEqualTo("FR");
        verify(institutionRepository).findByCountryOrderByNameAsc("FR");
    }

    @Test
    void shouldGetSystemInstitutions() {
        // Arrange
        List<Institution> systemInstitutions = List.of(systemInstitution);
        when(institutionRepository.findByIsSystemTrueOrderByCountryAscNameAsc()).thenReturn(systemInstitutions);

        // Act
        List<InstitutionResponse> result = institutionService.getSystemInstitutions();

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getIsSystem()).isTrue();
        verify(institutionRepository).findByIsSystemTrueOrderByCountryAscNameAsc();
    }

    @Test
    void shouldGetCustomInstitutions() {
        // Arrange
        List<Institution> customInstitutions = List.of(customInstitution);
        when(institutionRepository.findByIsSystemFalseOrderByNameAsc()).thenReturn(customInstitutions);

        // Act
        List<InstitutionResponse> result = institutionService.getCustomInstitutions();

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getIsSystem()).isFalse();
        verify(institutionRepository).findByIsSystemFalseOrderByNameAsc();
    }

    @Test
    void shouldSearchInstitutions() {
        // Arrange
        List<Institution> searchResults = List.of(systemInstitution);
        when(institutionRepository.searchByName("BNP")).thenReturn(searchResults);

        // Act
        List<InstitutionResponse> result = institutionService.searchInstitutions("BNP");

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("BNP Paribas");
        verify(institutionRepository).searchByName("BNP");
    }

    @Test
    void shouldGetCountries() {
        // Arrange
        List<String> countries = List.of("FR", "DE", "IT");
        when(institutionRepository.findDistinctCountries()).thenReturn(countries);

        // Act
        List<String> result = institutionService.getCountries();

        // Assert
        assertThat(result).containsExactly("FR", "DE", "IT");
        verify(institutionRepository).findDistinctCountries();
    }

    @Test
    void shouldCreateInstitutionSuccessfully() {
        // Arrange
        Institution savedInstitution = Institution.builder()
                .id(3L)
                .name("New Bank")
                .bic("NEWBFRPP")
                .country("FR")
                .logo("newlogo")
                .isSystem(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(institutionRepository.save(any(Institution.class))).thenReturn(savedInstitution);

        // Act
        InstitutionResponse result = institutionService.createInstitution(validRequest);

        // Assert
        assertThat(result.getId()).isEqualTo(3L);
        assertThat(result.getName()).isEqualTo("New Bank");
        assertThat(result.getIsSystem()).isFalse();
        verify(institutionRepository).save(any(Institution.class));
    }

    @Test
    void shouldUpdateInstitutionSuccessfully() {
        // Arrange
        InstitutionRequest updateRequest = InstitutionRequest.builder()
                .name("Updated Bank")
                .bic("UPDTFRPP")
                .country("FR")
                .logo("updatedlogo")
                .build();

        Institution updatedInstitution = Institution.builder()
                .id(2L)
                .name("Updated Bank")
                .bic("UPDTFRPP")
                .country("FR")
                .logo("updatedlogo")
                .isSystem(false)
                .createdAt(customInstitution.getCreatedAt())
                .updatedAt(LocalDateTime.now())
                .build();

        when(institutionRepository.findById(2L)).thenReturn(Optional.of(customInstitution));
        when(institutionRepository.save(any(Institution.class))).thenReturn(updatedInstitution);

        // Act
        InstitutionResponse result = institutionService.updateInstitution(2L, updateRequest);

        // Assert
        assertThat(result.getName()).isEqualTo("Updated Bank");
        assertThat(result.getBic()).isEqualTo("UPDTFRPP");
        verify(institutionRepository).findById(2L);
        verify(institutionRepository).save(any(Institution.class));
    }

    @Test
    void shouldThrowWhenUpdatingNonExistentInstitution() {
        // Arrange
        when(institutionRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(InstitutionNotFoundException.class,
                () -> institutionService.updateInstitution(999L, validRequest));
        verify(institutionRepository).findById(999L);
        verify(institutionRepository, never()).save(any(Institution.class));
    }

    @Test
    void shouldThrowWhenUpdatingSystemInstitution() {
        // Arrange
        when(institutionRepository.findById(1L)).thenReturn(Optional.of(systemInstitution));

        // Act & Assert
        assertThrows(IllegalStateException.class,
                () -> institutionService.updateInstitution(1L, validRequest));
        verify(institutionRepository).findById(1L);
        verify(institutionRepository, never()).save(any(Institution.class));
    }

    @Test
    void shouldDeleteInstitutionSuccessfully() {
        // Arrange
        when(institutionRepository.findById(2L)).thenReturn(Optional.of(customInstitution));
        when(institutionRepository.isInUse(2L)).thenReturn(false);

        // Act
        institutionService.deleteInstitution(2L);

        // Assert
        verify(institutionRepository).findById(2L);
        verify(institutionRepository).isInUse(2L);
        verify(institutionRepository).delete(customInstitution);
    }

    @Test
    void shouldThrowWhenDeletingNonExistentInstitution() {
        // Arrange
        when(institutionRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(InstitutionNotFoundException.class,
                () -> institutionService.deleteInstitution(999L));
        verify(institutionRepository).findById(999L);
        verify(institutionRepository, never()).delete(any(Institution.class));
    }

    @Test
    void shouldThrowWhenDeletingSystemInstitution() {
        // Arrange
        when(institutionRepository.findById(1L)).thenReturn(Optional.of(systemInstitution));

        // Act & Assert
        assertThrows(IllegalStateException.class,
                () -> institutionService.deleteInstitution(1L));
        verify(institutionRepository).findById(1L);
        verify(institutionRepository, never()).isInUse(anyLong());
        verify(institutionRepository, never()).delete(any(Institution.class));
    }

    @Test
    void shouldThrowWhenInstitutionInUse() {
        // Arrange
        when(institutionRepository.findById(2L)).thenReturn(Optional.of(customInstitution));
        when(institutionRepository.isInUse(2L)).thenReturn(true);

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> {
            institutionService.deleteInstitution(2L);
        });
        verify(institutionRepository).findById(2L);
        verify(institutionRepository).isInUse(2L);
        verify(institutionRepository, never()).delete(any(Institution.class));
    }

    @Test
    void shouldReturnTrueWhenInstitutionExists() {
        // Arrange
        when(institutionRepository.existsById(1L)).thenReturn(true);

        // Act
        boolean result = institutionService.existsById(1L);

        // Assert
        assertThat(result).isTrue();
        verify(institutionRepository).existsById(1L);
    }

    @Test
    void shouldReturnFalseWhenInstitutionDoesNotExist() {
        // Arrange
        when(institutionRepository.existsById(999L)).thenReturn(false);

        // Act
        boolean result = institutionService.existsById(999L);

        // Assert
        assertThat(result).isFalse();
        verify(institutionRepository).existsById(999L);
    }
}