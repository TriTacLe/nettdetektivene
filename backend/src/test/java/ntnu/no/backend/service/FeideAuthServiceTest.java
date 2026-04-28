package ntnu.no.backend.service;

import jakarta.persistence.EntityNotFoundException;
import ntnu.no.backend.dto.auth.FeideLoginRequestDTO;
import ntnu.no.backend.dto.auth.FeideAuthResponseDTO;
import ntnu.no.backend.exception.EmailAlreadyExistsException;
import ntnu.no.backend.model.auth.AppUserIdentity;
import ntnu.no.backend.model.auth.LocalCredential;
import ntnu.no.backend.model.auth.Role;
import ntnu.no.backend.model.auth.User;
import ntnu.no.backend.model.auth.UserProfile;
import ntnu.no.backend.model.enums.RoleName;
import ntnu.no.backend.repository.AppUserIdentityRepository;
import ntnu.no.backend.repository.LocalCredentialRepository;
import ntnu.no.backend.repository.RoleRepository;
import ntnu.no.backend.repository.UserProfileRepository;
import ntnu.no.backend.repository.UserRepository;
import ntnu.no.backend.security.JWTUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeideAuthServiceTest {

  @Mock
  private AppUserIdentityRepository identityRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private UserProfileRepository userProfileRepository;

  @Mock
  private RoleRepository roleRepository;

  @Mock
  private LocalCredentialRepository localCredentialRepository;

  @Mock
  private PasswordEncoder passwordEncoder;

  @Mock
  private AuthenticationManager authenticationManager;

  @Mock
  private RefreshTokenService refreshTokenService;

  @Mock
  private JWTUtil jwtUtil;

  @InjectMocks
  private FeideAuthService feideAuthService;

  private FeideLoginRequestDTO createLoginRequest(String email, String password, String affiliation) {
    FeideLoginRequestDTO dto = new FeideLoginRequestDTO();
    dto.setEmail(email);
    dto.setPassword(password);
    dto.setAffiliation(affiliation);
    dto.setDisplayName("Test User");
    return dto;
  }

  private User createTestUser() {
    Role studentRole = Role.builder()
        .id(UUID.randomUUID())
        .roleName(RoleName.STUDENT.getValue())
        .build();

    return User.builder()
        .id(UUID.randomUUID())
        .displayName("Test User")
        .email("test@example.com")
        .roles(List.of(studentRole))
        .build();
  }

  private AppUserIdentity createTestIdentity(User user, String feideId) {
    return AppUserIdentity.builder()
        .id(UUID.randomUUID())
        .user(user)
        .providerName("feide")
        .providerUserId(feideId)
        .providerEmail(user.getEmail())
        .build();
  }

  @Test
  void login_withExistingFeideId_returnsTokens() {
    User user = createTestUser();
    String feideId = UUID.randomUUID().toString();
    AppUserIdentity identity = createTestIdentity(user, feideId);

    FeideLoginRequestDTO dto = createLoginRequest("test@example.com", "password", "student");

    when(identityRepository.findByProviderNameAndProviderUserId("feide", feideId))
        .thenReturn(Optional.of(identity));
    when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(null);
    when(refreshTokenService.revokeAll(user)).thenReturn(null);
    when(refreshTokenService.create(user)).thenReturn("refresh.token");
    when(jwtUtil.generateToken(user, feideId)).thenReturn("jwt.token");

    FeideAuthResponseDTO result = feideAuthService.login(dto, feideId);

    assertThat(result.token()).isEqualTo("jwt.token");
    assertThat(result.getRefreshToken()).isEqualTo("refresh.token");
    assertThat(result.getFeideId()).isEqualTo(feideId);
    verify(refreshTokenService).revokeAll(user);
    verify(jwtUtil).generateToken(user, feideId);
  }

  @Test
  void login_withExistingFeideId_throwsEntityNotFound_whenIdentityNotFound() {
    User user = createTestUser();
    String feideId = UUID.randomUUID().toString();

    FeideLoginRequestDTO dto = createLoginRequest("test@example.com", "password", "student");

    when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(null);
    when(identityRepository.findByProviderNameAndProviderUserId("feide", feideId))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> feideAuthService.login(dto, feideId))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessageContaining(feideId);
  }

  @Test
  void login_withExistingEmailWithoutFeideId_returnsTokens() {
    User user = createTestUser();
    String feideId = UUID.randomUUID().toString();
    AppUserIdentity identity = createTestIdentity(user, feideId);

    FeideLoginRequestDTO dto = createLoginRequest("test@example.com", "password", "student");

    when(userRepository.existsByEmail("test@example.com")).thenReturn(true);
    when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(null);
    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
    when(identityRepository.findByProviderNameAndUser("feide", user))
        .thenReturn(Optional.of(identity));
    when(refreshTokenService.revokeAll(user)).thenReturn(null);
    when(refreshTokenService.create(user)).thenReturn("refresh.token");
    when(jwtUtil.generateToken(user, feideId)).thenReturn("jwt.token");

    FeideAuthResponseDTO result = feideAuthService.login(dto, null);

    assertThat(result.token()).isEqualTo("jwt.token");
    assertThat(result.getRefreshToken()).isEqualTo("refresh.token");
    assertThat(result.getFeideId()).isEqualTo(feideId);
  }

  @Test
  void login_withExistingEmailWithoutFeideId_throwsEntityNotFound_whenIdentityNotFound() {
    User user = createTestUser();

    FeideLoginRequestDTO dto = createLoginRequest("test@example.com", "password", "student");

    when(userRepository.existsByEmail("test@example.com")).thenReturn(true);
    when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(null);
    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
    when(identityRepository.findByProviderNameAndUser("feide", user))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> feideAuthService.login(dto, null))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessageContaining("test@example.com");
  }

  @Test
  void login_withNewUser_createsAccountAndReturnsTokens() {
    FeideLoginRequestDTO dto = createLoginRequest("newuser@example.com", "password", "student");
    Role studentRole = Role.builder()
        .id(UUID.randomUUID())
        .roleName(RoleName.STUDENT.getValue())
        .build();

    when(userRepository.existsByEmail("newuser@example.com")).thenReturn(false);
    when(roleRepository.findByRoleName(RoleName.STUDENT.getValue())).thenReturn(Optional.of(studentRole));
    when(jwtUtil.generateToken(any(User.class), anyString())).thenReturn("jwt.token");
    when(refreshTokenService.create(any(User.class))).thenReturn("refresh.token");
    when(passwordEncoder.encode("password")).thenReturn("hashed.password");

    FeideAuthResponseDTO result = feideAuthService.login(dto, null);

    assertThat(result.token()).isEqualTo("jwt.token");
    assertThat(result.getRefreshToken()).isEqualTo("refresh.token");
    assertThat(result.getFeideId()).isNotNull();
    verify(userRepository).save(any(User.class));
    verify(refreshTokenService).create(any(User.class));
  }

  @Test
  void login_withNewUser_throwsEmailAlreadyExists_whenEmailExists() {
    FeideLoginRequestDTO dto = createLoginRequest("existing@example.com", "password", "student");

    when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);
    when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
        .thenThrow(new BadCredentialsException("Bad credentials"));

    assertThatThrownBy(() -> feideAuthService.login(dto, null))
        .isInstanceOf(BadCredentialsException.class);
  }

  @Test
  void login_withNewUser_usesEmailAsDisplayName_whenDisplayNameBlank() {
    FeideLoginRequestDTO dto = new FeideLoginRequestDTO();
    dto.setEmail("user@example.com");
    dto.setPassword("password");
    dto.setAffiliation("student");
    dto.setDisplayName("");

    Role studentRole = Role.builder()
        .id(UUID.randomUUID())
        .roleName(RoleName.STUDENT.getValue())
        .build();

    when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
    when(roleRepository.findByRoleName(RoleName.STUDENT.getValue())).thenReturn(Optional.of(studentRole));
    when(jwtUtil.generateToken(any(User.class), anyString())).thenReturn("jwt.token");
    when(refreshTokenService.create(any(User.class))).thenReturn("refresh.token");
    when(passwordEncoder.encode("password")).thenReturn("hashed.password");

    FeideAuthResponseDTO result = feideAuthService.login(dto, null);

    verify(userRepository).save(argThat(user -> "user".equals(user.getDisplayName())));
  }

  @Test
  void login_withNewUser_withInstructorAffiliation_assignsInstructorRole() {
    FeideLoginRequestDTO dto = createLoginRequest("instructor@example.com", "password", "instructor");
    Role instructorRole = Role.builder()
        .id(UUID.randomUUID())
        .roleName(RoleName.INSTRUCTOR.getValue())
        .build();

    when(userRepository.existsByEmail("instructor@example.com")).thenReturn(false);
    when(roleRepository.findByRoleName(RoleName.INSTRUCTOR.getValue())).thenReturn(Optional.of(instructorRole));
    when(jwtUtil.generateToken(any(User.class), anyString())).thenReturn("jwt.token");
    when(refreshTokenService.create(any(User.class))).thenReturn("refresh.token");
    when(passwordEncoder.encode("password")).thenReturn("hashed.password");

    FeideAuthResponseDTO result = feideAuthService.login(dto, null);

    assertThat(result.getRoles()).containsExactly("instructor");
  }

  @Test
  void login_withNewUser_throwsIllegalArgumentException_whenUnknownAffiliation() {
    FeideLoginRequestDTO dto = createLoginRequest("user@example.com", "password", "unknown");

    when(userRepository.existsByEmail("user@example.com")).thenReturn(false);

    assertThatThrownBy(() -> feideAuthService.login(dto, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("unknown");
  }

  @Test
  void login_withNewUser_throwsEntityNotFoundException_whenRoleNotFound() {
    FeideLoginRequestDTO dto = createLoginRequest("user@example.com", "password", "student");

    when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
    when(roleRepository.findByRoleName(RoleName.STUDENT.getValue())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> feideAuthService.login(dto, null))
        .isInstanceOf(EntityNotFoundException.class);
  }

  @Test
  void login_withNewUser_createsLocalCredential_withHashedPassword() {
    FeideLoginRequestDTO dto = createLoginRequest("user@example.com", "password123", "student");
    Role studentRole = Role.builder()
        .id(UUID.randomUUID())
        .roleName(RoleName.STUDENT.getValue())
        .build();

    when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
    when(roleRepository.findByRoleName(RoleName.STUDENT.getValue())).thenReturn(Optional.of(studentRole));
    when(jwtUtil.generateToken(any(User.class), anyString())).thenReturn("jwt.token");
    when(refreshTokenService.create(any(User.class))).thenReturn("refresh.token");
    when(passwordEncoder.encode("password123")).thenReturn("hashed.password123");

    feideAuthService.login(dto, null);

    verify(passwordEncoder).encode("password123");
    verify(localCredentialRepository).save(argThat(credential ->
        "hashed.password123".equals(credential.getPasswordHash())));
  }

  @Test
  void login_withNewUser_createsAppUserIdentity_withCorrectProvider() {
    FeideLoginRequestDTO dto = createLoginRequest("user@example.com", "password", "student");
    Role studentRole = Role.builder()
        .id(UUID.randomUUID())
        .roleName(RoleName.STUDENT.getValue())
        .build();

    when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
    when(roleRepository.findByRoleName(RoleName.STUDENT.getValue())).thenReturn(Optional.of(studentRole));
    when(jwtUtil.generateToken(any(User.class), anyString())).thenReturn("jwt.token");
    when(refreshTokenService.create(any(User.class))).thenReturn("refresh.token");
    when(passwordEncoder.encode("password")).thenReturn("hashed.password");

    FeideAuthResponseDTO result = feideAuthService.login(dto, null);

    verify(identityRepository).save(argThat(identity ->
        "feide".equals(identity.getProviderName()) &&
        "user@example.com".equals(identity.getProviderEmail()) &&
        result.getFeideId().equals(identity.getProviderUserId())));
  }

  @Test
  void login_withNewUser_createsUserProfile_withFeideId() {
    FeideLoginRequestDTO dto = createLoginRequest("user@example.com", "password", "student");
    Role studentRole = Role.builder()
        .id(UUID.randomUUID())
        .roleName(RoleName.STUDENT.getValue())
        .build();

    when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
    when(roleRepository.findByRoleName(RoleName.STUDENT.getValue())).thenReturn(Optional.of(studentRole));
    when(jwtUtil.generateToken(any(User.class), anyString())).thenReturn("jwt.token");
    when(refreshTokenService.create(any(User.class))).thenReturn("refresh.token");
    when(passwordEncoder.encode("password")).thenReturn("hashed.password");

    FeideAuthResponseDTO result = feideAuthService.login(dto, null);

    verify(userProfileRepository).save(argThat(profile ->
        result.getFeideId().equals(profile.getFeideId())));
  }
}