package ntnu.no.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

/**
 * Composite primary key for {@link TaskBestScore}.
 * Consists of {@code user_id}, {@code task_id}, and {@code classroom_id}.
 *
 * <p>Implements {@link Serializable} as required by JPA for embeddable key classes.
 * {@link EqualsAndHashCode} is required so JPA can correctly compare and cache keys.</p>
 *
 * @author tri
 * @version 0.1
 * @since 0.1
 * @see TaskBestScore
 */
@Embeddable
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class TaskBestScoreId implements Serializable {

  /**
   * The ID of the user.
   */
  @Column(name = "user_id", nullable = false)
  private UUID userId;

  /**
   * The ID of the task.
   */
  @Column(name = "task_id", nullable = false)
  private UUID taskId;

  /**
   * The ID of the classroom.
   */
  @Column(name = "classroom_id", nullable = false)
  private UUID classroomId;
}
