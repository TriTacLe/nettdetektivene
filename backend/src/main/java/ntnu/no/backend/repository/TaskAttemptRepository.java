package ntnu.no.backend.repository;

import ntnu.no.backend.model.TaskAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for {@link TaskAttempt} entities.
 *
 * <p>Note: inserting a {@link TaskAttempt} triggers {@code trg_attempt_sync}
 * which updates {@code task_best_score} automatically. Never write to
 * {@code task_best_score} directly.</p>
 *
 * @author aninesst
 * @version 0.1
 * @since 0.1
 * @see TaskAttempt
 */
@Repository
public interface TaskAttemptRepository extends JpaRepository<TaskAttempt, UUID> {

  /**
   * Finds all attempts by a specific user for a specific task.
   *
   * @param userId the user's unique identifier
   * @param taskId the task's unique identifier
   * @return list of attempts ordered by Spring Data defaults
   */
  List<TaskAttempt> findByUserIdAndTaskId(UUID userId, UUID taskId);

  /**
   * Finds all attempts within a specific classroom.
   *
   * @param classroomId the classroom's unique identifier
   * @return list of attempts in the classroom
   */
  List<TaskAttempt> findByClassroomId(UUID classroomId);
}