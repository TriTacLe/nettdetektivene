package ntnu.no.backend.mapper;

import ntnu.no.backend.dto.classroom.ClassroomResponseDTO;
import ntnu.no.backend.model.Classroom;

/**
 * Utility class for mapping between {@link Classroom} entities and classroom-related DTOs.
 *
 * @author tri
 * @version 0.1
 * @since 0.1
 */
public class ClassroomMapper {

  private ClassroomMapper() {}

  /**
   * Converts a {@link Classroom} entity to a {@link ClassroomResponseDTO}.
   *
   * @param classroom the classroom entity to convert
   * @return a {@link ClassroomResponseDTO}, or {@code null} if {@code classroom} is null
   */
  public static ClassroomResponseDTO toDTO(Classroom classroom) {
    if (classroom == null) return null;
    return new ClassroomResponseDTO(
      classroom.getId(),
      classroom.getTitle(),
      classroom.getClassDesc(),
      classroom.getJoinCode(),
      classroom.getInstitution().getId(),
      classroom.getCreatedBy().getId(),
      classroom.isGameEnabled(),
      classroom.getCreatedAt(),
      classroom.getUpdatedAt()
    );
  }
}
