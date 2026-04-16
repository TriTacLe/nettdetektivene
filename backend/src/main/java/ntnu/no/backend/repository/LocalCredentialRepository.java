package ntnu.no.backend.repository;

import ntnu.no.backend.model.auth.LocalCredential;
import ntnu.no.backend.model.auth.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 *
 * @author norakirknes
 * @version 0.1
 * @since 0.1
 * @see LocalCredential
 */
@Repository
public interface LocalCredentialRepository extends JpaRepository<LocalCredential, Long> {

  /**
   * Retrieves user local credentials by user.
   *
   * @param user the user to retrieve local credentials for
   * @return a {@link Optional} containing the loca credentials
   */
  Optional<LocalCredential> findByUser(User user);
}
