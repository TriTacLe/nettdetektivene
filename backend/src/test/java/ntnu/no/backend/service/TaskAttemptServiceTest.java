package ntnu.no.backend.service;

import jakarta.persistence.EntityNotFoundException;
import ntnu.no.backend.dto.task.TaskAttemptResponseDTO;
import ntnu.no.backend.model.Classroom;
import ntnu.no.backend.model.Task;
import ntnu.no.backend.model.TaskAttempt;
import ntnu.no.backend.model.auth.User;
import ntnu.no.backend.repository.TaskAttemptRepository;
import ntnu.no.backend.util.TaskAttemptTestFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link TaskAttemptService}.
 *
 * @author tri
 * @version 0.1
 * @since 0.1
 */
@ExtendWith(MockitoExtension.class)
class TaskAttemptServiceTest {

  @Mock
  private TaskAttemptRepository taskAttemptRepository;

  @Mock
  private JdbcTemplate jdbcTemplate;

  @InjectMocks
  private TaskAttemptService taskAttemptService;

  @Test
  void submitAttempt_returnsDTO_afterStoredProcedureCall() {
    User user = TaskAttemptTestFactory.user();
    Task task = TaskAttemptTestFactory.task();
    Classroom classroom = TaskAttemptTestFactory.classroom();
    UUID taskId = UUID.randomUUID();
    UUID classroomId = UUID.randomUUID();
    TaskAttempt attempt = TaskAttemptTestFactory.attempt(user, task, classroom);

    when(jdbcTemplate.update(anyString(), any(), any(), any(), any(), any(), any())).thenReturn(1);
    when(taskAttemptRepository.findById(any(UUID.class))).thenReturn(Optional.of(attempt));

    TaskAttemptResponseDTO result = taskAttemptService.submitAttempt(
        user, TaskAttemptTestFactory.requestDTO(taskId, classroomId));

    assertThat(result.score()).isEqualTo(85);
    assertThat(result.passed()).isTrue();
  }

  @Test
  void submitAttempt_throwsEntityNotFoundException_whenAttemptNotFoundAfterInsert() {
    User user = TaskAttemptTestFactory.user();
    UUID taskId = UUID.randomUUID();
    UUID classroomId = UUID.randomUUID();

    when(jdbcTemplate.update(anyString(), any(), any(), any(), any(), any(), any())).thenReturn(1);
    when(taskAttemptRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

    assertThatThrownBy(() -> taskAttemptService.submitAttempt(
        user, TaskAttemptTestFactory.requestDTO(taskId, classroomId)))
        .isInstanceOf(EntityNotFoundException.class);
  }

  @Test
  void getAttemptsByUserAndTask_returnsMappedDTOs() {
    UUID userId = UUID.randomUUID();
    UUID taskId = UUID.randomUUID();
    User user = TaskAttemptTestFactory.user();
    TaskAttempt attempt = TaskAttemptTestFactory.attempt(user, TaskAttemptTestFactory.task(), TaskAttemptTestFactory.classroom());

    when(taskAttemptRepository.findByUserIdAndTaskId(userId, taskId)).thenReturn(List.of(attempt));

    List<TaskAttemptResponseDTO> result = taskAttemptService.getAttemptsByUserAndTask(userId, taskId);

    assertThat(result).hasSize(1);
    assertThat(result.getFirst().score()).isEqualTo(85);
  }

  @Test
  void getAttemptsByClassroom_returnsMappedDTOs() {
    UUID classroomId = UUID.randomUUID();
    User user = TaskAttemptTestFactory.user();
    TaskAttempt attempt = TaskAttemptTestFactory.attempt(user, TaskAttemptTestFactory.task(), TaskAttemptTestFactory.classroom());

    when(taskAttemptRepository.findByClassroomId(classroomId)).thenReturn(List.of(attempt));

    List<TaskAttemptResponseDTO> result = taskAttemptService.getAttemptsByClassroom(classroomId);

    assertThat(result).hasSize(1);
    assertThat(result.getFirst().passed()).isTrue();
  }
}
