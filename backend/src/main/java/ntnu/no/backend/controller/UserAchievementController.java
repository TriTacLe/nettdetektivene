package ntnu.no.backend.controller;

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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.UUID;

import java.util.List;

@RestController
@RequestMapping("/api/v1/achievements")
@RequiredArgsConstructor
@Tag(name = "User Achievements", description = "Endpoints for fetching achievements earned by the authenticated user")
public class UserAchievementController {
  private final UserAchievementService userAchievementService;

  @GetMapping("/me")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<ApiResponse<List<UserAchievementResponseDTO>>> getMyAchievements(
      @AuthenticationPrincipal User user) {
    return ResponseEntity.ok(ApiResponse.ok(userAchievementService.getAchievementsForUser(user.getId())));
  }

  @GetMapping("/user/{userId}")
  @PreAuthorize("hasAnyAuthority('instructor', 'inst_admin', 'sys_admin')")
  public ResponseEntity<ApiResponse<List<UserAchievementResponseDTO>>> getAchievementsForStudent(
      @PathVariable UUID userId) {
    return ResponseEntity.ok(ApiResponse.ok(userAchievementService.getAchievementsForUser(userId)));
  }

  @PostMapping("/secret/{triggerRef}")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<ApiResponse<UserAchievementResponseDTO>> awardSecret(
      @AuthenticationPrincipal User user,
      @PathVariable String triggerRef) {
    return ResponseEntity.ok(ApiResponse.ok(userAchievementService.awardSecret(user.getId(), triggerRef)));
  }
}
