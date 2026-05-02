package ntnu.no.backend.mapper;

import ntnu.no.backend.dto.task.TaskAttemptResponseDTO;
import ntnu.no.backend.model.TaskAttempt;

/**
 * Utility class for mapping between {@link TaskAttempt} entities and task-attempt-related DTOs.
 *
 * @author tri
 * @version 0.1
 * @since 0.1
 * @see TaskAttempt
 * @see TaskAttemptResponseDTO
 */
public class TaskAttemptMapper {

  private TaskAttemptMapper() {}

  /**
   * Converts a {@link TaskAttempt} entity to a {@link TaskAttemptResponseDTO}.
   *
   * @param attempt the task attempt entity to convert
   * @return a {@link TaskAttemptResponseDTO}, or {@code null} if {@code attempt} is null
   */
  public static TaskAttemptResponseDTO toDTO(TaskAttempt attempt) {
    if (attempt == null) return null;
    return new TaskAttemptResponseDTO(
      attempt.getId(),
      attempt.getUser().getId(),
      attempt.getTask().getId(),
      attempt.getClassroom().getId(),
      attempt.getScore(),
      attempt.isPassed(),
      attempt.getAttemptedAt()
    );
  }
}
