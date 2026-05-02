package ntnu.no.backend.repository;

import ntnu.no.backend.model.auth.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link User} entities
 *
 * @author norakirknes
 * @version 0.1
 * @since 0.1
 * @see User
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

  /**
   * Retrieves a user by their unique identifier, eagerly loading roles and local credentials.
   *
   * @param id the UUID of the user to retrieve
   * @return an {@link Optional} containing the user with the specified id, or empty if not found
   */
  @NonNull
  @EntityGraph(attributePaths = {"roles", "localCredential"})
  Optional<User> findById(@NonNull UUID id);

  /**
   * Retrieves a user identified by a specific email address.
   *
   * @param email the email address used to search a user
   * @return a {@link Optional} containing the user with the specified email
   */
  @EntityGraph(attributePaths = {"roles", "localCredential"})
  Optional<User> findByEmail(String email);

  /**
   * Check whether a user with a specific email exists
   *
   * @param email the email address to check if a user exists by
   * @return {@code true} if a user by that email address exists, {@code false} otherwise
   */
  boolean existsByEmail(String email);
}
