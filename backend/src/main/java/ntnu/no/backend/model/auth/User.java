package ntnu.no.backend.model.auth;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import ntnu.no.backend.model.BaseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Represents a user account in the system. Implements {@link UserDetails}
 * for Spring Security integration.
 *
 * @author norakirknes
 * @version 0.1
 * @since 0.1
 * @see Role
 */
@Entity
@Table(name = "app_user")
@AttributeOverride(name = "id", column = @Column(name = "user_id"))
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class User extends BaseEntity implements UserDetails {

  /**
   * The name displayed to other users in the application.
   */
  @Column(name = "display_name", nullable = false)
  private String displayName;

  /**
   * The user's email address, must be unique across all accounts.
   */
  @Column(name = "email", nullable = false, unique = true)
  private String email;

  /**
   * Whether the account is active. Defaults to {@code true} on creation.
   */
  @Builder.Default
  @Column(name = "is_active", nullable = false)
  private boolean isActive = true;

  /**
   * Timestamp of the user's last activity on the application.
   * Null if the user has never been seen after registration.
   */
  @Column(name = "global_last_seen_at")
  private LocalDateTime globalLastSeenAt;

  /**
   * Timestamp of when the account was created. Set once on persist,
   * never updated.
   */
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  /**
   * Timestamp of the last update to an account.
   */
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  /**
   * The local password credential for this user.
   */
  @OneToOne(mappedBy = "user", fetch = FetchType.LAZY)
  private LocalCredential localCredential;

  /**
   * The roles assigned to this user.
   */
  @Builder.Default
  @ManyToMany(fetch = FetchType.EAGER)
  @JoinTable(
    name = "user_role",
    joinColumns = @JoinColumn(name = "user_id"),
    inverseJoinColumns = @JoinColumn(name = "role_id")
  )
  private List<Role> roles = new ArrayList<>();

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

  /**
   * Returns the user's assigned roles as Spring Security authorities.
   */
  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return roles;
  }

  /**
   * Returns the password hash from the user's local credential.
   * Returns empty string if no local credential exists (SSO-only user, FEIDE-simulating)
   */
  @Override
  public String getPassword() {
    return localCredential != null ? localCredential.getPasswordHash() : "";
  }

  /**
   * Spring Security uses email as the username.
   */
  @Override
  public String getUsername() {
    return email;
  }

  /**
   * Whether the user account is enabled
   *
   * @return {@link #isActive}
   */
  @Override
  public boolean isEnabled() {
    return isActive;
  }

  /**
   * Whether the user account is non locked, or locked
   *
   * @return {@code true} if the account is not locked, {@code} false otherwise
   */
  @Override
  public boolean isAccountNonLocked() {
    return localCredential == null || localCredential.getLockedUntil() == null
      || localCredential.getLockedUntil().isBefore(LocalDateTime.now());
  }
}
