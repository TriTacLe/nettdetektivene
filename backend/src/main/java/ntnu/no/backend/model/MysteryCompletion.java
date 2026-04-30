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
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Records that a user has solved a specific {@link MysterySubmission}
 * by selecting the correct alternative.
 *
 * <p>Insertion is idempotent (PRIMARY KEY on user_id+submission_id) so
 * subsequent correct picks do not create duplicate rows. The
 * {@code trg_mystery_completion} database trigger awards the
 * "mystery_solved" achievement at thresholds 1 and 5.</p>
 */
@Entity
@Table(name = "mystery_completion")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MysteryCompletion {

  @EmbeddedId
  private MysteryCompletionId id;

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("userId")
  @JoinColumn(name = "user_id")
  private User user;

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("submissionId")
  @JoinColumn(name = "submission_id")
  private MysterySubmission submission;

  @CreationTimestamp
  @Column(name = "completed_at", nullable = false, updatable = false)
  private LocalDateTime completedAt;
}
