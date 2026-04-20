package ntnu.no.backend.repository;

import ntnu.no.backend.model.auth.User;
import ntnu.no.backend.model.auth.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link UserProfile} entities
 *
 * @author norakirknes
 * @version 0.1
 * @since 0.1
 * @see UserProfile
 */
@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {

  /**
   * Retrieves a profile by FEIDE identifier.
   * Used during FEIDE login to check whether a profile already exists
   * for the authenticated FEIDE user.
   *
   * @param feideId the FEIDE identifier to search by
   * @return an {@link Optional} containing the matching profile, or empty if not found
   */
  Optional<UserProfile> findByFeideId(String feideId);

  /**
   * Retrieves a profile by the owning user.
   *
   * @param user the user whose profile to retrieve
   * @return an {@link Optional} containing the matching profile, or empty if not found
   */
  Optional<UserProfile> findByUser(User user);
}
