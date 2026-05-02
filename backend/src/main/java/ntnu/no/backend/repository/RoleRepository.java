package ntnu.no.backend.repository;

import ntnu.no.backend.model.auth.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link Role} entities
 *
 * @author norakirknes
 * @version 0.1
 * @since 0.1
 * @see Role
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {

  /**
   * Retrieves a {@link Role} by role name.
   *
   * @param name the role name
   * @return a {@link Optional} containing the role matching the role name.
   */
  Optional<Role> findByRoleName(String name);
}
