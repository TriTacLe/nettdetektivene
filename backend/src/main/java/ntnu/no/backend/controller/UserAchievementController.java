package ntnu.no.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import ntnu.no.backend.dto.achievement.UserAchievementResponseDTO;
import ntnu.no.backend.model.auth.User;
import ntnu.no.backend.service.UserAchievementService;
import ntnu.no.backend.shared.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for user achievement endpoints.
 *
 * @author tri
 * @version 0.1
 * @since 0.1
 */
@RestController
@RequestMapping("/api/v1/achievements")
@RequiredArgsConstructor
@Tag(name = "User Achievements", description = "Endpoints for fetching achievements earned by the authenticated user")
public class UserAchievementController {
  private final UserAchievementService userAchievementService;

  /**
   * Returns all achievements earned by the authenticated user.
   *
   * @param user the authenticated user
   * @return 200 OK with list of {@link UserAchievementResponseDTO}
   */
  @GetMapping("/me")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Get achievements earned by the authenticated user")
  public ResponseEntity<ApiResponse<List<UserAchievementResponseDTO>>> getMyAchievements(@AuthenticationPrincipal User user){
    return ResponseEntity.ok(ApiResponse.ok(userAchievementService.getAchivementsForUser(user.getId())));
  }
}
