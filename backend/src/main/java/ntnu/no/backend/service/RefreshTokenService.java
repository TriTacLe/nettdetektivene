package ntnu.no.backend.service;

import lombok.RequiredArgsConstructor;
import ntnu.no.backend.dto.auth.AuthResponseDTO;
import ntnu.no.backend.exception.RefreshTokenException;
import ntnu.no.backend.model.auth.RefreshToken;
import ntnu.no.backend.model.auth.Role;
import ntnu.no.backend.model.auth.User;
import ntnu.no.backend.repository.RefreshTokenRepository;
import ntnu.no.backend.security.JWTUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;


/**
 * Service handling refresh token lifecycle; creation, rotation and revocation.
 * Refresh tokens are stored as SHA-256 hashes; the raw token is only ever held by the client.
 * On token rotation, the old token is soft-revoked and a new one is issued.
 * On login, all existing tokens are hard-deleted to prevent session accumulation.
 *
 * @author norakirknes
 * @version 0.1
 * @since 0.1
 * @see JWTUtil
 * @see RefreshTokenRepository
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

  private final RefreshTokenRepository refreshTokenRepository;
  private final JWTUtil jwtUtil;

  @Value("${jwt.refresh-expiration-ms}")
  private long refreshTokenExpirationInMs;

  /**
   * Creates a new refresh token, encoding the random 32 bytes
   * to string with Base64. Builds refresh token, storing only hashed
   * token in db. Returns the raw token for client.
   * TODO: populate ipAddress
   * @param user the user to generate the refresh token to
   * @return the raw token as a string
   */
  @Transactional
  public String create(User user) {
    byte[] bytes = new byte[32];
    new SecureRandom().nextBytes(bytes);
    String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

    RefreshToken token = RefreshToken.builder()
      .user(user)
      .tokenHash(sha256(rawToken))
      .expiresAt(LocalDateTime.now().plus(refreshTokenExpirationInMs, ChronoUnit.MILLIS))
      .build();
    refreshTokenRepository.save(token);
    return rawToken;
  }

  /**
   * Hard deletes all refresh tokens belonging to the given user.
   * Called before issuing a new token to prevent session accumulation.
   *
   * @param user the user whose refresh tokens should be deleted
   */
  @Transactional
  public void revokeAll(User user) { refreshTokenRepository.deleteByUser(user); }

  /**
   * Soft-revokes a single refresh token by setting {@code revokedAt}.
   * Used on logout to invalidate the current session.
   *
   * @param rawToken the raw refresh token to revoke
   */
  @Transactional
  public void revoke(String rawToken) {
    RefreshToken token = refreshTokenRepository.findByTokenHash(sha256(rawToken))
      .orElseThrow(() -> new RefreshTokenException("Refresh token not found"));
    softRevoke(token);
  }

  /**
   * Refreshes the refresh token, revoking the old one and
   * generating a new one. Soft revoke old refresh token. Old JWT
   * forwards the {@code feideId} claim, if present.
   *
   * @param rawToken the raw expired refresh token
   * @param expiredJwt the raw expired JWT token, used for forwarding claims (optional)
   * @return a {@link AuthResponseDTO} containing the successful authentication.
   */
  @Transactional
  public AuthResponseDTO refresh(String rawToken, String expiredJwt) {
    RefreshToken token = refreshTokenRepository.findByTokenHash(sha256(rawToken))
      .orElseThrow(() -> new RefreshTokenException("Refresh token not found"));

    if (token.getRevokedAt() != null) {
      throw new RefreshTokenException("Refresh token has been revoked");
    }
    if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
      throw new RefreshTokenException("Refresh token has expired");
    }
    User user = token.getUser();
    softRevoke(token);
    String newRefreshToken = create(user);

    String newAccessToken = jwtUtil.introspect(expiredJwt)
      .map(claims -> claims.get("feideId", String.class))
      .map(feideId -> jwtUtil.generateToken(user, feideId))
      .orElseGet(() -> jwtUtil.generateToken(user));

    return new AuthResponseDTO(
      newAccessToken,
      newRefreshToken,
      user.getRoles().stream().map(Role::getAuthority).toList()
    );
  }

  /**
   * Sets {@code revokedAt} on the given token and persists it.
   *
   * @param token the token to soft-revoke
   */
  private void softRevoke(RefreshToken token) {
    token.setRevokedAt(LocalDateTime.now());
    refreshTokenRepository.save(token);
  }

  /**
   * Private helper method for hashing the raw token
   * using sha 256 hashing algorithm, and converting to
   * hex format and returning the hashed token in hex format as string.
   *
   * @param rawToken the token to be hashed by SHA256 algorithm
   * @return the hashed token in hex format
   */
  private String sha256(String rawToken) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));

      StringBuilder hex = new StringBuilder();
      for (byte b : hash) {
        hex.append(String.format("%02x", b));
      }
      return hex.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 algorithm not available", e);
    }
  }
}
