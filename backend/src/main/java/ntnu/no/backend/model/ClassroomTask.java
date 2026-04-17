package ntnu.no.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents the assignment of a {@link Task} to a {@link Classroom}.
 * Uses a composite primary key {@link ClassroomTaskId} consisting of
 * {@code classroom_id} and {@code task_id}.
 *
 * @author tri
 * @version 0.1
 * @since 0.1
 * @see ClassroomTaskId
 */
@Entity
@Table(name = "classroom_task")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ClassroomTask {

  /**
   * Composite primary key consisting of {@code classroom_id} and {@code task_id}.
   */
  @EmbeddedId
  private ClassroomTaskId id;

  /**
   * The classroom this task is assigned to.
   * Maps the {@code classroomId} field in {@link ClassroomTaskId}.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("classroomId")
  private Classroom classroom;

  /**
   * The task assigned to the classroom.
   * Maps the {@code taskId} field in {@link ClassroomTaskId}.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("taskId")
  private Task task;

  /**
   * The display order of this task within the classroom.
   * Lower values appear first. Defaults to {@code 0}.
   */
  @Column(name = "sort_order", nullable = false)
  private int sortOrder = 0;
}
