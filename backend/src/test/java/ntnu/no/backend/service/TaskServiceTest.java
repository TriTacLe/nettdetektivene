package ntnu.no.backend.service;

import jakarta.persistence.EntityNotFoundException;
import ntnu.no.backend.dto.task.TaskRequestDTO;
import ntnu.no.backend.dto.task.TaskResponseDTO;
import ntnu.no.backend.model.Task;
import ntnu.no.backend.model.enums.TaskType;
import ntnu.no.backend.repository.TaskRepository;
import ntnu.no.backend.util.TaskTestFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link TaskService}.
 *
 * @author tri
 * @version 0.1
 * @since 0.1
 */
@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

  @Mock
  private TaskRepository taskRepository;

  @InjectMocks
  private TaskService taskService;

  @Test
  void getAllTask_returnsMappedDTOs() {
    when(taskRepository.findAll()).thenReturn(List.of(TaskTestFactory.task()));

    List<TaskResponseDTO> result = taskService.getAllTask();

    assertThat(result).hasSize(1);
    assertThat(result.get(0).title()).isEqualTo("Spot the phishing email");
  }

  @Test
  void getTask_returnsDTO_whenFound() {
    UUID id = UUID.randomUUID();
    when(taskRepository.findById(id)).thenReturn(Optional.of(TaskTestFactory.task()));

    TaskResponseDTO result = taskService.getTask(id);

    assertThat(result.title()).isEqualTo("Spot the phishing email");
  }

  @Test
  void getTask_throwsEntityNotFoundException_whenNotFound() {
    UUID id = UUID.randomUUID();
    when(taskRepository.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> taskService.getTask(id))
      .isInstanceOf(EntityNotFoundException.class)
      .hasMessageContaining(id.toString());
  }

  @Test
  void createTask_savesAndReturnsDTO() {
    when(taskRepository.save(any(Task.class))).thenAnswer(i -> i.getArgument(0));

    TaskResponseDTO result = taskService.createTask(TaskTestFactory.requestDTO());

    assertThat(result.title()).isEqualTo("Spot the phishing email");
    verify(taskRepository).save(any(Task.class));
  }

  @Test
  void updateTask_updatesAndReturnsDTO() {
    UUID id = UUID.randomUUID();
    TaskRequestDTO dto = TaskRequestDTO.builder()
      .title("New title")
      .taskType(TaskType.EMAIL)
      .difficulty(2)
      .content("{}")
      .build();

    when(taskRepository.findById(id)).thenReturn(Optional.of(TaskTestFactory.task()));

    TaskResponseDTO result = taskService.updateTask(dto, id);

    assertThat(result.title()).isEqualTo("New title");
  }

  @Test
  void deleteTask_deletesWhenFound() {
    UUID id = UUID.randomUUID();
    when(taskRepository.existsById(id)).thenReturn(true);

    taskService.deleteTask(id);

    verify(taskRepository).deleteById(id);
  }

  @Test
  void deleteTask_throwsEntityNotFoundException_whenNotFound() {
    UUID id = UUID.randomUUID();
    when(taskRepository.existsById(id)).thenReturn(false);

    assertThatThrownBy(() -> taskService.deleteTask(id))
      .isInstanceOf(EntityNotFoundException.class)
      .hasMessageContaining(id.toString());
  }
}
