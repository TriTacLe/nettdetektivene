package ntnu.no.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ntnu.no.backend.dto.auth.AuthResponseDTO;
import ntnu.no.backend.dto.auth.LoginRequestDTO;
import ntnu.no.backend.dto.auth.RefreshRequestDTO;
import ntnu.no.backend.dto.auth.RegisterRequestDTO;
import ntnu.no.backend.service.AuthService;
import ntnu.no.backend.service.RefreshTokenService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author norakirknes
 * @version 0.1
 * @since 0.1
 * @see AuthService
 */
@RestController
@RequestMapping("api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Endpoints for registration and login")
public class AuthController {
  private final AuthService authService;
  private final RefreshTokenService refreshTokenService;

  /**
   *
   * @param dto
   * @return
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
   *
   * @param dto
   * @return
   */
  @Operation(summary = "Login with email and password")
  @ApiResponse(responseCode = "200", description = "Login successful")
  @ApiResponse(responseCode = "401", description = "Invalid email or password")
  @ApiResponse(responseCode = "400", description = "Invalid input")
  @PostMapping("/login")
  public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO dto) {
    return ResponseEntity.ok(authService.login(dto));
  }

  @Operation(summary = "Refresh access token using a valid refresh token")
  @ApiResponse(responseCode = "200", description = "Token refreshed successfully")
  @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token")
  @ApiResponse(responseCode = "400", description = "Invalid input")
  @PostMapping("/refresh")
  public ResponseEntity<AuthResponseDTO> refresh(@Valid @RequestBody RefreshRequestDTO dto) {
    return ResponseEntity.ok(refreshTokenService.refresh(dto.getRefreshToken()));
  }

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
