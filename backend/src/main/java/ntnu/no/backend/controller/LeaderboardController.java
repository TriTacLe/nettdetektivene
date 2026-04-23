package ntnu.no.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import ntnu.no.backend.dto.task.ClassroomLeaderboardEntryResponseDTO;
import ntnu.no.backend.dto.task.LeaderboardEntryResponseDTO;
import ntnu.no.backend.dto.task.TaskBestScoreResponseDTO;
import ntnu.no.backend.service.LeaderboardService;
import ntnu.no.backend.shared.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for leaderboard endpoints.
 * All data is read-only; scores are maintained by the {@code trg_attempt_sync} trigger.
 *
 * @author tri, aninesst
 * @version 0.2
 * @since 0.1
 */
@RestController
@RequestMapping("/api/v1/classrooms")
@RequiredArgsConstructor
@Tag(name = "Leaderboard", description = "Endpoints for classroom leaderboards and student scores")
public class LeaderboardController {
  private final LeaderboardService leaderboardService;

  @GetMapping("/leaderboard")
  @Operation(summary = "Get all classrooms ranked by total score")
  public ResponseEntity<ApiResponse<List<ClassroomLeaderboardEntryResponseDTO>>> getClassroomsLeaderboard(){
    return ResponseEntity.ok(ApiResponse.ok(leaderboardService.getClassroomsLeaderboard()));
  }

  /**
   * Returns all students in a classroom ranked by their total score.
   *
   * @param classroomId the classroom to fetch the leaderboard for
   * @return 200 with ranked list of {@link LeaderboardEntryResponseDTO}
   */
  @GetMapping("/{classroomId}/leaderboard")
  @Operation(summary = "Get classroom leaderboard ranked by total score")
  public ResponseEntity<ApiResponse<List<LeaderboardEntryResponseDTO>>> getClassroomLeaderboard(@PathVariable UUID classroomId){
    return ResponseEntity.ok(ApiResponse.ok(leaderboardService.getClassroomLeaderboard(classroomId)));
  }

  /**
   * Returns a specific student's task-by-task best scores within a classroom.
   *
   * @param classroomId the classroom to look up scores in
   * @param userId      the student whose scores to retrieve
   * @return 200 with list of {@link TaskBestScoreResponseDTO}
   */
  @GetMapping("/{classroomId}/leaderboard/{userId}")
  @Operation(summary = "Get a student's best scores per task within a classroom")
  public ResponseEntity<ApiResponse<List<TaskBestScoreResponseDTO>>> getStudentScore(@PathVariable UUID classroomId, @PathVariable UUID userId){
    return ResponseEntity.ok(ApiResponse.ok(leaderboardService.getStudentScore(classroomId, userId)));
  }

  @GetMapping("/{classroomId}/leaderboard/{userId}/passed-count")
  @Operation(summary = "Get how many tasks a student has passed in a stop location")
  public ResponseEntity<ApiResponse<Long>> countPassedTasksByStopLocation(
      @PathVariable UUID classroomId,
      @PathVariable UUID userId,
      @RequestParam String stopLocation) {
    return ResponseEntity.ok(ApiResponse.ok(
        leaderboardService.countPassedTasksByStopLocation(classroomId, userId, stopLocation)
    ));
  }
}
