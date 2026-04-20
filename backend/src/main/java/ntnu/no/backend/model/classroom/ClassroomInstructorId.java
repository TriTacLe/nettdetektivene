package ntnu.no.backend.model.classroom;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

/**
 * Composite primary key for {@link ClassroomInstructor}.
 * Consists of {@code classroom_id} and {@code user_id}, which together
 * uniquely identify an instructors assignment to a classroom.
 *
 * @author norakirknes
 * @version 0.1
 * @since 0.1
 * @see ClassroomInstructor
 */
@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ClassroomInstructorId implements Serializable {

  /**
   * The ID of the classroom.
   */
  @Column(name = "classroom_id", nullable = false)
  private UUID classroomId;

  /**
   * The ID of the instructor user.
   */
  @Column(name = "user_id", nullable = false)
  private UUID userId;
}
