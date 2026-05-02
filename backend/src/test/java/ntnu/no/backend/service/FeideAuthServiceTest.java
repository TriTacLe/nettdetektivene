package ntnu.no.backend.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import ntnu.no.backend.dto.auth.FeideAuthResponseDTO;
import ntnu.no.backend.dto.auth.FeideLoginRequestDTO;
import ntnu.no.backend.exception.EmailAlreadyExistsException;
import ntnu.no.backend.model.auth.AppUserIdentity;
import ntnu.no.backend.model.auth.LocalCredential;
import ntnu.no.backend.model.auth.Role;
import ntnu.no.backend.model.auth.User;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Unit tests for {@link FeideAuthService}.
 *
 * @author tri
 * @version 0.1
 * @since 0.1
 */
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

  @Test
  void login_withFeideId_authenticatesAndReturnsTokens() {
    FeideLoginRequestDTO request = new FeideLoginRequestDTO();
    request.setEmail("nora@ntnu.no");
    request.setPassword("password");
    request.setAffiliation("student");
    request.setDisplayName("Nora");

    String feideId = "feide-123";
    User user = createTestUser("nora@ntnu.no", "Nora", RoleName.STUDENT);
    AppUserIdentity identity = AppUserIdentity.builder()
      .providerName("feide")
      .providerUserId(feideId)
      .providerEmail("nora@ntnu.no")
      .user(user)
      .build();

    when(identityRepository.findByProviderNameAndProviderUserId("feide", feideId))
      .thenReturn(Optional.of(identity));
    when(refreshTokenService.create(user)).thenReturn("refresh-token");
    when(jwtUtil.generateToken(user, feideId)).thenReturn("access-token");

    FeideAuthResponseDTO response = feideAuthService.login(request, feideId);

    verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    verify(refreshTokenService).revokeAll(user);
    verify(jwtUtil).generateToken(user, feideId);
    verify(refreshTokenService).create(user);

    assert response.token().equals("access-token");
    assert response.refreshToken().equals("refresh-token");
    assert response.feideId().equals(feideId);
    assert response.roles().contains("student");
  }

  @Test
  void loginWithoutFeideId_existingUser_authenticatesAndReturnsTokens() {
    FeideLoginRequestDTO request = new FeideLoginRequestDTO();
    request.setEmail("nora@ntnu.no");
    request.setPassword("password");
    request.setAffiliation("student");
    request.setDisplayName("Nora");

    String feideId = "feide-123";
    User user = createTestUser("nora@ntnu.no", "Nora", RoleName.STUDENT);
    AppUserIdentity identity = AppUserIdentity.builder()
      .providerName("feide")
      .providerUserId(feideId)
      .providerEmail("nora@ntnu.no")
      .user(user)
      .build();

    when(userRepository.existsByEmail("nora@ntnu.no")).thenReturn(true);
    when(userRepository.findByEmail("nora@ntnu.no")).thenReturn(Optional.of(user));
    when(identityRepository.findByProviderNameAndUser("feide", user))
      .thenReturn(Optional.of(identity));
    when(refreshTokenService.create(user)).thenReturn("refresh-token");
    when(jwtUtil.generateToken(user, feideId)).thenReturn("access-token");

    FeideAuthResponseDTO response = feideAuthService.login(request, null);

    verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    verify(userRepository).findByEmail("nora@ntnu.no");
    verify(jwtUtil).generateToken(user, feideId);

    assert response.token().equals("access-token");
    assert response.feideId().equals(feideId);
  }

  @Test
  void loginWithoutFeideId_newUser_createsUserAndTokens() {
    FeideLoginRequestDTO request = new FeideLoginRequestDTO();
    request.setEmail("nora@ntnu.no");
    request.setPassword("password");
    request.setAffiliation("student");
    request.setDisplayName("Nora");

    Role studentRole = Role.builder()
      .roleName(RoleName.STUDENT.getValue())
      .build();

    when(userRepository.existsByEmail("nora@ntnu.no")).thenReturn(false);
    when(roleRepository.findByRoleName(RoleName.STUDENT.getValue()))
      .thenReturn(Optional.of(studentRole));
    when(passwordEncoder.encode(anyString())).thenReturn("hashed-password");
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(localCredentialRepository.save(any(LocalCredential.class)))
      .thenAnswer(invocation -> invocation.getArgument(0));
    when(identityRepository.save(any(AppUserIdentity.class)))
      .thenAnswer(invocation -> invocation.getArgument(0));
    when(userProfileRepository.save(any()))
      .thenAnswer(invocation -> invocation.getArgument(0));
    when(refreshTokenService.create(any(User.class))).thenReturn("refresh-token");
    when(jwtUtil.generateToken(any(User.class), anyString())).thenReturn("access-token");

    FeideAuthResponseDTO response = feideAuthService.login(request, null);

    verify(userRepository).save(any(User.class));
    verify(localCredentialRepository).save(any(LocalCredential.class));
    verify(identityRepository).save(any(AppUserIdentity.class));
    verify(userProfileRepository).save(any());
    verify(refreshTokenService).revokeAll(any(User.class));
    verify(jwtUtil).generateToken(any(User.class), anyString());

    assert response.token().equals("access-token");
    assert response.refreshToken().equals("refresh-token");
    assert response.roles().contains("student");
    assert response.feideId() != null;
  }

  @Test
  void loginWithoutFeideId_existingEmail_throwsBadCredentialsException() {
    FeideLoginRequestDTO request = new FeideLoginRequestDTO();
    request.setEmail("nora@ntnu.no");
    request.setPassword("password");
    request.setAffiliation("student");
    request.setDisplayName("Nora");

    when(userRepository.existsByEmail("nora@ntnu.no")).thenReturn(true);
    when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
      .thenThrow(new BadCredentialsException("Bad credentials"));

    try {
      feideAuthService.login(request, null);
      assert false : "Should have thrown exception";
    } catch (BadCredentialsException e) {
      assert e.getMessage().contains("Bad credentials");
    }
  }

  @Test
  void login_withUnknownAffiliation_throwsIllegalArgumentException() {
    FeideLoginRequestDTO request = new FeideLoginRequestDTO();
    request.setEmail("test@example.com");
    request.setPassword("password");
    request.setAffiliation("unknown");
    request.setDisplayName("Test");

    try {
      feideAuthService.login(request, null);
      assert false : "Should have thrown IllegalArgumentException";
    } catch (IllegalArgumentException e) {
      assert e.getMessage().contains("Unknown affiliation: unknown");
    }
  }

  @Test
  void login_withoutDisplayName_defaultsToEmailPrefix() {
    FeideLoginRequestDTO request = new FeideLoginRequestDTO();
    request.setEmail("nora@ntnu.no");
    request.setPassword("password");
    request.setAffiliation("student");
    request.setDisplayName(null);

    Role studentRole = Role.builder()
      .roleName(RoleName.STUDENT.getValue())
      .build();

    when(userRepository.existsByEmail("nora@ntnu.no")).thenReturn(false);
    when(roleRepository.findByRoleName(RoleName.STUDENT.getValue()))
      .thenReturn(Optional.of(studentRole));
    when(passwordEncoder.encode(anyString())).thenReturn("hashed-password");
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(localCredentialRepository.save(any(LocalCredential.class)))
      .thenAnswer(invocation -> invocation.getArgument(0));
    when(identityRepository.save(any(AppUserIdentity.class)))
      .thenAnswer(invocation -> invocation.getArgument(0));
    when(userProfileRepository.save(any()))
      .thenAnswer(invocation -> invocation.getArgument(0));
    when(refreshTokenService.create(any(User.class))).thenReturn("refresh-token");
    when(jwtUtil.generateToken(any(User.class), anyString())).thenReturn("access-token");

    feideAuthService.login(request, null);

    verify(userRepository).save(argThat(user -> user.getDisplayName().equals("nora")));
  }

  @Test
  void login_withEmptyDisplayName_defaultsToEmailPrefix() {
    FeideLoginRequestDTO request = new FeideLoginRequestDTO();
    request.setEmail("nora@ntnu.no");
    request.setPassword("password");
    request.setAffiliation("instructor");
    request.setDisplayName("  ");

    Role instructorRole = Role.builder()
      .roleName(RoleName.INSTRUCTOR.getValue())
      .build();

    when(userRepository.existsByEmail("nora@ntnu.no")).thenReturn(false);
    when(roleRepository.findByRoleName(RoleName.INSTRUCTOR.getValue()))
      .thenReturn(Optional.of(instructorRole));
    when(passwordEncoder.encode(anyString())).thenReturn("hashed-password");
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(localCredentialRepository.save(any(LocalCredential.class)))
      .thenAnswer(invocation -> invocation.getArgument(0));
    when(identityRepository.save(any(AppUserIdentity.class)))
      .thenAnswer(invocation -> invocation.getArgument(0));
    when(refreshTokenService.create(any(User.class))).thenReturn("refresh-token");
    when(jwtUtil.generateToken(any(User.class), anyString())).thenReturn("access-token");

    feideAuthService.login(request, null);

    verify(userRepository).save(argThat(user -> user.getDisplayName().equals("nora")));
  }

  @Test
  void login_studentRole_createsUserWithStudentRole() {
    FeideLoginRequestDTO request = new FeideLoginRequestDTO();
    request.setEmail("nora@ntnu.no");
    request.setPassword("password");
    request.setAffiliation("student");
    request.setDisplayName("Nora");

    Role studentRole = Role.builder()
      .roleName(RoleName.STUDENT.getValue())
      .build();

    when(userRepository.existsByEmail("nora@ntnu.no")).thenReturn(false);
    when(roleRepository.findByRoleName(RoleName.STUDENT.getValue()))
      .thenReturn(Optional.of(studentRole));
    when(passwordEncoder.encode(anyString())).thenReturn("hashed-password");
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(localCredentialRepository.save(any(LocalCredential.class)))
      .thenAnswer(invocation -> invocation.getArgument(0));
    when(identityRepository.save(any(AppUserIdentity.class)))
      .thenAnswer(invocation -> invocation.getArgument(0));
    when(userProfileRepository.save(any()))
      .thenAnswer(invocation -> invocation.getArgument(0));
    when(refreshTokenService.create(any(User.class))).thenReturn("refresh-token");
    when(jwtUtil.generateToken(any(User.class), anyString())).thenReturn("access-token");

    FeideAuthResponseDTO response = feideAuthService.login(request, null);

    assert response.roles().contains("student");
  }

  @Test
  void login_instructorRole_createsUserWithInstructorRole() {
    FeideLoginRequestDTO request = new FeideLoginRequestDTO();
    request.setEmail("tri@ntnu.no");
    request.setPassword("password");
    request.setAffiliation("instructor");
    request.setDisplayName("Tri");

    Role instructorRole = Role.builder()
      .roleName(RoleName.INSTRUCTOR.getValue())
      .build();

    when(userRepository.existsByEmail("tri@ntnu.no")).thenReturn(false);
    when(roleRepository.findByRoleName(RoleName.INSTRUCTOR.getValue()))
      .thenReturn(Optional.of(instructorRole));
    when(passwordEncoder.encode(anyString())).thenReturn("hashed-password");
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(localCredentialRepository.save(any(LocalCredential.class)))
      .thenAnswer(invocation -> invocation.getArgument(0));
    when(identityRepository.save(any(AppUserIdentity.class)))
      .thenAnswer(invocation -> invocation.getArgument(0));
    when(refreshTokenService.create(any(User.class))).thenReturn("refresh-token");
    when(jwtUtil.generateToken(any(User.class), anyString())).thenReturn("access-token");

    FeideAuthResponseDTO response = feideAuthService.login(request, null);

    assert response.roles().contains("instructor");
  }

  @Test
  void login_withFeideId_nonExistentIdentity_throwsEntityNotFoundException() {
    FeideLoginRequestDTO request = new FeideLoginRequestDTO();
    request.setEmail("nora@ntnu.no");
    request.setPassword("password");
    request.setAffiliation("student");
    request.setDisplayName("Nora");

    String feideId = "non-existent-feide";
    when(identityRepository.findByProviderNameAndProviderUserId("feide", feideId))
      .thenReturn(Optional.empty());

    try {
      feideAuthService.login(request, feideId);
      assert false : "Should have thrown EntityNotFoundException";
    } catch (EntityNotFoundException e) {
      assert e.getMessage().contains("No FEIDE identity found for id: " + feideId);
    }
  }

  private User createTestUser(String email, String displayName, RoleName roleName) {
    Role role = Role.builder().roleName(roleName.getValue()).build();
    return User.builder()
      .email(email)
      .displayName(displayName)
      .roles(List.of(role))
      .build();
  }
}