package ntnu.no.backend.mapper;

import ntnu.no.backend.dto.task.TaskRequestDTO;
import ntnu.no.backend.dto.task.TaskResponseDTO;
import ntnu.no.backend.model.Task;

/**
 * Utility class for mapping between {@link Task} entities and task-related DTOs.
 *
 * @author tri
 * @version 0.1
 * @since 0.1
 * @see Task
 * @see TaskRequestDTO
 * @see TaskResponseDTO
 */
public class TaskMapper {

  private TaskMapper() {}

  /**
   * Converts a {@link Task} entity to a {@link TaskResponseDTO}.
   *
   * @param task the task entity to convert
   * @return a {@link TaskResponseDTO}, or {@code null} if {@code task} is null
   */
  public static TaskResponseDTO toDTO(Task task) {
    if (task == null) return null;
    return new TaskResponseDTO(
      task.getId(),
      task.getTitle(),
      task.getTaskType(),
      task.getStopLocation(),
      task.getDifficulty(),
      task.getContent(),
      task.isActive(),
      task.getCreatedAt(),
      task.getUpdatedAt()
    );
  }

  /**
   * Converts a {@link TaskRequestDTO} to a {@link Task} entity.
   *
   * @param dto the request DTO to convert
   * @return a {@link Task} entity, or {@code null} if {@code dto} is null
   */
  public static Task toEntity(TaskRequestDTO dto) {
    if (dto == null) return null;
    return Task.builder()
      .title(dto.getTitle())
      .taskType(dto.getTaskType())
      .stopLocation(dto.getStopLocation())
      .difficulty(dto.getDifficulty())
      .content(dto.getContent())
      .build();
  }

  /**
   * Updates the fields of a {@link Task} entity from a {@link TaskRequestDTO}.
   *
   * @param task the task entity to update
   * @param dto  the DTO containing the new values
   */
  public static void updateEntity(Task task, TaskRequestDTO dto) {
    if (task == null || dto == null) return;
    task.setTitle(dto.getTitle());
    task.setTaskType(dto.getTaskType());
    task.setStopLocation(dto.getStopLocation());
    task.setDifficulty(dto.getDifficulty());
    task.setContent(dto.getContent());
  }
}
