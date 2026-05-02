package ntnu.no.backend.mapper;

import ntnu.no.backend.dto.task.ClassroomTaskResponseDTO;
import ntnu.no.backend.model.ClassroomTask;

import java.util.List;
import java.util.UUID;

/**
 * Utility class for mapping between {@link ClassroomTask} and DTOs.
 */
public class ClassroomTaskMapper {

  private ClassroomTaskMapper() {}

  public static ClassroomTaskResponseDTO toDTO(ClassroomTask classroomTask) {
    if (classroomTask == null) return null;

    return new ClassroomTaskResponseDTO(
        classroomTask.getClassroom().getId(),
        classroomTask.getTask().getId(),
        classroomTask.getSortOrder(),
        classroomTask.getIsActive(),
        false,
        TaskMapper.toDTO(classroomTask.getTask())
    );
  }

  public static ClassroomTaskResponseDTO toDTO(ClassroomTask classroomTask, List<UUID> unlockedTaskIds) {
    if (classroomTask == null) return null;

    boolean isUnlocked = unlockedTaskIds != null && unlockedTaskIds.contains(classroomTask.getTask().getId());

    return new ClassroomTaskResponseDTO(
        classroomTask.getClassroom().getId(),
        classroomTask.getTask().getId(),
        classroomTask.getSortOrder(),
        classroomTask.getIsActive(),
        isUnlocked,
        TaskMapper.toDTO(classroomTask.getTask())
    );
  }
}