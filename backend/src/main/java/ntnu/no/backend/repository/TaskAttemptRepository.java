package ntnu.no.backend.repository;

import ntnu.no.backend.model.TaskAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository for {@link TaskAttempt} entities.
 *
 * <p>Note: inserting a {@link TaskAttempt} triggers {@code trg_attempt_sync}
 * which updates {@code task_best_score} automatically. Never write to
 * {@code task_best_score} directly.</p>
 *
 * @author tri
 * @version 0.1
 * @since 0.1
 * @see TaskAttempt
 */
@Repository
public interface TaskAttemptRepository extends JpaRepository<TaskAttempt, UUID> {
}
