package ntnu.no.backend.model.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Represents an external OAuth/SSO provider identity linked to a user account.
 * A single user can have multiple identities from different providers.
 * Uses a {@code BIGINT} primary key as per the DB schema, does not extend Base Entity.
 *
 * @author norakirknes
 * @version 0.1
 * @since 0.1
 * @see User
 */
@Entity
@Table(
  name = "app_user_identity",
  uniqueConstraints = @UniqueConstraint(
    name = "uq_provider_identity",
    columnNames = {"provider_name", "provider_user_id"}
  )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppUserIdentity {

  /**
   * Primary key. Auto-incremented by the database.
   */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "identity_id", updatable = false, nullable = false)
  private Long id;

  /**
   * The user this identity belongs to.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false, updatable = false)
  private User user;

  /**
   * The name of the OAuth/SSO provider, in this case {@code "feide"}
   */
  @Column(name = "provider_name", nullable = false, length = 50)
  private String providerName;

  /**
   * The unique user identifier issued by the provider.
   */
  @Column(name = "provider_user_id", nullable = false)
  private String providerUserId;

  /**
   * The email address returned by the provider at the time of authentication.
   * May differ from the user's primary email, and may be {@code null} if the
   * provider does not expose it.
   */
  @Column(name = "provider_email")
  private String providerEmail;

  /**
   * Timestamp of when this identity record was created. Set once on persist, never updated.
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
