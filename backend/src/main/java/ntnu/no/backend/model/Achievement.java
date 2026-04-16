package ntnu.no.backend.model;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import jakarta.persistence.PrePersist;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * Represents an achievement that can be earned by users (STUDENT)
 * upon completing specific actions in the application.
 *
 * @author Tri
 * @version 0.1
 * @since 0.1
 */
@Entity
@Table(name = "achievement")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
@AttributeOverride(name = "id", column = @Column(name="achievement_id"))
public class Achievement extends BaseEntity {

  /**
   * The title of the achievement.
   */
  @Column(name = "title", nullable = false)
  private String title;

  /**
   * A description of what the achievement represents or how to earn it.
   */
  @Column(name = "desc_ach", columnDefinition = "TEXT")
  private String descAch;

  /**
   * Icon identifier for the achievement's visual representation.
   */
  @Column(name = "icon", columnDefinition = "TEXT")
  private String icon;

  /**
   * The type of event that triggers this achievement, for example {@code ukens_mysterium}.
   */
  @Column(name = "trigger_type", length = 50)
  private String triggerType;

  /**
   * An optional reference value used alongside {@code triggerType} to
   * narrow which specific event awards this achievement.
   */
  @Column(name = "trigger_ref", length = 100)
  private String triggerRef;

  /**
   * Timestamp of when this achievement was created. Set once on persist.
   */
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  /**
   * Sets {@code createdAt} before the entity is first persisted.
   */
  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
  }
}
