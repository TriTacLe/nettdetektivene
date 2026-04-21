package ntnu.no.backend.mapper;

import ntnu.no.backend.dto.institution.InstitutionResponseDTO;
import ntnu.no.backend.model.Institution;

/**
 * Utility class for mapping between {@link Institution} entities and institution-related DTOs.
 *
 * @author tri
 * @version 0.1
 * @since 0.1
 */
public class InstitutionMapper {

  private InstitutionMapper() {}

  /**
   * Converts an {@link Institution} entity to an {@link InstitutionResponseDTO}.
   *
   * @param institution the institution entity to convert
   * @return an {@link InstitutionResponseDTO}, or {@code null} if {@code institution} is null
   */
  public static InstitutionResponseDTO toDTO(Institution institution) {
    if (institution == null) return null;
    return new InstitutionResponseDTO(
        institution.getId(),
        institution.getInstName(),
        institution.getCreatedAt(),
        institution.getUpdatedAt()
    );
  }
}
