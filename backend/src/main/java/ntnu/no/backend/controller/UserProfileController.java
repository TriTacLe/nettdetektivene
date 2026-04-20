package ntnu.no.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ntnu.no.backend.dto.profile.UserProfileResponseDTO;
import ntnu.no.backend.dto.profile.UserProfileUpdateRequestDTO;
import ntnu.no.backend.model.auth.User;
import ntnu.no.backend.service.UserProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints for user profile management.
 *
 * @author norakirknes
 * @version 0.1
 * @since 0.1
 * @see UserProfileService
 */
@RestController
@RequestMapping("/api/v1/profiles")
@RequiredArgsConstructor
@Tag(name = "Profiles", description = "Endpoints for user profile management")
public class UserProfileController {
  private final UserProfileService userProfileService;

  /**
   * Retrieves the profile of the authenticated user.
   *
   * @param user the authenticated user
   * @return {@code 200 OK} with the user's profile
   */
  @Operation(summary = "Get the authenticated user's profile")
  @ApiResponse(responseCode = "200", description = "Profile retrieved successfully")
  @ApiResponse(responseCode = "401", description = "Not authenticated")
  @ApiResponse(responseCode = "403", description = "Not authorized")
  @ApiResponse(responseCode = "404", description = "Profile not found")
  @PreAuthorize("isAuthenticated()")
  @GetMapping("/me")
  public ResponseEntity<UserProfileResponseDTO> getProfile(@AuthenticationPrincipal User user) {
    return ResponseEntity.ok(userProfileService.getByUser(user));
  }

  /**
   * Updates the profile of the authenticated user.
   *
   * @param user the authenticated user
   * @param dto  the update request containing new display name and avatar config
   * @return {@code 200 OK} with the updated profile
   */
  @Operation(summary = "Update the authenticated user's profile")
  @ApiResponse(responseCode = "200", description = "Profile updated successfully")
  @ApiResponse(responseCode = "400", description = "Invalid input")
  @ApiResponse(responseCode = "401", description = "Not authenticated")
  @ApiResponse(responseCode = "403", description = "Not authorized")
  @ApiResponse(responseCode = "404", description = "Profile not found")
  @PreAuthorize("isAuthenticated()")
  @PatchMapping("/me")
  public ResponseEntity<UserProfileResponseDTO> updateProfile(@AuthenticationPrincipal User user,
                                                              @Valid @RequestBody UserProfileUpdateRequestDTO dto) {
    return ResponseEntity.ok(userProfileService.update(user, dto));
  }

  /**
   * Retrieves a user profile by FEIDE identifier.
   * Used to view another user's profile from a leaderboard or classroom context.
   *
   * @param feideId the FEIDE identifier of the user
   * @return {@code 200 OK} with the user's profile
   */
  @Operation(summary = "Get a user profile by FEIDE identifier")
  @ApiResponse(responseCode = "200", description = "Profile retrieved successfully")
  @ApiResponse(responseCode = "401", description = "Not authenticated")
  @ApiResponse(responseCode = "404", description = "Profile not found")
  @PreAuthorize("isAuthenticated()")
  @GetMapping("/{feideId}")
  public ResponseEntity<UserProfileResponseDTO> getProfileByFeideId(@PathVariable String feideId) {
    return ResponseEntity.ok(userProfileService.getByFeideId(feideId));
  }
}
