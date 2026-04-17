package ntnu.no.backend.model.auth;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import ntnu.no.backend.model.BaseEntity;

import java.time.LocalDateTime;

/**
 * Represents the public-facing profile of a user, including display name,
 * avatar configuration, and experience points.
 *
 * @author norakirknes
 * @version 0.1
 * @since 0.1
 * @see User
 */
@Entity
@Table(name = "user_profile")
@AttributeOverride(name = "id", column = @Column(name = "profile_id"))
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class UserProfile extends BaseEntity {

  /**
   * The name displayed to other users in the application.
   */
  @Column(name = "display_name", nullable = false)
  private String displayName;

  /**
   * The user this profile belongs to.
   */
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false, updatable = false, unique = true)
  private User user;

  /**
   * The FEIDE identifier for this user, {@code null} for non-FEIDE users.
   */
  @Column(name = "feide_id", unique = true)
  private String feideId;

  /**
   * JSON blob for the user's avatar configuration,
   * {@code null} if the user has not customised their avatar.
   */
  @Column(name = "avatar_config", columnDefinition = "JSON")
  private String avatarConfig;

  /**
   * Total experience points earned by the user.
   */
  @Builder.Default
  @Column(name = "xp", nullable = false)
  private int xp = 0;

  /**
   * The user's current XP level
   */
  @Builder.Default
  @Column(name = "xp_level", nullable = false)
  private int xpLevel = 1;

  /**
   * Timestamp of when this profile was created, set once on persist
   */
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  /**
   * Timestamp of the last update to this profile.
   */
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  /**
   * Sets {@code createdAt} and {@code updatedAt} before the entity is first persisted.
   */
  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
  }

  /**
   * Refreshes {@code updatedAt} before every update.
   */
  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
