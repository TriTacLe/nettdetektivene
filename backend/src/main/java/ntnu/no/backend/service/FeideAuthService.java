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
import ntnu.no.backend.repository.*;
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
   * todo IMPORTANT NOTE:
   * this method makes it possible to login for the first time
   * with any given email and password, as long as it has not been used before
   * mirroring first login with FEIDE. Should only be used for testing purposes,
   * since there is no way of vouching credentials for new users without a real
   * OAuth handshake. Later on the whole of first login should be removed(?), and
   * feide id should be seeded in test data as a way of vouching feide users supporting
   * only login for predefined "feide" users.
   * All the following helper methods is only called by this method. Work in progress as to
   * how best to handle possible first logins, if handled at all, if not all the code from here
   * on out can be safely removed and the {@link #issueTokens(User, String)} can be pulled
   * out of helper and be implemented directly in {@link #login(FeideLoginRequestDTO, String)}
   * :)
   *
   * @param dto the FEIDE login request
   * @param email the email address
   * @return a {@link FeideAuthResponseDTO} with JWT, refresh token, role and feideId
   */
  private FeideAuthResponseDTO firstLogin(FeideLoginRequestDTO dto, String email) {
    if (userRepository.existsByEmail(email)) {
      throw new EmailAlreadyExistsException(email);
    }
    String roleName = resolveRole(dto.getAffiliation());
    User user = createUser(dto, roleName);
    createLocalCredential(user, dto.getPassword());

    //simulated user id from feide, generated on first login
    String generatedFeideId = UUID.randomUUID().toString();

    createIdentity(user, generatedFeideId, email);
    if (RoleName.STUDENT.getValue().equals(roleName)) {
      createStudentProfile(user, dto, generatedFeideId);
    }
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
   * Creates and persists a {@link UserProfile} for a student, setting the feideId for public lookup.
   *
   * @param user the authenticated student
   * @param dto the FEIDE login request
   * @param feideId the generated FEIDE identifier
   */
  private void createStudentProfile(User user, FeideLoginRequestDTO dto, String feideId) {
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
