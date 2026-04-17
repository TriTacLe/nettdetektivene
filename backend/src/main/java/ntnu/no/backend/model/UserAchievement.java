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
 * Represents an achievement earned by a user, optionally within a classroom context.
 * Uses a composite primary key {@link UserAchievementId} of {@code user_id} and
 * {@code achievement_id}.
 *
 * <p>Achievements are awarded automatically by the {@code trg_mystery_approved} trigger
 * — do NOT award achievements manually in service code.</p>
 *
 * @author tri
 * @version 0.1
 * @since 0.1
 * @see UserAchievementId
 * @see Achievement
 */
@Entity
@Table(name = "user_achievement")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserAchievement {

  /**
   * Composite primary key consisting of {@code user_id} and {@code achievement_id}.
   */
  @EmbeddedId
  private UserAchievementId id;

  /**
   * The user who earned the achievement.
   * Maps the {@code userId} field in {@link UserAchievementId}.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("userId")
  private User user;

  /**
   * The achievement that was earned.
   * Maps the {@code achievementId} field in {@link UserAchievementId}.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("achievementId")
  private Achievement achievement;

  /**
   * The classroom in which the achievement was earned. May be {@code null}
   * for achievements earned outside a classroom context.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "classroom_id")
  private Classroom classroom;

  /**
   * Timestamp of when the achievement was earned. Set once on persist.
   */
  @CreationTimestamp
  @Column(name = "earned_at", nullable = false, updatable = false)
  private LocalDateTime earnedAt;
}
