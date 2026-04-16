package ntnu.no.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.security.core.GrantedAuthority;

/**
 * Represents a system role that can be assigned to users.
 * Implements {@link GrantedAuthority} so role instances can be
 * returned directly from {@code getAuthorities()}.
 *
 * @author norakirknes
 * @version 0.1
 * @since 0.1
 */
@Entity
@Table(name = "role")
@AttributeOverride(name = "id", column = @Column(name = "role_id", updatable = false, nullable = false))
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class Role extends BaseEntity implements GrantedAuthority {

  /**
   * The role name returned to Spring Security (STUDENT, TEACHER etc.)
   * Must be unique across all roles.
   */
  @Column(name = "role_name", nullable = false, unique = true, length = 50)
  private String roleName;

  /**
   * Returns the role name as the authority string used by Spring Security.
   */
  @Override
  public String getAuthority() {
    return roleName;
  }
}
