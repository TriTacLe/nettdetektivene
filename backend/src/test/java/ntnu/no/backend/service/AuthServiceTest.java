package ntnu.no.backend.service;

import jakarta.persistence.EntityNotFoundException;
import ntnu.no.backend.dto.auth.AuthResponseDTO;
import ntnu.no.backend.dto.auth.LoginRequestDTO;
import ntnu.no.backend.dto.auth.RegisterRequestDTO;
import ntnu.no.backend.exception.EmailAlreadyExistsException;
import ntnu.no.backend.model.auth.LocalCredential;
import ntnu.no.backend.model.auth.Role;
import ntnu.no.backend.model.auth.User;
import ntnu.no.backend.model.enums.RoleName;
import ntnu.no.backend.repository.LocalCredentialRepository;
import ntnu.no.backend.repository.RoleRepository;
import ntnu.no.backend.repository.UserRepository;
import ntnu.no.backend.security.JWTUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

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
 * Unit tests for {@link AuthService}.
 *
 * @author aninesst
 * @version 0.1
 * @since 0.1
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private LocalCredentialRepository localCredentialRepository;

  @Mock
  private PasswordEncoder passwordEncoder;

  @Mock
  private AuthenticationManager authenticationManager;

  @Mock
  private JWTUtil jwtUtil;

  @Mock
  private RoleRepository roleRepository;

  @Mock
  private RefreshTokenService refreshTokenService;

  @InjectMocks
  private AuthService authService;

  private RegisterRequestDTO sampleRegisterDTO() {
    RegisterRequestDTO dto = new RegisterRequestDTO();
    dto.setDisplayName("New User");
    dto.setEmail("newuser@example.com");
    dto.setPassword("securePassword123");
    dto.setRole(RoleName.SYS_ADMIN.getValue());
    return dto;
  }

  private LoginRequestDTO sampleLoginDTO() {
    LoginRequestDTO dto = new LoginRequestDTO();
    dto.setEmail("test@example.com");
    dto.setPassword("securePassword123");
    return dto;
  }

  private User sampleUser(Role... assignedRoles) {
    User user = new User();
    user.setDisplayName("Test User");
    user.setEmail("test@example.com");
    List<Role> roles = new ArrayList<>();
    for (Role role : assignedRoles) {
      roles.add(role);
    }
    user.setRoles(roles);
    return user;
  }

  private Role sampleRole(String roleName) {
    Role role = new Role();
    role.setRoleName(roleName);
    return role;
  }

  @Test
  void register_returnsAuthResponse_onSuccess() {
    RegisterRequestDTO dto = sampleRegisterDTO();
    Role role = sampleRole(RoleName.SYS_ADMIN.getValue());

    when(userRepository.existsByEmail("newuser@example.com")).thenReturn(false);
    when(roleRepository.findByRoleName(RoleName.SYS_ADMIN.getValue())).thenReturn(Optional.of(role));
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
      return invocation.<User>getArgument(0);
    });
    when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");
    when(localCredentialRepository.save(any(LocalCredential.class))).thenReturn(null);
    when(jwtUtil.generateToken(any(User.class))).thenReturn("jwt.token.here");
    when(refreshTokenService.create(any(User.class))).thenReturn("refresh.token.here");

    AuthResponseDTO result = authService.register(dto);

    assertThat(result.token()).isEqualTo("jwt.token.here");
    assertThat(result.refreshToken()).isEqualTo("refresh.token.here");
  }

  @Test
  void register_throwsEmailAlreadyExistsException_whenDuplicate() {
    RegisterRequestDTO dto = sampleRegisterDTO();
    when(userRepository.existsByEmail("newuser@example.com")).thenReturn(true);

    assertThatThrownBy(() -> authService.register(dto))
        .isInstanceOf(EmailAlreadyExistsException.class);
  }

  @Test
  void register_throwsIllegalArgumentException_whenInvalidRole() {
    RegisterRequestDTO dto = sampleRegisterDTO();
    dto.setRole("invalid_role");

    when(userRepository.existsByEmail("newuser@example.com")).thenReturn(false);

    assertThatThrownBy(() -> authService.register(dto))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining(RoleName.SYS_ADMIN.getValue());
  }

  @Test
  void register_throwsEntityNotFoundException_whenRoleNotInDB() {
    RegisterRequestDTO dto = sampleRegisterDTO();

    when(userRepository.existsByEmail("newuser@example.com")).thenReturn(false);
    when(roleRepository.findByRoleName(RoleName.SYS_ADMIN.getValue())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> authService.register(dto))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessageContaining("role not found");
  }

  @Test
  void register_assignsCorrectRole() {
    RegisterRequestDTO dto = sampleRegisterDTO();
    dto.setRole(RoleName.INST_ADMIN.getValue());
    Role role = sampleRole(RoleName.INST_ADMIN.getValue());

    when(userRepository.existsByEmail("newuser@example.com")).thenReturn(false);
    when(roleRepository.findByRoleName(RoleName.INST_ADMIN.getValue())).thenReturn(Optional.of(role));
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
      User saved = invocation.getArgument(0);
      return saved;
    });
    when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");
    when(localCredentialRepository.save(any(LocalCredential.class))).thenReturn(null);
    when(jwtUtil.generateToken(any(User.class))).thenReturn("jwt.token.here");
    when(refreshTokenService.create(any(User.class))).thenReturn("refresh.token.here");

    AuthResponseDTO result = authService.register(dto);

    assertThat(result.roles()).contains(RoleName.INST_ADMIN.getValue());
  }

  @Test
  void login_returnsAuthResponse_onSuccess() {
    LoginRequestDTO dto = sampleLoginDTO();
    Role role = sampleRole(RoleName.SYS_ADMIN.getValue());
    User user = sampleUser(role);

    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
    when(jwtUtil.generateToken(any(User.class))).thenReturn("jwt.token.here");
    when(refreshTokenService.create(any(User.class))).thenReturn("refresh.token.here");

    AuthResponseDTO result = authService.login(dto);

    assertThat(result.token()).isEqualTo("jwt.token.here");
    assertThat(result.refreshToken()).isEqualTo("refresh.token.here");
    verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    verify(refreshTokenService).revokeAll(user);
  }

  @Test
  void login_throwsBadCredentialsException_whenInvalidPassword() {
    LoginRequestDTO dto = sampleLoginDTO();

    when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
        .thenThrow(new BadCredentialsException("Invalid email or password"));

    assertThatThrownBy(() -> authService.login(dto))
        .isInstanceOf(BadCredentialsException.class);
  }

  @Test
  void login_throwsUsernameNotFoundException_whenEmailNotFound() {
    LoginRequestDTO dto = sampleLoginDTO();

    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> authService.login(dto))
        .isInstanceOf(UsernameNotFoundException.class)
        .hasMessageContaining("test@example.com");
  }
}