package ntnu.no.backend.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import ntnu.no.backend.dto.task.TaskAttemptRequestDTO;
import ntnu.no.backend.dto.task.TaskAttemptResponseDTO;
import ntnu.no.backend.model.TaskAttempt;
import ntnu.no.backend.model.Classroom;
import ntnu.no.backend.model.auth.User;
import ntnu.no.backend.model.classroom.ClassroomStudent;
import ntnu.no.backend.model.classroom.ClassroomStudentId;
import ntnu.no.backend.repository.TaskAttemptRepository;
import ntnu.no.backend.repository.TaskBestScoreRepository;
import ntnu.no.backend.repository.TaskRepository;
import ntnu.no.backend.repository.ClassroomRepository;
import ntnu.no.backend.repository.ClassroomStudentRepository;
import ntnu.no.backend.repository.ClassroomInstructorRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
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
  private final TaskBestScoreRepository taskBestScoreRepository;
  private final TaskRepository taskRepository;
  private final JdbcTemplate jdbcTemplate;
  private final ClassroomRepository classroomRepository;
  private final ClassroomStudentRepository classroomStudentRepository;
  private final ClassroomInstructorRepository classroomInstructorRepository;
  private final TaskAttemptVerifier taskAttemptVerifier;

  /**
   * Submits a task attempt by calling the {@code sp_submit_attempt} stored procedure.
   *
   * <p>The stored procedure validates that the classroom has game enabled,
   * inserts the attempt row, and the {@code trg_attempt_sync} trigger then
   * updates best scores and awards XP automatically.</p>
   *
   * @param user the authenticated user submitting the attempt
   * @param dto  the attempt request containing taskId, classroomId, and raw answers
   * @return a {@link TaskAttemptResponseDTO} representing the recorded attempt
   */
  @Transactional
  public TaskAttemptResponseDTO submitAttempt(User user, TaskAttemptRequestDTO dto) {
    if (!taskRepository.existsByIdAndDeletedAtIsNull(dto.getTaskId())) {
      throw new EntityNotFoundException("Task is no longer available: " + dto.getTaskId());
    }

    UUID attemptId = UUID.randomUUID();
    String userId = user.getId().toString();
    String taskId = dto.getTaskId().toString();
    String classroomId = dto.getClassroomId().toString();
    boolean passed = taskAttemptVerifier.isCorrect(dto.getTaskId(), dto.getAnswers());
    int score = passed ? 100 : 0;

    jdbcTemplate.update("CALL sp_submit_attempt(?, ?, ?, ?, ?, ?)",
        attemptId.toString(), userId, taskId, classroomId, score, passed ? 1 : 0);

    TaskAttempt attempt = taskAttemptRepository.findById(attemptId)
        .orElseThrow(() -> new EntityNotFoundException("Attempt not found after insert: " + attemptId));

    return toDTO(attempt);
  }

  /**
   * Retrieves all attempts for a given user and task combination.
   * Verifies that the requesting user is authorized to view the attempts.
   *
   * @param userId the user's unique identifier
   * @param taskId the task's unique identifier
   * @param requestingUser the authenticated user making the request
   * @return a list of {@link TaskAttemptResponseDTO} for the user/task pair
   * @throws AccessDeniedException if the requesting user is not authorized
   */
  @Transactional(readOnly = true)
  public List<TaskAttemptResponseDTO> getAttemptsByUserAndTask(UUID userId, UUID taskId, User requestingUser) {
    verifyAttemptAccess(userId, requestingUser);
    return taskAttemptRepository.findByUserIdAndTaskId(userId, taskId)
        .stream()
        .map(this::toDTO)
        .toList();
  }

  /**
   * Retrieves all attempts within a given classroom.
   * Verifies that the requesting user is an instructor of the classroom.
   *
   * @param classroomId the classroom's unique identifier
   * @param requestingUser the authenticated instructor
   * @return a list of {@link TaskAttemptResponseDTO} for the classroom
   * @throws AccessDeniedException if the requesting user is not an instructor of the classroom
   */
  /**
   * Returns the IDs of every task the authenticated user has passed at
   * least once (any classroom). Source of truth: {@code task_best_score}
   * rows where {@code first_passed_at IS NOT NULL}, maintained by the
   * {@code trg_attempt_sync} trigger.
   *
   * @param user the authenticated user
   * @return list of passed task IDs
   */
  @Transactional(readOnly = true)
  public List<UUID> getMyPassedTaskIds(User user) {
    return taskBestScoreRepository.findPassedTaskIdsByUserId(user.getId());
  }

  @Transactional(readOnly = true)
  public List<TaskAttemptResponseDTO> getAttemptsByClassroom(UUID classroomId, User requestingUser) {
    verifyInstructor(classroomId, requestingUser);
    return taskAttemptRepository.findByClassroomId(classroomId)
        .stream()
        .map(this::toDTO)
        .toList();
  }

  /**
   * Verifies that the requesting user is authorized to view attempts for a specific user.
   * Students can only see their own attempts. Instructors can only see attempts of students
   * in their classroom.
   *
   * @param targetUserId the user whose attempts are being requested
   * @param requestingUser the authenticated user making the request
   * @throws AccessDeniedException if the requesting user is not authorized
   */
  private void verifyAttemptAccess(UUID targetUserId, User requestingUser) {
    boolean isInstructor = requestingUser.getAuthorities().stream()
        .anyMatch(a -> "instructor".equals(a.getAuthority()));

    if (isInstructor) {
      verifyInstructorByStudent(classroomRepository, classroomStudentRepository, targetUserId, requestingUser);
    } else {
      if (!targetUserId.equals(requestingUser.getId())) {
        throw new AccessDeniedException("You can only view your own attempts");
      }
    }
  }

  /**
   * Verifies that an instructor is allowed to view a student's attempts.
   * The instructor must be in a classroom that the student belongs to.
   *
   * @param classroomRepository the classroom repository
   * @param classroomStudentRepository the classroom student repository
   * @param targetUserId the student's user ID
   * @param requestingInstructor the authenticated instructor
   * @throws AccessDeniedException if the instructor and student are not in the same classroom
   */
  private void verifyInstructorByStudent(ClassroomRepository classroomRepository,
                                          ClassroomStudentRepository classroomStudentRepository,
                                          UUID targetUserId,
                                          User requestingInstructor) {
    ClassroomStudent membership = classroomStudentRepository.findByUser(
      requestingInstructor
    ).orElseThrow(() -> new AccessDeniedException("You must be an instructor with a classroom"));

    Classroom instructorClassroom = classroomRepository.findByIdAndDeletedAtIsNull(membership.getClassroom().getId())
        .orElseThrow(() -> new AccessDeniedException("Classroom not found"));

    boolean studentInClass = classroomStudentRepository.existsById(
      ClassroomStudentId.builder()
        .classroomId(instructorClassroom.getId())
        .userId(targetUserId)
        .build()
    );

    if (!studentInClass) {
      throw new AccessDeniedException("You can only view attempts of students in your classroom");
    }
  }

  /**
   * Verifies that the user is an instructor of the given classroom.
   *
   * @param classroomId the classroom ID
   * @param user the user to verify
   * @throws AccessDeniedException if the user is not an instructor of the classroom
   */
  private void verifyInstructor(UUID classroomId, User user) {
    boolean isInstructor = user.getAuthorities().stream()
        .anyMatch(a -> "instructor".equals(a.getAuthority()));

    if (!isInstructor) {
      throw new AccessDeniedException("Only instructors can view classroom attempts");
    }

    Classroom classroom = classroomRepository.findByIdAndDeletedAtIsNull(classroomId)
        .orElseThrow(() -> new AccessDeniedException("Classroom not found"));

    boolean isInstructorOfClassroom = classroomInstructorRepository.existsByClassroomAndUser(
        classroom, user
    );

    if (!isInstructorOfClassroom) {
      throw new AccessDeniedException("You are not an instructor of this classroom");
    }
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