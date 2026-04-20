package ntnu.no.backend.repository;

import ntnu.no.backend.model.auth.AppUserIdentity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for {@link AppUserIdentity} entities
 *
 * @author norakirknes
 * @version 0.1
 * @since 0.1
 * @see AppUserIdentity
 */
@Repository
public interface AppUserIdentityRepository extends JpaRepository<AppUserIdentity, Long> {

  /**
   * Looks up an identity by provider name and the provider-issued user identifier.
   * Used during login to resolve which user the incoming token belongs to.
   *
   * @param providerName   the name of the OAuth provider (e.g. {@code "feide"})
   * @param providerUserId the unique user identifier issued by the provider
   * @return an {@link Optional} containing the matching identity, or empty if not found
   */
  @EntityGraph(attributePaths = {"user"})
  Optional<AppUserIdentity> findByProviderNameAndProviderUserId(String providerName, String providerUserId);
}
