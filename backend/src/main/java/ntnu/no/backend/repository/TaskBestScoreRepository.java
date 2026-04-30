package ntnu.no.backend.repository;

import ntnu.no.backend.model.TaskBestScore;
import ntnu.no.backend.model.TaskBestScoreId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Read-only repository for {@link TaskBestScore} entities.
 *
 * <p><strong>Never call {@code save()} through this repository.</strong>
 * {@code task_best_score} is maintained exclusively by the {@code trg_attempt_sync}
 * database trigger.</p>
 *
 * @author tri, aninesst
 * @version 0.2
 * @since 0.1
 * @see TaskBestScore
 * @see TaskBestScoreId
 */

@Repository
public interface TaskBestScoreRepository extends JpaRepository<TaskBestScore, TaskBestScoreId> {
  List<TaskBestScore> findByClassroomId(UUID id);

  long countByClassroomIdAndUserIdAndTaskStopLocationAndFirstPassedAtIsNotNull(
      UUID classroomId,
      UUID userId,
      String stopLocation
  );

  boolean existsByUserIdAndTaskIdAndClassroomIdAndFirstPassedAtIsNotNull(
      UUID userId,
      UUID taskId,
      UUID classroomId
  );

  @Query("SELECT bs.task.id FROM TaskBestScore bs " +
      "WHERE bs.user.id = :userId AND bs.firstPassedAt IS NOT NULL")
  List<UUID> findPassedTaskIdsByUserId(@Param("userId") UUID userId);
}