package ntnu.no.backend.service;

import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import ntnu.no.backend.dto.task.TaskRequestDTO;
import ntnu.no.backend.dto.task.TaskResponseDTO;
import ntnu.no.backend.mapper.TaskMapper;
import ntnu.no.backend.model.Task;
import ntnu.no.backend.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service for managing tasks.
 *
 * @author tri
 * @version 0.1
 * @since 0.1
 */
@Service
@RequiredArgsConstructor
public class TaskService {

  private final TaskRepository taskRepository;
  private final TaskImageStorageService taskImageStorageService;

  /**
   * Returns all active tasks (excluding soft-deleted).
   *
   * @return list of all active tasks as DTOs
   */
  @Transactional(readOnly = true)
  public List<TaskResponseDTO> getAllTask() {
    return taskRepository.findAllActive().stream().map(TaskMapper::toDTO).toList();
  }

  /**
   * Returns a single active task by ID (excluding soft-deleted).
   *
   * @param id the task ID
   * @return the task as a DTO
   * @throws EntityNotFoundException if no active task found with the given ID
   */
  @Transactional(readOnly = true)
  public TaskResponseDTO getTask(UUID id) {
    Task task = taskRepository.findByIdAndDeletedAtIsNull(id)
      .orElseThrow(() -> new EntityNotFoundException("No active task found with id: " + id));
    return TaskMapper.toDTO(task);
  }

  /**
   * Creates a new task.
   *
   * @param dto the request DTO
   * @return the created task as a DTO
   */
  @Transactional
  public TaskResponseDTO createTask(TaskRequestDTO dto) {
    Task task = TaskMapper.toEntity(dto);
    taskRepository.save(task);
    return TaskMapper.toDTO(task);
  }

  /**
   * Creates a new task with optional image uploads.
   *
   * @param dto    the request DTO
   * @param images uploaded images referenced from content
   * @return the created task as a DTO
   */
  @Transactional
  public TaskResponseDTO createTask(TaskRequestDTO dto, List<MultipartFile> images) {
    TaskRequestDTO resolvedDto = withResolvedContent(dto, images);
    return createTask(resolvedDto);
  }

  /**
   * Soft deletes a task by ID (sets deletedAt timestamp instead of hard delete).
   * This preserves foreign key references in classroom_task, task_attempt, and task_best_score tables.
   *
   * @param id the task ID
   * @throws EntityNotFoundException if no active task found with the given ID
   */
  @Transactional
  public void deleteTask(UUID id) {
    Task task = taskRepository.findByIdAndDeletedAtIsNull(id)
      .orElseThrow(() -> new EntityNotFoundException("No active task found with id: " + id));
    task.setDeletedAt(LocalDateTime.now());
    taskRepository.save(task);
  }

  /**
   * Updates an existing active task.
   *
   * @param dto the request DTO with updated values
   * @param id  the task ID
   * @return the updated task as a DTO
   * @throws EntityNotFoundException if no active task found with the given ID
   */
  @Transactional
  public TaskResponseDTO updateTask(TaskRequestDTO dto, UUID id) {
    Task task = taskRepository.findByIdAndDeletedAtIsNull(id)
      .orElseThrow(() -> new EntityNotFoundException("No active task found with id: " + id));
    TaskMapper.updateEntity(task, dto);
    return TaskMapper.toDTO(task);
  }

  /**
   * Updates an existing active task with optional image uploads.
   *
   * @param dto    the request DTO with updated values
   * @param id     the task ID
   * @param images uploaded images referenced from content
   * @return the updated task as a DTO
   * @throws EntityNotFoundException if no active task found with the given ID
   */
  @Transactional
  public TaskResponseDTO updateTask(TaskRequestDTO dto, UUID id, List<MultipartFile> images) {
    TaskRequestDTO resolvedDto = withResolvedContent(dto, images);
    return updateTask(resolvedDto, id);
  }

  private TaskRequestDTO withResolvedContent(TaskRequestDTO dto, List<MultipartFile> images) {
    if (dto == null) {
      return null;
    }

    return TaskRequestDTO.builder()
      .title(dto.getTitle())
      .taskType(dto.getTaskType())
      .stopLocation(dto.getStopLocation())
      .difficulty(dto.getDifficulty())
      .content(taskImageStorageService.replaceUploadReferences(dto.getContent(), images))
      .build();
  }
}
