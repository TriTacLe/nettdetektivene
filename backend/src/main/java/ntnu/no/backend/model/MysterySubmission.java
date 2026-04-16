package ntnu.no.backend.model;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import ntnu.no.backend.model.enums.MysteryStatus;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Represents a mystery submission made by a student within a classroom.
 * Status transitions (PENDING → APPROVED/REJECTED) trigger the
 * {@code trg_mystery_approved} database trigger which creates a notification
 * and awards an achievement — do NOT replicate this logic in Java.
 *
 * @author tri
 * @version 0.1
 * @since 0.1
 */
@Entity
@Table(name="mystery_submission")
@AttributeOverride(name = "id", column = @Column(name = "submission_id"))
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class MysterySubmission extends BaseEntity{

  /**
   * The classroom this submission belongs to.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "classroom_id", nullable = false)
  private Classroom classroom;

  /**
   * The student who submitted this mystery.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "submitted_by", nullable = false)
  private User userSubmittedBy;

  /**
   * The description of the mystery submitted by the student.
   */
  @Column(name = "mys_desc", columnDefinition = "TEXT", nullable = false)
  private String mysDesc;

  /**
   * Optional URL to an attachment (image, file) supporting the submission.
   */
  @Column(name = "attachment_url")
  private String attachmentUrl;

  /**
   * Current review status of the submission. Defaults to {@link MysteryStatus#PENDING}.
   */
  @Enumerated(EnumType.STRING)
  @Column(name = "mys_status", nullable = false)
  private MysteryStatus mysStatus = MysteryStatus.PENDING;

  /**
   * The instructor who reviewed this submission. {@code null} if not yet reviewed.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "reviewed_by")
  private User userReviewedBy;

  /**
   * Timestamp of when the submission was reviewed. {@code null} if not yet reviewed.
   */
  @Column(name = "reviewed_at")
  private LocalDateTime reviewedAt;

  /**
   * Whether this submission has been featured. Defaults to {@code false}.
   */
  @Column(name = "is_featured", nullable = false)
  private boolean isFeatured = false;

  /**
   * The week number within the year this mystery is associated with.
   */
  @Column(name = "week_number")
  private int weekNumber;

  /**
   * The year this mystery is associated with.
   */
  @Column(name = "week_year")
  private int weekYear;

  /**
   * Timestamp of when this submission was created. Set once on persist.
   */
  @CreationTimestamp
  @Column(name = "submitted_at", nullable = false, updatable = false)
  private LocalDateTime submittedAt;
}
