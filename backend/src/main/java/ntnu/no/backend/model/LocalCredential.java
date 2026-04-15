package ntnu.no.backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Stores password-based login credentials for a user.
 * Uses a {@code BIGINT} key as per the DB schema, does not extend
 * Base Entity.
 *
 * @author norakirknes
 * @version 0.1
 * @since 0.1
 * @see User
 */
@Entity
@Table(name = "app_user_local_credential")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocalCredential {

  /**
   * Primary key. Auto-incremented by the database.
   */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "credential_id", updatable = false, nullable = false)
  private Long id;

  /**
   * The user these credentials belong to.
   */
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false, updatable = false)
  private User user;

  /**
   * Bcrypt hash of the user's password.
   */
  @Column(name = "password_hash", nullable = false)
  private String passwordHash;

  /**
   * When {@code true}, the user must change their password on next login.
   */
  @Builder.Default
  @Column(name = "must_change_pw", nullable = false)
  private boolean mustChangePw = false;

  /**
   * Timestamp of the last password change.
   */
  @Column(name = "last_changed_at", nullable = false)
  private LocalDateTime lastChangedAt;

  /**
   * Number of consecutive failed login attempts since the last success.
   * Used to trigger account locking.
   */
  @Builder.Default
  @Column(name = "failed_attempts", nullable = false)
  private int failedAttempts = 0;

  /**
   * Set by too many failed password attempts.
   * If set, the account is locked until this timestamp.
   */
  @Column(name = "locked_until")
  private LocalDateTime lockedUntil;

  /**
   * Timestamp of when this credential record was created. Set once on persist.
   */
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  /**
   * Timestamp of the last update to this credential record.
   */
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  /**
   * Sets {@code createdAt}, {@code updatedAt} and {@code lastChangedAt} before
   * the entity is first persisted.
   */
  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
    lastChangedAt = LocalDateTime.now();
  }

  /**
   * Refreshes {@code updatedAt} before every update.
   */
  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
