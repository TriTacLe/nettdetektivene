package ntnu.no.backend.service;

import lombok.RequiredArgsConstructor;
import ntnu.no.backend.dto.institution.InstitutionResponseDTO;
import ntnu.no.backend.mapper.InstitutionMapper;
import ntnu.no.backend.repository.InstitutionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for managing institutions.
 *
 * @author tri
 * @version 0.1
 * @since 0.1
 */
@Service
@RequiredArgsConstructor
public class InstitutionService {

  private final InstitutionRepository institutionRepository;

  /**
   * Returns all institutions.
   *
   * @return list of all institutions as DTOs
   */
  @Transactional(readOnly = true)
  public List<InstitutionResponseDTO> getAllInstitutions() {
    return institutionRepository.findAll().stream()
        .map(InstitutionMapper::toDTO)
        .toList();
  }
}
