package ntnu.no.backend.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import ntnu.no.backend.dto.auth.AuthResponseDTO;
import ntnu.no.backend.dto.auth.LoginRequestDTO;
import ntnu.no.backend.dto.auth.RegisterRequestDTO;
import ntnu.no.backend.mapper.UserMapper;
import ntnu.no.backend.model.auth.LocalCredential;
import ntnu.no.backend.model.auth.Role;
import ntnu.no.backend.model.auth.User;
import ntnu.no.backend.repository.LocalCredentialRepository;
import ntnu.no.backend.repository.RoleRepository;
import ntnu.no.backend.repository.UserRepository;
import ntnu.no.backend.exception.EmailAlreadyExistsException;
import ntnu.no.backend.model.enums.RoleName;
import ntnu.no.backend.security.JWTUtil;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service handling user registration and login.
 *
 * @author norakirknes
 * @version 0.1
 * @since 0.1
 */
@Service
@RequiredArgsConstructor
public class AuthService {

  private final UserRepository userRepository;
  private final LocalCredentialRepository localCredentialRepository;
  private final PasswordEncoder passwordEncoder;
  private final AuthenticationManager authenticationManager;
  private final JWTUtil jwtUtil;
  private final RoleRepository roleRepository;
  private final RefreshTokenService refreshTokenService;


  /**
   * Registers a new {@code sys_admin} or {@code inst_admin} user with local credentials.
   *
   * @param dto the registration request containing display name, email, password and role
   * @return an {@link AuthResponseDTO} containing a JWT, refresh token and roles
   */
  @Transactional
  public AuthResponseDTO register(RegisterRequestDTO dto) {
    String email = dto.getEmail().toLowerCase();
    if (userRepository.existsByEmail(email)) {
      throw new EmailAlreadyExistsException(email);
    }
    if (dto.getDisplayName() == null || dto.getDisplayName().isBlank()) {
      dto.setDisplayName(email.split("@")[0]);
    }
    Role role = resolveAdminRole(dto.getRole());
    User user = createUser(dto, email, role);
    createLocalCredential(user, dto.getPassword());
    return new AuthResponseDTO(
      jwtUtil.generateToken(user),
      refreshTokenService.create(user),
      user.getRoles().stream().map(Role::getAuthority).toList()
    );
  }

  /**
   * Validates that the role is {@code sys_admin} or {@code inst_admin} and returns
   * the {@link Role} entity.
   *
   * @param roleName the role name to validate and look up
   * @return the matching {@link Role} entity
   */
  private Role resolveAdminRole(String roleName) {
    if (!RoleName.SYS_ADMIN.getValue().equals(roleName) && !RoleName.INST_ADMIN.getValue().equals(roleName)) {
      throw new IllegalArgumentException("Role must be " + RoleName.SYS_ADMIN.getValue() + " or " + RoleName.INST_ADMIN.getValue());
    }
    return roleRepository.findByRoleName(roleName)
      .orElseThrow(() -> new EntityNotFoundException(roleName + " role not found"));
  }

  /**
   * Maps the DTO to a {@link User} entity, assigns the role and persists it.
   *
   * @param dto   the registration request
   * @param email the normalised email address
   * @param role  the resolved {@link Role} entity
   * @return the persisted {@link User}
   */
  private User createUser(RegisterRequestDTO dto, String email, Role role) {
    User user = UserMapper.toEntity(dto);
    user.setEmail(email);
    user.getRoles().add(role);
    return userRepository.save(user);
  }

  /**
   * Hashes and persists a {@link LocalCredential} for the given user.
   *
   * @param user     the provisioned user
   * @param password the raw password to hash and store
   */
  private void createLocalCredential(User user, String password) {
    localCredentialRepository.save(LocalCredential.builder()
      .user(user)
      .passwordHash(passwordEncoder.encode(password))
      .build());
  }

  /**
   * Authenticates a user with email and password via {@link AuthenticationManager}.
   *
   * @param dto the login request containing email and password
   * @return an {@link AuthResponseDTO} containing a JWT, refresh token and roles
   */
  @Transactional
  public AuthResponseDTO login(LoginRequestDTO dto) {
    String email = dto.getEmail().toLowerCase();
    authenticationManager.authenticate(
      new UsernamePasswordAuthenticationToken(email, dto.getPassword())
    );
    User user = userRepository.findByEmail(email)
      .orElseThrow(() -> new UsernameNotFoundException("No user found with email: " + email));
    refreshTokenService.revokeAll(user);

    return new AuthResponseDTO(
      jwtUtil.generateToken(user),
      refreshTokenService.create(user),
      user.getRoles().stream().map(Role::getAuthority).toList()
    );
  }
}
