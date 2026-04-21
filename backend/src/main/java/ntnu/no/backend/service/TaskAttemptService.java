package ntnu.no.backend.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import ntnu.no.backend.dto.task.TaskAttemptRequestDTO;
import ntnu.no.backend.dto.task.TaskAttemptResponseDTO;
import ntnu.no.backend.model.TaskAttempt;
import ntnu.no.backend.model.auth.User;
import ntnu.no.backend.repository.TaskAttemptRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service for task attempt management.
 *
 * <p><strong>Critical:</strong> all attempt submissions go through {@code sp_submit_attempt}
 * via {@link JdbcTemplate}. The database trigger {@code trg_attempt_sync} automatically
 * upserts {@code task_best_score}, awards XP on first pass, and updates {@code xp_level}.
 * Never call {@code .save()} directly or replicate trigger logic in Java.</p>
 *
 * @author aninesst
 * @version 0.1
 * @since 0.1
 * @see TaskAttemptRepository
 */
@Service
@RequiredArgsConstructor
public class TaskAttemptService {

  private final TaskAttemptRepository taskAttemptRepository;
  private final JdbcTemplate jdbcTemplate;

  /**
   * Submits a task attempt by calling the {@code sp_submit_attempt} stored procedure.
   *
   * <p>The stored procedure validates that the classroom has game enabled,
   * inserts the attempt row, and the {@code trg_attempt_sync} trigger then
   * updates best scores and awards XP automatically.</p>
   *
   * @param user the authenticated user submitting the attempt
   * @param dto  the attempt request containing taskId, classroomId, and score
   * @return a {@link TaskAttemptResponseDTO} representing the recorded attempt
   */
  @Transactional
  public TaskAttemptResponseDTO submitAttempt(User user, TaskAttemptRequestDTO dto) {
    UUID attemptId = UUID.randomUUID();
    String userId = user.getId().toString();
    String taskId = dto.getTaskId().toString();
    String classroomId = dto.getClassroomId().toString();
    int score = dto.getScore();
    boolean passed = score >= 80;

    jdbcTemplate.update("CALL sp_submit_attempt(?, ?, ?, ?, ?, ?)",
        attemptId.toString(), userId, taskId, classroomId, score, passed ? 1 : 0);

    TaskAttempt attempt = taskAttemptRepository.findById(attemptId)
        .orElseThrow(() -> new EntityNotFoundException("Attempt not found after insert: " + attemptId));

    return toDTO(attempt);
  }

  /**
   * Retrieves all attempts for a given user and task combination.
   *
   * @param userId the user's unique identifier
   * @param taskId the task's unique identifier
   * @return a list of {@link TaskAttemptResponseDTO} for the user/task pair
   */
  @Transactional(readOnly = true)
  public List<TaskAttemptResponseDTO> getAttemptsByUserAndTask(UUID userId, UUID taskId) {
    return taskAttemptRepository.findByUserIdAndTaskId(userId, taskId)
        .stream()
        .map(this::toDTO)
        .toList();
  }

  /**
   * Retrieves all attempts within a given classroom.
   *
   * @param classroomId the classroom's unique identifier
   * @return a list of {@link TaskAttemptResponseDTO} for the classroom
   */
  @Transactional(readOnly = true)
  public List<TaskAttemptResponseDTO> getAttemptsByClassroom(UUID classroomId) {
    return taskAttemptRepository.findByClassroomId(classroomId)
        .stream()
        .map(this::toDTO)
        .toList();
  }

  private TaskAttemptResponseDTO toDTO(TaskAttempt attempt) {
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