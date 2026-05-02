package ntnu.no.backend.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import ntnu.no.backend.dto.auth.FeideAuthResponseDTO;
import ntnu.no.backend.dto.auth.FeideLoginRequestDTO;
import ntnu.no.backend.exception.EmailAlreadyExistsException;
import ntnu.no.backend.mapper.UserMapper;
import ntnu.no.backend.mapper.UserProfileMapper;
import ntnu.no.backend.model.auth.AppUserIdentity;
import ntnu.no.backend.model.auth.Role;
import ntnu.no.backend.model.auth.User;
import ntnu.no.backend.model.auth.UserProfile;
import ntnu.no.backend.model.enums.RoleName;
import ntnu.no.backend.model.auth.LocalCredential;
import ntnu.no.backend.repository.AppUserIdentityRepository;
import ntnu.no.backend.repository.LocalCredentialRepository;
import ntnu.no.backend.repository.RoleRepository;
import ntnu.no.backend.repository.UserProfileRepository;
import ntnu.no.backend.repository.UserRepository;
import ntnu.no.backend.security.JWTUtil;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Handles simulated FEIDE login.
 * First login provisions a {@link User}, {@link AppUserIdentity} and {@link UserProfile} (for students),
 * embedding the generated {@code feideId} in the issued JWT.
 * Later logins resolve the user via {@code feideId} extracted from the existing JWT by the filter.
 *
 * @author norakirknes
 * @version 0.1
 * @since 0.1
 */
@Service
@RequiredArgsConstructor
public class FeideAuthService {

  private static final String PROVIDER_NAME = "feide";

  private final AppUserIdentityRepository identityRepository;
  private final UserRepository userRepository;
  private final UserProfileRepository userProfileRepository;
  private final RoleRepository roleRepository;
  private final LocalCredentialRepository localCredentialRepository;
  private final PasswordEncoder passwordEncoder;
  private final AuthenticationManager authenticationManager;
  private final RefreshTokenService refreshTokenService;
  private final JWTUtil jwtUtil;

