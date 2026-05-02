package ntnu.no.backend.mapper;

import ntnu.no.backend.dto.classroom.ClassroomStudentResponseDTO;
import ntnu.no.backend.model.classroom.ClassroomStudent;

/**
 * Utility class for mapping between {@link ClassroomStudent} entities and DTOs.
 *
 * @author norakirknes
 * @version 0.1
 * @since 0.1
 * @see ClassroomStudent
 * @see ClassroomStudentResponseDTO
 */
public class ClassroomStudentMapper {

  private ClassroomStudentMapper() {
  }

  /**
   * Converts a {@link ClassroomStudent} entity to a
   * {@link ClassroomStudentResponseDTO}.
   *
   * @param student the classroom student entity to convert
   * @return a {@link ClassroomStudentResponseDTO}, or {@code null} if
   *         {@code student} is null
   */
  public static ClassroomStudentResponseDTO toDTO(ClassroomStudent student, String feideId) {
    if (student == null)
      return null;
    return new ClassroomStudentResponseDTO(
        student.getId().getClassroomId(),
        student.getId().getUserId(),
        student.getDisplayName(),
        student.isAllowed(),
        student.getNameChangeStatus(),
        student.getRequestedDisplayName(),
        student.getJoinedAt(),
        feideId);
  }
}
