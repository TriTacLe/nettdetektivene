package ntnu.no.backend.model.classroom;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ntnu.no.backend.model.Classroom;
import ntnu.no.backend.model.auth.User;
import ntnu.no.backend.model.enums.NameChangeStatus;

import java.time.LocalDateTime;

/**
 * Represents a student's membership in a {@link Classroom}.
 * Uses a composite primary key {@link ClassroomStudentId} consisting of
 * {@code classroom_id} and {@code user_id}.
 *
 * @author norakirknes
 * @version 0.1
 * @since 0.1
 * @see ClassroomStudentId
 * @see Classroom
 * @see User
 * @see NameChangeStatus
 */
@Entity
@Table(
  name = "classroom_student",
  uniqueConstraints = @UniqueConstraint(
    name = "uq_classroom_student_one_classroom",
    columnNames = "user_id"
  )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassroomStudent {

  /**
   * Composite primary key consisting of {@code classroom_id} and {@code user_id}.
   */
  @EmbeddedId
  private ClassroomStudentId id;

  /**
   * The classroom the student belongs to.
   * Maps the {@code classroomId} field in {@link ClassroomStudentId}.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("classroomId")
  @JoinColumn(name = "classroom_id")
  private Classroom classroom;

  /**
   * The student user.
   * Maps the {@code userId} field in {@link ClassroomStudentId}.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("userId")
  @JoinColumn(name = "user_id")
  private User user;

  /**
   * The student's display name within this classroom.
   * Initially set from the student's User Profile.
   */
  @Column(name = "display_name", nullable = false)
  private String displayName;

  /**
   * Whether the student has been approved by an instructor to participate.
   * Defaults to {@code false}; the instructor must explicitly allow the student.
   */
  @Builder.Default
  @Column(name = "is_allowed", nullable = false)
  private boolean isAllowed = false;

  /**
   * The display name the student has requested to change to.
   * {@code null} if no change has been requested.
   */
  @Column(name = "requested_display_name")
  private String requestedDisplayName;

  /**
   * The current status of the student's display name change request.
   * Defaults to {@code NONE}.
   */
  @Builder.Default
  @Enumerated(EnumType.STRING)
  @Column(name = "name_change_status", nullable = false)
  private NameChangeStatus nameChangeStatus = NameChangeStatus.NONE;

  /**
   * Timestamp of when the student submitted the name change request.
   * {@code null} if no request has been made.
   */
  @Column(name = "name_change_requested_at")
  private LocalDateTime nameChangeRequestedAt;

  /**
   * The instructor who reviewed the name change request.
   * {@code null} if no review has taken place.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "name_change_reviewed_by")
  private User nameChangeReviewedBy;

  /**
   * Timestamp of when the name change request was reviewed.
   * {@code null} if no review has taken place.
   */
  @Column(name = "name_change_reviewed_at")
  private LocalDateTime nameChangeReviewedAt;

  /**
   * Timestamp of when the student joined the classroom.
   * Set once on persist, never updated.
   */
  @Column(name = "joined_at", nullable = false, updatable = false)
  private LocalDateTime joinedAt;

  /**
   * Sets {@code joinedAt} before the entity is first persisted.
   */
  @PrePersist
  protected void onCreate() {
    joinedAt = LocalDateTime.now();
  }
}
