package ntnu.no.backend.mapper;

import ntnu.no.backend.dto.notebook.NotebookEntryRequestDTO;
import ntnu.no.backend.dto.notebook.NotebookEntryResponseDTO;
import ntnu.no.backend.model.NotebookEntry;

/**
 * Utility class for mapping between {@link NotebookEntry} entities and related DTOs.
 *
 * @author tri
 * @version 0.1
 * @since 0.1
 * @see NotebookEntry
 * @see NotebookEntryRequestDTO
 * @see NotebookEntryResponseDTO
 */
public class NotebookEntryMapper {

  private NotebookEntryMapper() {}

  /**
   * Converts a {@link NotebookEntry} entity to a {@link NotebookEntryResponseDTO}.
   *
   * @param entry the notebook entry entity to convert
   * @return a {@link NotebookEntryResponseDTO}, or {@code null} if {@code entry} is null
   */
  public static NotebookEntryResponseDTO toDTO(NotebookEntry entry) {
    if (entry == null) return null;
    return new NotebookEntryResponseDTO(
      entry.getId(),
      entry.getUser().getId(),
      entry.getClassroom().getId(),
      entry.getStopLocation(),
      entry.getAutoNotes(),
      entry.getStudentReflection(),
      entry.getCreatedAt(),
      entry.getUpdatedAt()
    );
  }

  /**
   * Updates the fields of a {@link NotebookEntry} entity from a {@link NotebookEntryRequestDTO}.
   *
   * @param entry the notebook entry entity to update
   * @param dto   the DTO containing the new values
   */
  public static void updateEntity(NotebookEntry entry, NotebookEntryRequestDTO dto) {
    if (entry == null || dto == null) return;
    entry.setAutoNotes(dto.getAutoNotes());
    entry.setStudentReflection(dto.getStudentReflection());
  }
}
