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
 * Composite primary key for {@link MysteryCompletion}.
 * Together {@code user_id} and {@code submission_id} uniquely identify
 * which student solved which mystery.
 */
@Embeddable
@EqualsAndHashCode
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MysteryCompletionId implements Serializable {

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "submission_id", nullable = false)
  private UUID submissionId;
}
