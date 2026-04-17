package ntnu.no.backend.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import ntnu.no.backend.dto.task.TaskRequestDTO;
import ntnu.no.backend.dto.task.TaskResponseDTO;
import ntnu.no.backend.mapper.TaskMapper;
import ntnu.no.backend.model.Task;
import ntnu.no.backend.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

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

  /**
   * Returns all tasks.
   *
   * @return list of all tasks as DTOs
   */
  @Transactional(readOnly = true)
  public List<TaskResponseDTO> getAllTask() {
    return taskRepository.findAll().stream().map(TaskMapper::toDTO).toList();
  }

  /**
   * Returns a single task by ID.
   *
   * @param id the task ID
   * @return the task as a DTO
   * @throws EntityNotFoundException if no task found with the given ID
   */
  @Transactional(readOnly = true)
  public TaskResponseDTO getTask(UUID id) {
    Task task = taskRepository.findById(id)
      .orElseThrow(() -> new EntityNotFoundException("No task found with id: " + id));
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
   * Updates an existing task.
   *
   * @param dto the request DTO with updated values
   * @param id  the task ID
   * @return the updated task as a DTO
   * @throws EntityNotFoundException if no task found with the given ID
   */
  @Transactional
  public TaskResponseDTO updateTask(TaskRequestDTO dto, UUID id) {
    Task task = taskRepository.findById(id)
      .orElseThrow(() -> new EntityNotFoundException("No task found with id: " + id));
    TaskMapper.updateEntity(task, dto);
    return TaskMapper.toDTO(task);
  }

  /**
   * Deletes a task by ID.
   *
   * @param id the task ID
   * @throws EntityNotFoundException if no task found with the given ID
   */
  @Transactional
  public void deleteTask(UUID id) {
    if (!taskRepository.existsById(id)) {
      throw new EntityNotFoundException("No task found with id: " + id);
    }
    taskRepository.deleteById(id);
  }
}
