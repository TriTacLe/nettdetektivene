package ntnu.no.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

/**
 * Composite primary key for {@link ClassroomTask}.
 * Consists of {@code classroom_id} and {@code task_id}, which together
 * uniquely identify a task assignment within a classroom.
 *
 * <p>Implements {@link Serializable} as required by JPA for embeddable key classes.
 * {@link EqualsAndHashCode} is required so JPA can correctly compare and cache keys.</p>
 *
 * @author tri
 * @version 0.1
 * @since 0.1
 * @see ClassroomTask
 */
@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ClassroomTaskId implements Serializable {

  /**
   * The ID of the classroom.
   */
  @Column(name = "classroom_id", nullable = false)
  private UUID classroomId;

  /**
   * The ID of the task.
   */
  @Column(name = "task_id", nullable = false)
  private UUID taskId;
}
