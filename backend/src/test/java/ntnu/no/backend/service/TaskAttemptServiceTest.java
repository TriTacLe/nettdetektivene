package ntnu.no.backend.service;

import jakarta.persistence.EntityNotFoundException;
import ntnu.no.backend.controller.TaskController;
import ntnu.no.backend.dto.task.TaskAttemptRequestDTO;
import ntnu.no.backend.dto.task.TaskAttemptResponseDTO;
import ntnu.no.backend.model.Classroom;
import ntnu.no.backend.model.Task;
import ntnu.no.backend.model.TaskAttempt;
import ntnu.no.backend.model.auth.User;
import ntnu.no.backend.repository.TaskAttemptRepository;
import ntnu.no.backend.repository.TaskBestScoreRepository;
import ntnu.no.backend.repository.ClassroomRepository;
import ntnu.no.backend.repository.ClassroomStudentRepository;
import ntnu.no.backend.repository.ClassroomInstructorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.security.access.AccessDeniedException;

/**
 * Controller tests for {@link TaskAttemptService}.
 *
 * @author aninesst
 * @version 0.2
 * @since 0.1
 */

@ExtendWith(MockitoExtension.class)
class TaskAttemptServiceTest {

  @Mock
  TaskAttemptRepository taskAttemptRepository;

  @Mock
  TaskBestScoreRepository taskBestScoreRepository;

  @Mock
  JdbcTemplate jdbcTemplate;

  @Mock
  ClassroomRepository classroomRepository;

  @Mock
  ClassroomStudentRepository classroomStudentRepository;

  @Mock
  ClassroomInstructorRepository classroomInstructorRepository;

  @InjectMocks
  private TaskAttemptService taskAttemptService;

