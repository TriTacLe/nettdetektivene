package ntnu.no.backend.model;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import ntnu.no.backend.model.auth.User;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Represents a single attempt by a student on a task within a classroom.
 * After insert, the {@code trg_attempt_sync} trigger automatically upserts
 * {@code task_best_score}, awards XP on first pass, and updates {@code xp_level}
 *
 * do NOT replicate this logic in Java.
 *
 * @author tri
 * @version 0.1
 * @since 0.1
 */
@Entity
@Table(name = "task_attempt")
@AttributeOverride(name = "id", column = @Column(name = "attempt_id"))
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@SuperBuilder
public class TaskAttempt extends BaseEntity{

  /**
   * The student who made this attempt.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  /**
   * The task that was attempted.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "task_id", nullable = false)
  private Task task;

  /**
   * The classroom context in which the attempt was made.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "classroom_id", nullable = false)
  private Classroom classroom;

  /**
   * The score achieved on this attempt. Defaults to {@code 0}.
   */
  @Column(nullable = false)
  private int score = 0;

  /**
   * Whether the student passed this attempt. Defaults to {@code false}.
   */
  @Column(nullable = false)
  private boolean passed = false;

  /**
   * Timestamp of when this attempt was made. Set once on persist.
   */
  @CreationTimestamp
  @Column(name="attempted_at", nullable = false, updatable = false)
  private LocalDateTime attemptedAt;
}
