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
 *
 * <p>A submission contains an uploaded image and three text alternatives,
 * one of which is marked as the correct answer. After approval by an
 * instructor it becomes playable by other students in the classroom.</p>
 *
 * <p>Status transitions (PENDING → APPROVED/REJECTED) trigger
 * {@code trg_mystery_approved} which notifies the submitter. The
 * "mystery_solved" achievement is awarded to the SOLVER (not the submitter)
 * by {@code trg_mystery_completion}.</p>
 */
@Entity
@Table(name="mystery_submission")
@AttributeOverride(name = "id", column = @Column(name = "submission_id"))
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class MysterySubmission extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "classroom_id", nullable = false)
  private Classroom classroom;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "submitted_by", nullable = false)
  private User userSubmittedBy;

  @Column(name = "image_path", nullable = false, length = 500)
  private String imagePath;

  @Column(name = "alternative_1", columnDefinition = "TEXT", nullable = false)
  private String alternative1;

  @Column(name = "alternative_2", columnDefinition = "TEXT", nullable = false)
  private String alternative2;

  @Column(name = "alternative_3", columnDefinition = "TEXT", nullable = false)
  private String alternative3;

  /** Index of the correct alternative: 0, 1, or 2. */
  @Column(name = "correct_index", nullable = false)
  private byte correctIndex;

  @Enumerated(EnumType.STRING)
  @Column(name = "mys_status", nullable = false)
  private MysteryStatus mysStatus = MysteryStatus.PENDING;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "reviewed_by")
  private User userReviewedBy;

  @Column(name = "reviewed_at")
  private LocalDateTime reviewedAt;

  @CreationTimestamp
  @Column(name = "submitted_at", nullable = false, updatable = false)
  private LocalDateTime submittedAt;
}