  private UUID userId;
  private UUID taskId;
  private UUID classroomId;
  private LocalDateTime attemptedAt;

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();
    taskId = UUID.randomUUID();
    classroomId = UUID.randomUUID();
    attemptedAt = LocalDateTime.of(2026, 4, 22, 10, 30);
  }

  @Test
  void submitAttempt_callsStoredProcedure_andReturnsDto_whenPassed() {
    User user = userWithId(userId);
    TaskAttemptRequestDTO dto = new TaskAttemptRequestDTO(taskId, classroomId, 85);

    TaskAttempt persistedAttempt = taskAttempt(
        null, userId, taskId, classroomId, 85, true, attemptedAt
    );

    when(taskAttemptRepository.findById(org.mockito.ArgumentMatchers.any(UUID.class)))
        .thenReturn(Optional.of(persistedAttempt));

    TaskAttemptResponseDTO result = taskAttemptService.submitAttempt(user, dto);

    ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> attemptIdCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> userIdCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> taskIdCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> classroomIdCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<Integer> scoreCaptor = ArgumentCaptor.forClass(Integer.class);
    ArgumentCaptor<Integer> passedCaptor = ArgumentCaptor.forClass(Integer.class);

    verify(jdbcTemplate).update(
        sqlCaptor.capture(),
        attemptIdCaptor.capture(),
        userIdCaptor.capture(),
        taskIdCaptor.capture(),
        classroomIdCaptor.capture(),
        scoreCaptor.capture(),
        passedCaptor.capture()
    );

    assertThat(sqlCaptor.getValue()).isEqualTo("CALL sp_submit_attempt(?, ?, ?, ?, ?, ?)");
    assertThat(UUID.fromString(attemptIdCaptor.getValue())).isNotNull();
    assertThat(userIdCaptor.getValue()).isEqualTo(userId.toString());
    assertThat(taskIdCaptor.getValue()).isEqualTo(taskId.toString());
    assertThat(classroomIdCaptor.getValue()).isEqualTo(classroomId.toString());
    assertThat(scoreCaptor.getValue()).isEqualTo(85);
    assertThat(passedCaptor.getValue()).isEqualTo(1);

    assertThat(result.id()).isEqualTo(persistedAttempt.getId());
    assertThat(result.userId()).isEqualTo(userId);
    assertThat(result.taskId()).isEqualTo(taskId);
    assertThat(result.classroomId()).isEqualTo(classroomId);
    assertThat(result.score()).isEqualTo(85);
    assertThat(result.passed()).isTrue();
    assertThat(result.attemptedAt()).isEqualTo(attemptedAt);
  }

  @Test
  void submitAttempt_callsStoredProcedure_withPassedZero_whenScoreBelow80() {
    User user = userWithId(userId);
    TaskAttemptRequestDTO dto = new TaskAttemptRequestDTO(taskId, classroomId, 79);

    TaskAttempt persistedAttempt = taskAttempt(
        null, userId, taskId, classroomId, 79, false, attemptedAt
    );

    when(taskAttemptRepository.findById(org.mockito.ArgumentMatchers.any(UUID.class)))
        .thenReturn(Optional.of(persistedAttempt));

    TaskAttemptResponseDTO result = taskAttemptService.submitAttempt(user, dto);

    ArgumentCaptor<Integer> passedCaptor = ArgumentCaptor.forClass(Integer.class);

    verify(jdbcTemplate).update(
        eq("CALL sp_submit_attempt(?, ?, ?, ?, ?, ?)"),
        org.mockito.ArgumentMatchers.anyString(),
        eq(userId.toString()),
        eq(taskId.toString()),
        eq(classroomId.toString()),
        eq(79),
        passedCaptor.capture()
    );

    assertThat(passedCaptor.getValue()).isEqualTo(0);
    assertThat(result.passed()).isFalse();
  }

  @Test
  void submitAttempt_throwsEntityNotFoundException_whenAttemptCannotBeReadBack() {
    User user = userWithId(userId);
    TaskAttemptRequestDTO dto = new TaskAttemptRequestDTO(taskId, classroomId, 80);

    when(taskAttemptRepository.findById(org.mockito.ArgumentMatchers.any(UUID.class)))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> taskAttemptService.submitAttempt(user, dto))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessageContaining("Attempt not found after insert");

    verify(jdbcTemplate).update(
        eq("CALL sp_submit_attempt(?, ?, ?, ?, ?, ?)"),
        org.mockito.ArgumentMatchers.anyString(),
        eq(userId.toString()),
        eq(taskId.toString()),
        eq(classroomId.toString()),
        eq(80),
        eq(1)
    );
  }

  @Test
  void getAttemptsByUserAndTask_returnsMappedDtos() {
    TaskAttempt attempt1 = taskAttempt(
        UUID.randomUUID(), userId, taskId, classroomId, 65, false,
        LocalDateTime.of(2026, 4, 20, 9, 0)
    );
    TaskAttempt attempt2 = taskAttempt(
        UUID.randomUUID(), userId, taskId, classroomId, 90, true,
        LocalDateTime.of(2026, 4, 21, 14, 15)
    );

    when(taskAttemptRepository.findByUserIdAndTaskId(userId, taskId))
        .thenReturn(List.of(attempt1, attempt2));
    User requestingUser = userWithId(userId);

    List<TaskAttemptResponseDTO> result =
        taskAttemptService.getAttemptsByUserAndTask(userId, taskId, requestingUser);

    assertThat(result).hasSize(2);

    assertThat(result.get(0).id()).isEqualTo(attempt1.getId());
    assertThat(result.get(0).userId()).isEqualTo(userId);
    assertThat(result.get(0).taskId()).isEqualTo(taskId);
    assertThat(result.get(0).classroomId()).isEqualTo(classroomId);
    assertThat(result.get(0).score()).isEqualTo(65);
    assertThat(result.get(0).passed()).isFalse();

    assertThat(result.get(1).id()).isEqualTo(attempt2.getId());
    assertThat(result.get(1).score()).isEqualTo(90);
    assertThat(result.get(1).passed()).isTrue();
  }

  @Test
  void getAttemptsByClassroom_returnsMappedDtos() {
    UUID classroomId = UUID.randomUUID();
    UUID instructorId = UUID.randomUUID();
    UUID otherUserId = UUID.randomUUID();
    UUID otherTaskId = UUID.randomUUID();

    TaskAttempt attempt1 = taskAttempt(
        UUID.randomUUID(), userId, taskId, classroomId, 88, true,
        LocalDateTime.of(2026, 4, 18, 8, 0)
    );
    TaskAttempt attempt2 = taskAttempt(
        UUID.randomUUID(), otherUserId, otherTaskId, classroomId, 40, false,
        LocalDateTime.of(2026, 4, 18, 8, 30)
    );

    when(taskAttemptRepository.findByClassroomId(classroomId))
        .thenReturn(List.of(attempt1, attempt2));
    User requestingInstructor = instructorWithId(classroomId, instructorId);

    Classroom classroom = classroomEntity(classroomId);
    when(classroomRepository.findByIdAndDeletedAtIsNull(classroomId)).thenReturn(Optional.of(classroom));
    when(classroomInstructorRepository.existsByClassroomAndUser(classroom, requestingInstructor))
        .thenReturn(true);

    List<TaskAttemptResponseDTO> result =
        taskAttemptService.getAttemptsByClassroom(classroomId, requestingInstructor);

    assertThat(result).hasSize(2);

    assertThat(result.get(0).classroomId()).isEqualTo(classroomId);
    assertThat(result.get(0).userId()).isEqualTo(userId);
    assertThat(result.get(0).taskId()).isEqualTo(taskId);
    assertThat(result.get(0).score()).isEqualTo(88);
    assertThat(result.get(0).passed()).isTrue();

    assertThat(result.get(1).classroomId()).isEqualTo(classroomId);
    assertThat(result.get(1).userId()).isEqualTo(otherUserId);
    assertThat(result.get(1).taskId()).isEqualTo(otherTaskId);
    assertThat(result.get(1).score()).isEqualTo(40);
    assertThat(result.get(1).passed()).isFalse();
  }

  @Test
  void getMyPassedTaskIds_delegatesToRepository_scopedToCallingUser() {
    User user = userWithId(userId);
    UUID t1 = UUID.randomUUID();
    UUID t2 = UUID.randomUUID();
    when(taskBestScoreRepository.findPassedTaskIdsByUserId(userId))
        .thenReturn(List.of(t1, t2));

    List<UUID> result = taskAttemptService.getMyPassedTaskIds(user);

    assertThat(result).containsExactly(t1, t2);
    verify(taskBestScoreRepository).findPassedTaskIdsByUserId(userId);
  }

  private User userWithId(UUID id) {
    User user = new User();
    setIdReflectively(user, id);
    return user;
  }

  private User instructorWithId(UUID classroomId, UUID instructorId) {
    User user = new User();
    setIdReflectively(user, instructorId);

    org.springframework.security.core.authority.SimpleGrantedAuthority authority =
        new org.springframework.security.core.authority.SimpleGrantedAuthority("instructor");

    try {
      java.lang.reflect.Field rolesField = User.class.getDeclaredField("roles");
      rolesField.setAccessible(true);
      rolesField.set(user, java.util.List.of(authority));
    } catch (Exception e) {
      throw new RuntimeException("Failed to set instructor authority", e);
    }

    return user;
  }

  private Classroom classroomEntity(UUID id) {
    Classroom classroom = new Classroom();
    setIdReflectively(classroom, id);
    return classroom;
  }

  private TaskAttempt taskAttempt(
      UUID attemptId,
      UUID userId,
      UUID taskId,
      UUID classroomId,
      int score,
      boolean passed,
      LocalDateTime attemptedAt
  ) {
    User user = new User();
    setIdReflectively(user, userId);

    Task task = new Task();
    setIdReflectively(task, taskId);

    Classroom classroom = new Classroom();
    setIdReflectively(classroom, classroomId);

    TaskAttempt attempt = new TaskAttempt();
    if (attemptId != null) {
      setIdReflectively(attempt, attemptId);
    }
    attempt.setUser(user);
    attempt.setTask(task);
    attempt.setClassroom(classroom);
    attempt.setScore(score);
    attempt.setPassed(passed);
    attempt.setAttemptedAt(attemptedAt);
    return attempt;
  }

  private void setIdReflectively(Object target, UUID id) {
    Class<?> type = target.getClass();
    while (type != null) {
      try {
        var field = type.getDeclaredField("id");
        field.setAccessible(true);
        field.set(target, id);
        return;
      } catch (NoSuchFieldException ignored) {
        type = type.getSuperclass();
      } catch (IllegalAccessException e) {
        throw new RuntimeException(e);
      }
    }
    throw new IllegalArgumentException("Could not find id field on " + target.getClass());
  }
}