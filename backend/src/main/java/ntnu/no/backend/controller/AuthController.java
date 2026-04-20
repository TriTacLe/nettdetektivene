package ntnu.no.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ntnu.no.backend.dto.auth.AuthResponseDTO;
import ntnu.no.backend.dto.auth.FeideAuthResponseDTO;
import ntnu.no.backend.dto.auth.FeideLoginRequestDTO;
import ntnu.no.backend.dto.auth.LoginRequestDTO;
import ntnu.no.backend.dto.auth.RefreshRequestDTO;
import ntnu.no.backend.dto.auth.RegisterRequestDTO;
import ntnu.no.backend.service.AuthService;
import ntnu.no.backend.service.FeideAuthService;
import ntnu.no.backend.service.RefreshTokenService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints for authentication management.
 *
 * @author norakirknes
 * @version 0.1
 * @since 0.1
 * @see AuthService
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Endpoints for registration and login")
public class AuthController {
  private final AuthService authService;
  private final FeideAuthService feideAuthService;
  private final RefreshTokenService refreshTokenService;

  /**
   * Simulated FEIDE login. Creates a new user, identity and profile on first login.
   * On later logins, the feideId is extracted from the existing JWT by
   * auth filter, and passed in as a request attribute.
   *
   * @param dto the FEIDE login request containing displayName, email and affiliation
   * @param feideId the feideId extracted from the JWT by filter, or {@code null} on first login
   * @return {@code 200 OK} with a JWT, refresh token, roles and feideId
   */
  @Operation(summary = "Login via simulated FEIDE")
  @ApiResponse(responseCode = "200", description = "Login successful")
  @ApiResponse(responseCode = "400", description = "Invalid input")
  @PostMapping("/feide")
  public ResponseEntity<FeideAuthResponseDTO> feideLogin(
      @Valid @RequestBody FeideLoginRequestDTO dto,
      @RequestAttribute(required = false) String feideId) {
    return ResponseEntity.ok(feideAuthService.login(dto, feideId));
  }

  /**
   * Dev/test registration endpoint. Creates a user with local credentials via Swagger.
   * todo note; for testing
   *
   * @param dto the registration request containing display name, email and password
   * @return {@code 201 Created} with a JWT, refresh token and roles
   */
  @Operation(summary = "Register a new user")
  @ApiResponse(responseCode = "201", description = "User registered successfully")
  @ApiResponse(responseCode = "409", description = "Email already in use")
  @ApiResponse(responseCode = "400", description = "Invalid input")
  @PostMapping("/register")
  public ResponseEntity<AuthResponseDTO> register(@Valid @RequestBody RegisterRequestDTO dto) {
    return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(dto));
  }

  /**
   * Login with email and password. Intended for sys_admin and inst_admin accounts.
   *
   * @param dto the login request containing email and password
   * @return {@code 200 OK} with a JWT, refresh token and roles
   */
  @Operation(summary = "Login with email and password")
  @ApiResponse(responseCode = "200", description = "Login successful")
  @ApiResponse(responseCode = "401", description = "Invalid email or password")
  @ApiResponse(responseCode = "400", description = "Invalid input")
  @PostMapping("/login")
  public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO dto) {
    return ResponseEntity.ok(authService.login(dto));
  }

  /**
   * Rotates the session by revoking the current refresh token and issuing a new
   * refresh token and access token pair. For simulated FEIDE users, the expired JWT is used
   * to forward the {@code feideId} claim to the new access token.
   *
   * @param dto the request containing the current refresh token and, optionally, the expired JWT
   * @return {@code 200 OK} with a new JWT, refresh token and roles
   */
  @Operation(summary = "Rotate refresh token and issue new access token")
  @ApiResponse(responseCode = "200", description = "Token refreshed successfully")
  @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token")
  @ApiResponse(responseCode = "400", description = "Invalid input")
  @PostMapping("/refresh")
  public ResponseEntity<AuthResponseDTO> refresh(@Valid @RequestBody RefreshRequestDTO dto) {
    return ResponseEntity.ok(refreshTokenService.refresh(dto.getRefreshToken(), dto.getExpiredToken()));
  }

  /**
   * Revokes the refresh token on logout. The access token (JWT) remains valid until
   * it expires naturally, a true session invalidation would require a token blocklist
   * backed by a {@code revoked_token} table. Accepted limitation for this project as of now.
   * TODO: possibly implement JWT blocklist or a new table
   *
   * @param dto the request containing the refresh token to be revoked
   * @return an empty response entity
   */
  @Operation(summary = "Logout by revoking the refresh token")
  @ApiResponse(responseCode = "204", description = "Logged out successfully")
  @ApiResponse(responseCode = "401", description = "Invalid refresh token")
  @ApiResponse(responseCode = "400", description = "Invalid input")
  @PostMapping("/logout")
  public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequestDTO dto) {
    refreshTokenService.revoke(dto.getRefreshToken());
    return ResponseEntity.noContent().build();
  }
}