  /**
   * Authenticates a user via simulated FEIDE login.
   * On later logins where {@code feideId} present, the password is verified via
   * {@link AuthenticationManager} before resolving the identity.
   *
   * @param dto the FEIDE login request containing displayName, email, affiliation and password
   * @param feideId the feideId extracted from the existing JWT by the filter
   * @return a {@link FeideAuthResponseDTO} containing a JWT, refresh token, role and feideId
   */
  @Transactional
  public FeideAuthResponseDTO login(FeideLoginRequestDTO dto, String feideId) {
    String email = dto.getEmail().toLowerCase();
    if (feideId != null) {
      authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, dto.getPassword()));
      AppUserIdentity identity = identityRepository
        .findByProviderNameAndProviderUserId(PROVIDER_NAME, feideId)
        .orElseThrow(() -> new EntityNotFoundException("No FEIDE identity found for id: " + feideId));
      return issueTokens(identity.getUser(), identity.getProviderUserId());
    }
    if (userRepository.existsByEmail(email)) {
      authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, dto.getPassword()));
      User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new EntityNotFoundException("No user found with email: " + email));
      AppUserIdentity identity = identityRepository
        .findByProviderNameAndUser(PROVIDER_NAME, user)
        .orElseThrow(() -> new EntityNotFoundException("No FEIDE identity found for user: " + email));
      return issueTokens(identity.getUser(), identity.getProviderUserId());
    }
    return firstLogin(dto, email);
  }

  /**
   * Revokes existing refresh tokens and issues a new JWT and refresh token.
   *
   * @param user the authenticated user
   * @param feideId the FEIDE identifier to embed in the JWT
   * @return a {@link FeideAuthResponseDTO} with JWT, refresh token, roles and feideId
   */
  private FeideAuthResponseDTO issueTokens(User user, String feideId) {
    refreshTokenService.revokeAll(user);
    return new FeideAuthResponseDTO(
      jwtUtil.generateToken(user, feideId),
      refreshTokenService.create(user),
      user.getRoles().stream().map(Role::getAuthority).toList(),
      feideId
    );
  }

  /**
   * Provisions a new user, identity and (for students) profile, then issues tokens.
   *
   * this method makes it possible to login for the first time
   * with any given email and password, as long as it has not been used before
   * mirroring first login with FEIDE. In practice the first login is a registration.
   *
   *
   * @param dto the FEIDE login request
   * @param email the email address
   * @return a {@link FeideAuthResponseDTO} with JWT, refresh token, role and feideId
   */
  private FeideAuthResponseDTO firstLogin(FeideLoginRequestDTO dto, String email) {
    if (userRepository.existsByEmail(email)) {
      throw new EmailAlreadyExistsException(email);
    }
    if (dto.getDisplayName() == null || dto.getDisplayName().isBlank()) {
      dto.setDisplayName(email.split("@")[0]);
    }
    String roleName = resolveRole(dto.getAffiliation());
    User user = createUser(dto, roleName);
    createLocalCredential(user, dto.getPassword());

    //simulated user id from feide, generated on first login
    String generatedFeideId = UUID.randomUUID().toString();

    createIdentity(user, generatedFeideId, email);
    createProfile(user, dto, generatedFeideId);
    return issueTokens(user, generatedFeideId);
  }

  /**
   * Looks up the role, maps the DTO to a {@link User} entity and persists it.
   *
   * @param dto the FEIDE login request
   * @param roleName the resolved role name
   * @return the persisted {@link User}
   */
  private User createUser(FeideLoginRequestDTO dto, String roleName) {
    Role role = roleRepository.findByRoleName(roleName)
      .orElseThrow(() -> new EntityNotFoundException(roleName + " role not found"));
    User user = UserMapper.toEntity(dto);
    user.getRoles().add(role);
    return userRepository.save(user);
  }

  /**
   * Hashes and persists a {@link LocalCredential} for the given user.
   *
   * @param user the provisioned user
   * @param password the raw password to hash and store
   */
  private void createLocalCredential(User user, String password) {
    localCredentialRepository.save(LocalCredential.builder()
      .user(user)
      .passwordHash(passwordEncoder.encode(password))
      .build());
  }

  /**
   * Creates and persists an {@link AppUserIdentity} linking the user to the FEIDE provider.
   *
   * @param user the authenticated user
   * @param feideId the generated FEIDE identifier
   * @param email the provider email
   */
  private void createIdentity(User user, String feideId, String email) {
    identityRepository.save(AppUserIdentity.builder()
      .user(user)
      .providerName(PROVIDER_NAME)
      .providerUserId(feideId)
      .providerEmail(email)
      .build());
  }

  /**
   * Creates and persists a {@link UserProfile} for the user, setting the feideId for public lookup.
   *
   * @param user the authenticated user
   * @param dto the FEIDE login request
   * @param feideId the generated FEIDE identifier
   */
  private void createProfile(User user, FeideLoginRequestDTO dto, String feideId) {
    UserProfile profile = UserProfileMapper.toEntity(user, dto);
    profile.setFeideId(feideId);
    userProfileRepository.save(profile);
  }

  /**
   * Maps the simulated {@code eduPersonAffiliation} claim to an internal role name.
   * {@code student} maps to {@link RoleName#STUDENT}, {@code instructor} maps to {@link RoleName#INSTRUCTOR}.
   *
   * @param affiliation the affiliation claim from the request
   * @return the internal role name
   */
  private String resolveRole(String affiliation) {
    return switch (affiliation) {
      case "student" -> RoleName.STUDENT.getValue();
      case "instructor" -> RoleName.INSTRUCTOR.getValue();
      default -> throw new IllegalArgumentException("Unknown affiliation: " + affiliation);
    };
  }
}
