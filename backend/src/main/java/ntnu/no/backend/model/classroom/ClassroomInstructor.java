package ntnu.no.backend.model.classroom;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ntnu.no.backend.model.Classroom;
import ntnu.no.backend.model.auth.User;

import java.time.LocalDateTime;

/**
 * Represents an instructors assignment to a {@link Classroom}.
 * Uses a composite primary key {@link ClassroomInstructorId} consisting of
 * {@code classroom_id} and {@code user_id}.
 *
 * @author norakirknes
 * @version 0.1
 * @since 0.1
 * @see ClassroomInstructorId
 * @see Classroom
 * @see User
 */
@Entity
@Table(name = "classroom_instructor")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassroomInstructor {

  /**
   * Composite primary key consisting of {@code classroom_id} and {@code user_id}.
   */
  @EmbeddedId
  private ClassroomInstructorId id;

  /**
   * The classroom the instructor is assigned to.
   * Maps the {@code classroomId} field in {@link ClassroomInstructorId}.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("classroomId")
  @JoinColumn(name = "classroom_id")
  private Classroom classroom;

  /**
   * The instructor user.
   * Maps the {@code userId} field in {@link ClassroomInstructorId}.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("userId")
  @JoinColumn(name = "user_id")
  private User user;

  /**
   * Timestamp of when the instructor was assigned to the classroom.
   * Set once on persist, never updated.
   */
  @Column(name = "assigned_at", nullable = false, updatable = false)
  private LocalDateTime assignedAt;

  /**
   * Sets {@code assignedAt} before the entity is first persisted.
   */
  @PrePersist
  protected void onCreate() {
    assignedAt = LocalDateTime.now();
  }
}
