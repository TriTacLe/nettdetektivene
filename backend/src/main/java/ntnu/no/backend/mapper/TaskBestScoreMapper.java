package ntnu.no.backend.mapper;

import ntnu.no.backend.dto.task.TaskBestScoreResponseDTO;
import ntnu.no.backend.model.TaskBestScore;

/**
 * Utility class for mapping between {@link TaskBestScore} entities and related DTOs.
 *
 * @author tri
 * @version 0.1
 * @since 0.1
 * @see TaskBestScore
 * @see TaskBestScoreResponseDTO
 */
public class TaskBestScoreMapper {

  private TaskBestScoreMapper() {}

  /**
   * Converts a {@link TaskBestScore} entity to a {@link TaskBestScoreResponseDTO}.
   *
   * @param score the task best score entity to convert
   * @return a {@link TaskBestScoreResponseDTO}, or {@code null} if {@code score} is null
   */
  public static TaskBestScoreResponseDTO toDTO(TaskBestScore score) {
    if (score == null) return null;
    return new TaskBestScoreResponseDTO(
      score.getUser().getId(),
      score.getTask().getId(),
      score.getClassroom().getId(),
      score.getBestScore(),
      score.getTotalAttempts(),
      score.getFirstPassedAt(),
      score.getLastAttemptedAt()
    );
  }
}
