package ntnu.no.backend.service;

import ntnu.no.backend.dto.auth.AuthResponseDTO;
import ntnu.no.backend.exception.RefreshTokenException;
import ntnu.no.backend.model.auth.RefreshToken;
import ntnu.no.backend.model.auth.Role;
import ntnu.no.backend.model.auth.User;
import ntnu.no.backend.repository.RefreshTokenRepository;
import ntnu.no.backend.security.JWTUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link RefreshTokenService}.
 *
 * @author tri
 * @version 0.1
 * @since 0.1
 */
@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

  @Mock
  private RefreshTokenRepository refreshTokenRepository;

  @Mock
  private JWTUtil jwtUtil;

  @InjectMocks
  private RefreshTokenService refreshTokenService;

  private User sampleUser() {
    User user = new User();
    user.setDisplayName("Test User");
    user.setEmail("test@example.com");
    Role role = new Role();
    role.setRoleName("sys_admin");
    List<Role> roles = new ArrayList<>();
    roles.add(role);
    user.setRoles(roles);
    return user;
  }

  private RefreshToken sampleRefreshToken(User user) {
    return RefreshToken.builder()
        .id(1L)
        .user(user)
        .tokenHash("hashed_token_value")
        .expiresAt(LocalDateTime.now().plusDays(7))
        .build();
  }

  @Test
  void create_returnsRawToken_andPersistsHash() {
    User user = sampleUser();
    when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

    String rawToken = refreshTokenService.create(user);

    assertThat(rawToken).isNotBlank();
    verify(refreshTokenRepository).save(any(RefreshToken.class));
  }

  @Test
  void revokeAll_deletesAllTokensForUser() {
    User user = sampleUser();

    refreshTokenService.revokeAll(user);

    verify(refreshTokenRepository).deleteByUser(user);
  }

  @Test
  void revoke_setsRevokedAt_whenTokenExists() {
    User user = sampleUser();
    RefreshToken token = sampleRefreshToken(user);
    when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

    refreshTokenService.revoke("raw_token_value");

    assertThat(token.getRevokedAt()).isNotNull();
    verify(refreshTokenRepository).save(token);
  }

  @Test
  void revoke_throwsRefreshTokenException_whenTokenNotFound() {
    when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> refreshTokenService.revoke("nonexistent_token"))
        .isInstanceOf(RefreshTokenException.class)
        .hasMessageContaining("Refresh token not found");
  }

  @Test
  void refresh_returnsNewTokens_whenValid() {
    User user = sampleUser();
    RefreshToken token = sampleRefreshToken(user);

    when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));
    when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));
    when(jwtUtil.introspect(any())).thenReturn(Optional.empty());
    when(jwtUtil.generateToken(any(User.class))).thenReturn("new.jwt.token");

    AuthResponseDTO result = refreshTokenService.refresh("raw_token_value", null);

    assertThat(result.token()).isEqualTo("new.jwt.token");
    assertThat(result.refreshToken()).isNotBlank();
    assertThat(result.roles()).contains("sys_admin");
  }

  @Test
  void refresh_throwsRefreshTokenException_whenTokenRevoked() {
    User user = sampleUser();
    RefreshToken token = sampleRefreshToken(user);
    token.setRevokedAt(LocalDateTime.now());
    when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

    assertThatThrownBy(() -> refreshTokenService.refresh("revoked_token", null))
        .isInstanceOf(RefreshTokenException.class)
        .hasMessageContaining("revoked");
  }

  @Test
  void refresh_throwsRefreshTokenException_whenTokenExpired() {
    User user = sampleUser();
    RefreshToken token = RefreshToken.builder()
        .id(2L)
        .user(user)
        .tokenHash("hashed_token_value")
        .expiresAt(LocalDateTime.now().minusDays(1))
        .build();
    when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

    assertThatThrownBy(() -> refreshTokenService.refresh("expired_token", null))
        .isInstanceOf(RefreshTokenException.class)
        .hasMessageContaining("expired");
  }

  @Test
  void refresh_throwsRefreshTokenException_whenTokenNotFound() {
    when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> refreshTokenService.refresh("nonexistent_token", null))
        .isInstanceOf(RefreshTokenException.class)
        .hasMessageContaining("Refresh token not found");
  }
}