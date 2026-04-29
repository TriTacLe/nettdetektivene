package ntnu.no.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ntnu.no.backend.dto.profile.UserProfileResponseDTO;
import ntnu.no.backend.dto.profile.UserProfileUpdateRequestDTO;
import ntnu.no.backend.model.auth.User;
import ntnu.no.backend.service.UserProfileService;
import ntnu.no.backend.shared.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

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
  @PreAuthorize("isAuthenticated()")
  @GetMapping("/me")
  public ResponseEntity<ApiResponse<UserProfileResponseDTO>> getProfile(@AuthenticationPrincipal User user) {
    return ResponseEntity.ok(ApiResponse.ok(userProfileService.getByUser(user)));
  }

  /**
   * Updates the profile of the authenticated user.
   *
   * @param user the authenticated user
   * @param dto  the update request containing new display name and avatar config
   * @return {@code 200 OK} with the updated profile
   */
  @Operation(summary = "Update the authenticated user's profile")
  @PreAuthorize("isAuthenticated()")
  @PatchMapping("/me")
  public ResponseEntity<ApiResponse<UserProfileResponseDTO>> updateProfile(@AuthenticationPrincipal User user,
                                                                           @Valid @RequestBody UserProfileUpdateRequestDTO dto) {
    return ResponseEntity.ok(ApiResponse.ok(userProfileService.update(user, dto)));
  }

  /**
   * Retrieves a user profile by FEIDE identifier.
   * Used to view another user's profile from a leaderboard or classroom context.
   *
   * @param feideId the FEIDE identifier of the user
   * @return {@code 200 OK} with the user's profile
   */
  @Operation(summary = "Get the authenticated user's story progression")
  @PreAuthorize("isAuthenticated()")
  @GetMapping("/me/story-progress")
  public ResponseEntity<ApiResponse<Map<String, Integer>>> getStoryProgress(@AuthenticationPrincipal User user) {
    int step = userProfileService.getStoryProgress(user);
    return ResponseEntity.ok(ApiResponse.ok(Map.of("step", step)));
  }

  @Operation(summary = "Advance the authenticated user's story progression")
  @PreAuthorize("isAuthenticated()")
  @PostMapping("/me/story-progress")
  public ResponseEntity<ApiResponse<Map<String, Integer>>> advanceStoryProgress(
      @AuthenticationPrincipal User user,
      @RequestBody Map<String, Integer> body) {
    Integer step = body == null ? null : body.get("step");
    if (step == null || step < 0) {
      return ResponseEntity.badRequest().body(ApiResponse.error("Invalid step"));
    }
    int updated = userProfileService.advanceStoryProgress(user, step);
    return ResponseEntity.ok(ApiResponse.ok(Map.of("step", updated)));
  }

  @Operation(summary = "Get a user profile by FEIDE identifier")
  @PreAuthorize("isAuthenticated()")
  @GetMapping("/{feideId}")
  public ResponseEntity<ApiResponse<UserProfileResponseDTO>> getProfileByFeideId(@PathVariable String feideId) {
    return ResponseEntity.ok(ApiResponse.ok(userProfileService.getByFeideId(feideId)));
  }
}
