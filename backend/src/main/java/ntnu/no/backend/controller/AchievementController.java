package ntnu.no.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import ntnu.no.backend.dto.achievement.AchievementResponseDTO;
import ntnu.no.backend.service.AchievementService;
import ntnu.no.backend.shared.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for achievement endpoints.
 *
 * @author tri
 * @version 0.1
 * @since 0.1
 */
@RestController
@RequestMapping("/api/v1/achievements")
@RequiredArgsConstructor
@Tag(name = "Achievements", description = "Endpoints for achievements")
public class AchievementController {

  private final AchievementService achievementService;

  /**
   * Returns all achievements.
   *
   * @return 200 OK with list of achievements
   */
  @GetMapping
  @Operation(summary = "Get all achievements")
  public ResponseEntity<ApiResponse<List<AchievementResponseDTO>>> getAllAchievements() {
    return ResponseEntity.ok(ApiResponse.ok(achievementService.getAllAchievement()));
  }

  /**
   * Returns a single achievement by ID.
   *
   * @param id the achievement ID
   * @return 200 OK with the achievement
   */
  @GetMapping("/{id}")
  @Operation(summary = "Get achievement by ID")
  public ResponseEntity<ApiResponse<AchievementResponseDTO>> getAchievement(@PathVariable UUID id) {
    return ResponseEntity.ok(ApiResponse.ok(achievementService.getAchievement(id)));
  }

  /**
   * Returns all achievements matching a trigger type.
   *
   * @param triggerType the trigger type to filter by
   * @return 200 OK with list of matching achievements
   */
  @GetMapping("/by-trigger")
  @Operation(summary = "Get achievements by trigger type")
  public ResponseEntity<ApiResponse<List<AchievementResponseDTO>>> getAchievementsByTrigger(
      @RequestParam String triggerType) {
    return ResponseEntity.ok(ApiResponse.ok(achievementService.getAchievementsByTrigger(triggerType)));
  }
} 
