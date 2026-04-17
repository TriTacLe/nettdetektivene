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
 * Composite primary key for {@link UserAchievement}.
 * Consists of {@code user_id} and {@code achievement_id}, which together
 * uniquely identify an achievement earned by a user.
 *
 * <p>Implements {@link Serializable} as required by JPA for embeddable key classes.
 * {@link EqualsAndHashCode} is required so JPA can correctly compare and cache keys.</p>
 *
 * @author tri
 * @version 0.1
 * @since 0.1
 * @see UserAchievement
 */
@Embeddable
@EqualsAndHashCode
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserAchievementId implements Serializable {

  /**
   * The ID of the user.
   */
  @Column(name = "user_id", nullable = false)
  private UUID userId;

  /**
   * The ID of the achievement.
   */
  @Column(name="achievement_id", nullable = false)
  private UUID achievementId;
}
