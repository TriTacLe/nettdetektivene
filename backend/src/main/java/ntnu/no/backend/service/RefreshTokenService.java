package ntnu.no.backend.service;

import lombok.RequiredArgsConstructor;
import ntnu.no.backend.dto.auth.AuthResponseDTO;
import ntnu.no.backend.exception.RefreshTokenException;
import ntnu.no.backend.model.auth.RefreshToken;
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

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

  private final RefreshTokenRepository refreshTokenRepository;
  private final JWTUtil jwtUtil;

  @Value("${jwt.refresh-expiration-ms}")
  private long refreshTokenExpirationInMs;

  // TODO: populate ipAddress
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

  @Transactional
  public void revoke(String rawToken) {
    RefreshToken token = refreshTokenRepository.findByTokenHash(sha256(rawToken))
      .orElseThrow(() -> new RefreshTokenException("Refresh token not found"));
    token.setRevokedAt(LocalDateTime.now());
    refreshTokenRepository.save(token);
  }

  @Transactional
  public AuthResponseDTO refresh(String rawToken) {
    RefreshToken token = refreshTokenRepository.findByTokenHash(sha256(rawToken))
      .orElseThrow(() -> new RefreshTokenException("Refresh token not found"));

    if (token.getRevokedAt() != null) {
      throw new RefreshTokenException("Refresh token has been revoked");
    }
    if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
      throw new RefreshTokenException("Refresh token has expired");
    }

    token.setRevokedAt(LocalDateTime.now());
    refreshTokenRepository.save(token);

    User user = token.getUser();
    String newRefreshToken = create(user);
    String newAccessToken = jwtUtil.generateToken(user);

    return AuthResponseDTO.builder()
      .token(newAccessToken)
      .refreshToken(newRefreshToken)
      .build();
  }

  private String sha256(String input) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
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
