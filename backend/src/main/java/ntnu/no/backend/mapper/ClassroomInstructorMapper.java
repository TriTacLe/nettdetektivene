package ntnu.no.backend.mapper;

import ntnu.no.backend.dto.classroom.ClassroomInstructorResponseDTO;
import ntnu.no.backend.model.classroom.ClassroomInstructor;

/**
 * Utility class for mapping between {@link ClassroomInstructor} entities and DTOs.
 *
 * @author norakirknes
 * @version 0.1
 * @since 0.1
 * @see ClassroomInstructor
 * @see ClassroomInstructorResponseDTO
 */
public class ClassroomInstructorMapper {

  private ClassroomInstructorMapper() {}

  /**
   * Converts a {@link ClassroomInstructor} entity to a {@link ClassroomInstructorResponseDTO}.
   *
   * @param instructor the classroom instructor entity to convert
   * @return a {@link ClassroomInstructorResponseDTO}, or {@code null} if {@code instructor} is null
   */
  public static ClassroomInstructorResponseDTO toDTO(ClassroomInstructor instructor) {
    if (instructor == null) return null;
    return new ClassroomInstructorResponseDTO(
      instructor.getId().getUserId(),
      instructor.getAssignedAt()
    );
  }
}
