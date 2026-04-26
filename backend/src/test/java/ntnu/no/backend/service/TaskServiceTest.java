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
import org.springframework.mock.web.MockMultipartFile;

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

  @Mock
  private TaskImageStorageService taskImageStorageService;

  @InjectMocks
  private TaskService taskService;

  @Test
  void getAllTask_returnsMappedDTOs() {
    when(taskRepository.findAllActive()).thenReturn(List.of(TaskTestFactory.task()));

    List<TaskResponseDTO> result = taskService.getAllTask();

    assertThat(result).hasSize(1);
    assertThat(result.get(0).title()).isEqualTo("Spot the phishing email");
  }

  @Test
  void getTask_returnsDTO_whenFound() {
    UUID id = UUID.randomUUID();
    when(taskRepository.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.of(TaskTestFactory.task()));

    TaskResponseDTO result = taskService.getTask(id);

    assertThat(result.title()).isEqualTo("Spot the phishing email");
  }

  @Test
  void getTask_throwsEntityNotFoundException_whenNotFound() {
    UUID id = UUID.randomUUID();
    when(taskRepository.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> taskService.getTask(id))
      .isInstanceOf(EntityNotFoundException.class)
      .hasMessageContaining("No active task found with id: " + id);
  }

  @Test
  void createTask_savesAndReturnsDTO() {
    when(taskRepository.save(any(Task.class))).thenAnswer(i -> i.getArgument(0));

    TaskResponseDTO result = taskService.createTask(TaskTestFactory.requestDTO());

    assertThat(result.title()).isEqualTo("Spot the phishing email");
    verify(taskRepository).save(any(Task.class));
  }

  @Test
  void createTask_withImages_resolvesContentBeforeSaving() {
    MockMultipartFile image = new MockMultipartFile(
      "images",
      "real-article-1.png",
      "image/png",
      "png".getBytes()
    );
    when(taskImageStorageService.replaceUploadReferences(any(), any()))
      .thenReturn("{\"articles\":[{\"imagePath\":\"/uploads/tasks/batch/image.png\"}]}");
    when(taskRepository.save(any(Task.class))).thenAnswer(i -> i.getArgument(0));

    TaskResponseDTO result = taskService.createTask(TaskTestFactory.requestDTOWithUploadReference(), List.of(image));

    assertThat(result.content()).contains("/uploads/tasks/batch/image.png");
    verify(taskImageStorageService).replaceUploadReferences(any(), any());
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

    when(taskRepository.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.of(TaskTestFactory.task()));

    TaskResponseDTO result = taskService.updateTask(dto, id);

    assertThat(result.title()).isEqualTo("New title");
  }

  @Test
  void updateTask_withImages_resolvesContentBeforeUpdating() {
    UUID id = UUID.randomUUID();
    MockMultipartFile image = new MockMultipartFile(
      "images",
      "real-article-1.png",
      "image/png",
      "png".getBytes()
    );
    when(taskRepository.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.of(TaskTestFactory.task()));
    when(taskImageStorageService.replaceUploadReferences(any(), any()))
      .thenReturn("{\"articles\":[{\"imagePath\":\"/uploads/tasks/batch/image.png\"}]}");

    TaskResponseDTO result = taskService.updateTask(TaskTestFactory.requestDTOWithUploadReference(), id, List.of(image));

    assertThat(result.content()).contains("/uploads/tasks/batch/image.png");
    verify(taskImageStorageService).replaceUploadReferences(any(), any());
  }

  @Test
  void deleteTask_softDeletesWhenFound() {
    UUID id = UUID.randomUUID();
    Task task = TaskTestFactory.task();
    when(taskRepository.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.of(task));
    when(taskRepository.save(any(Task.class))).thenAnswer(i -> i.getArgument(0));

    taskService.deleteTask(id);

    assertThat(task.getDeletedAt()).isNotNull();
    verify(taskRepository).save(task);
  }

  @Test
  void deleteTask_throwsEntityNotFoundException_whenNotFound() {
    UUID id = UUID.randomUUID();
    when(taskRepository.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> taskService.deleteTask(id))
      .isInstanceOf(EntityNotFoundException.class)
      .hasMessageContaining("No active task found with id: " + id);
  }
}
