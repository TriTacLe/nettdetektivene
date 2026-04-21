package ntnu.no.backend.controller;

import lombok.RequiredArgsConstructor;
import ntnu.no.backend.dto.task.LeaderboardEntryResponseDTO;
import ntnu.no.backend.dto.task.TaskBestScoreResponseDTO;
import ntnu.no.backend.service.LeaderboardService;
import ntnu.no.backend.shared.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for leaderboard endpoints.
 * All data is read-only; scores are maintained by the {@code trg_attempt_sync} trigger.
 *
 * @author tri
 * @version 0.1
 * @since 0.1
 */
@RestController
@RequestMapping("/api/v1/classrooms")
@RequiredArgsConstructor
public class LeaderboardController {
  private final LeaderboardService leaderboardService;

  /**
   * Returns all students in a classroom ranked by their total score.
   *
   * @param classroomId the classroom to fetch the leaderboard for
   * @return 200 with ranked list of {@link LeaderboardEntryResponseDTO}
   */
  @GetMapping("/{classroomId}/leaderboard")
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
  public ResponseEntity<ApiResponse<List<TaskBestScoreResponseDTO>>> getStudentScore(@PathVariable UUID classroomId, @PathVariable UUID userId){
    return ResponseEntity.ok(ApiResponse.ok(leaderboardService.getStudentScore(classroomId, userId)));
  }
}
