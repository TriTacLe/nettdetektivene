package ntnu.no.backend.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import ntnu.no.backend.dto.institution.InstitutionRequestDTO;
import ntnu.no.backend.dto.institution.InstitutionResponseDTO;
import ntnu.no.backend.mapper.InstitutionMapper;
import ntnu.no.backend.model.Institution;
import ntnu.no.backend.repository.InstitutionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

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
   * Returns all active institutions.
   *
   * @return list of all non-deleted institutions as DTOs
   */
  @Transactional(readOnly = true)
  public List<InstitutionResponseDTO> getAllInstitutions() {
    return institutionRepository.findAllByDeletedAtIsNull().stream()
        .map(InstitutionMapper::toDTO)
        .toList();
  }

  /**
   * Returns a single active institution by ID.
   *
   * @param id the institution ID
   * @return the institution as a DTO
   * @throws EntityNotFoundException if no active institution found with the given ID
   */
  @Transactional(readOnly = true)
  public InstitutionResponseDTO getInstitutionById(UUID id) {
    Institution institution = institutionRepository.findByIdAndDeletedAtIsNull(id)
        .orElseThrow(() -> new EntityNotFoundException("Institution not found with id: " + id));
    return InstitutionMapper.toDTO(institution);
  }

  /**
   * Creates a new institution.
   *
   * @param dto the request DTO
   * @return the created institution as a DTO
   */
  @Transactional
  public InstitutionResponseDTO createInstitution(InstitutionRequestDTO dto) {
    Institution institution = Institution.builder()
        .instName(dto.getInstName())
        .build();
    institutionRepository.save(institution);
    return InstitutionMapper.toDTO(institution);
  }

  /**
   * Updates an existing institution.
   *
   * @param id  the institution ID
   * @param dto the request DTO with updated values
   * @return the updated institution as a DTO
   * @throws EntityNotFoundException if no active institution found with the given ID
   */
  @Transactional
  public InstitutionResponseDTO updateInstitution(UUID id, InstitutionRequestDTO dto) {
    Institution institution = institutionRepository.findByIdAndDeletedAtIsNull(id)
        .orElseThrow(() -> new EntityNotFoundException("Institution not found with id: " + id));
    institution.setInstName(dto.getInstName());
    return InstitutionMapper.toDTO(institution);
  }

  /**
   * Soft-deletes an institution by setting its {@code deletedAt} timestamp.
   *
   * @param id the institution ID
   * @throws EntityNotFoundException if no active institution found with the given ID
   */
  @Transactional
  public void deleteInstitution(UUID id) {
    Institution institution = institutionRepository.findByIdAndDeletedAtIsNull(id)
        .orElseThrow(() -> new EntityNotFoundException("Institution not found with id: " + id));
    institution.setDeletedAt(LocalDateTime.now());
  }
}
