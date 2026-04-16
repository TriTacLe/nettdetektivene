package ntnu.no.backend.repository;

import ntnu.no.backend.model.auth.RefreshToken;
import ntnu.no.backend.model.auth.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

  /**
   * Looks up a refresh token by its SHA-256 hash.
   * The raw token is never stored, only its hash. Incoming tokens
   * must be hashed before calling this method.
   *
   * @param tokenHash the SHA-256 hash of the raw refresh token
   * @return an {@link Optional} containing the matching token, or empty if not found
   */

  Optional<RefreshToken> findByTokenHash(String tokenHash);
  /**
   * Deletes all refresh tokens belonging to the given user.
   * Used to invalidate all active sessions.
   *
   * @param user the user whose refresh tokens should be deleted
   */
  void deleteByUser(User user);
}
