package ntnu.no.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ntnu.no.backend.model.auth.User;

import java.time.LocalDateTime;

/**
 * Read-only denormalized view of a student's best score on a task within a classroom.
 * Uses a composite primary key {@link TaskBestScoreId} of {@code user_id},
 * {@code task_id}, and {@code classroom_id}.
 *
 * <p><strong>Never call {@code save()} on this entity.</strong>
 * It is updated exclusively by the {@code trg_attempt_sync} trigger
 * after each {@code task_attempt} insert.</p>
 *
 * @author tri
 * @version 0.1
 * @since 0.1
 * @see TaskBestScoreId
 * @see TaskAttempt
 */
@Entity
@Table(name = "task_best_score")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class TaskBestScore {

  /**
   * Composite primary key consisting of {@code user_id}, {@code task_id}, and {@code classroom_id}.
   */
  @EmbeddedId
  TaskBestScoreId id;

  /**
   * The student this score belongs to.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("userId")
  @JoinColumn(name = "user_id")
  private User user;

  /**
   * The task this score is for.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("taskId")
  @JoinColumn(name = "task_id")
  private Task task;

  /**
   * The classroom context for this score.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("classroomId")
  @JoinColumn(name = "classroom_id")
  private Classroom classroom;

  /**
   * The highest score the student has achieved on this task. Defaults to {@code 0}.
   */
  @Column(name = "best_score", nullable = false)
  private int bestScore = 0;

  /**
   * Total number of attempts made by the student on this task. Defaults to {@code 0}.
   */
  @Column(name = "total_attempts", nullable = false)
  private int totalAttempts = 0;

  /**
   * Timestamp of when the student first passed this task. {@code null} if never passed.
   */
  @Column(name = "first_passed_at")
  private LocalDateTime firstPassedAt;

  /**
   * Timestamp of the most recent attempt. Updated by the trigger on every attempt.
   */
  @Column(name = "last_attempted_at", nullable = false)
  private LocalDateTime lastAttemptedAt;
}
