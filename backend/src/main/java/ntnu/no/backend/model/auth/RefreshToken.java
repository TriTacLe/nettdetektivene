package ntnu.no.backend.model.auth;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Stores a refresh token used for session renewal.
 * Uses a {@code BIGINT} key as per the DB schema, does not extend Base Entity.
 *
 * @author norakirknes
 * @version 0.1
 * @since 0.1
 * @see User
 */
@Entity
@Table(name = "app_user_refresh_token")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

  /**
   * Primary key. Auto-incremented by the database.
   */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "token_id", updatable = false, nullable = false)
  private Long id;

  /**
   * The user this token was issued to.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false, updatable = false)
  private User user;

  /**
   * SHA-256 hash of the raw token string.
   */
  @Column(name = "token_hash", nullable = false, length = 64)
  private String tokenHash;

  /**
   * Timestamp of when this token was issued. Set once on persist.
   */
  @Column(name = "issued_at", nullable = false, updatable = false)
  private LocalDateTime issuedAt;

  /**
   * Timestamp of when this token expires and can no longer be used.
   */
  @Column(name = "expires_at", nullable = false)
  private LocalDateTime expiresAt;

  /**
   * Timestamp of when this token was explicitly revoked.
   * Null if the token has not been revoked.
   */
  @Column(name = "revoked_at")
  private LocalDateTime revokedAt;

  /**
   * IP address of the client that was issued this token.
   */
  @Column(name = "ip_address")
  private byte[] ipAddress;

  /**
   * Sets {@code issuedAt} before the entity is first persisted.
   */
  @PrePersist
  protected void onCreate() {
    issuedAt = LocalDateTime.now();
  }
}
