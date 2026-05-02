package ntnu.no.backend.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import ntnu.no.backend.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repository for {@link Task} entities.
 *
 * @author tri
 * @version 0.1
 * @since 0.1
 */
public interface TaskRepository extends JpaRepository<Task, UUID> {

  /**
   * Finds all tasks that are not soft-deleted.
   *
   * @return list of active tasks
   */
  @Query("SELECT t FROM Task t WHERE t.deletedAt IS NULL")
  List<Task> findAllActive();

  /**
   * Finds a task by ID that is not soft-deleted.
   *
   * @param id the task ID
   * @return optional containing the task if found and not deleted
   */
  @Query("SELECT t FROM Task t WHERE t.id = :id AND t.deletedAt IS NULL")
  Optional<Task> findByIdAndDeletedAtIsNull(@Param("id") UUID id);

  /**
   * Checks if a task exists and is not soft-deleted.
   *
   * @param id the task ID
   * @return true if the task exists and is not deleted
   */
  @Query("SELECT COUNT(t) > 0 FROM Task t WHERE t.id = :id AND t.deletedAt IS NULL")
  boolean existsByIdAndDeletedAtIsNull(@Param("id") UUID id);

  @Query("SELECT t FROM Task t WHERE t.title = :title AND t.deletedAt IS NULL")
  Optional<Task> findByTitleAndDeletedAtIsNull(@Param("title") String title);
}
