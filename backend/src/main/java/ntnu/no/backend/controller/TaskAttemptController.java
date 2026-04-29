package ntnu.no.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ntnu.no.backend.dto.task.TaskAttemptRequestDTO;
import ntnu.no.backend.dto.task.TaskAttemptResponseDTO;
import ntnu.no.backend.model.auth.User;
import ntnu.no.backend.service.TaskAttemptService;
import ntnu.no.backend.shared.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Endpoints for task attempt management.
 *
 * <p>Attempt submission delegates to {@code sp_submit_attempt} via
 * {@link TaskAttemptService}. The database trigger {@code trg_attempt_sync}
 * handles best-score tracking and XP awards — no trigger logic is
 * replicated here.</p>
 *
 * @author aninesst
 * @version 0.1
 * @since 0.1
 * @see TaskAttemptService
 */
@RestController
@RequestMapping("/api/v1/attempts")
@RequiredArgsConstructor
@Tag(name = "Task Attempts", description = "Endpoints for submitting and querying task attempts")
public class TaskAttemptController {

  private final TaskAttemptService taskAttemptService;

  /**
   * Submits a new task attempt for the authenticated user.
   *
   * <p>The attempt is recorded via the {@code sp_submit_attempt} stored procedure,
   * which validates that game mode is enabled for the classroom. The database
   * trigger then updates best scores and awards XP automatically.</p>
   *
   * @param user the authenticated user
   * @param dto  the attempt request containing taskId, classroomId, and score
   * @return {@code 200 OK} with the recorded attempt
   */
  @Operation(summary = "Submit a task attempt")
  @PreAuthorize("isAuthenticated()")
  @PostMapping
  public ResponseEntity<ApiResponse<TaskAttemptResponseDTO>> submitAttempt(
      @AuthenticationPrincipal User user,
      @Valid @RequestBody TaskAttemptRequestDTO dto) {
    return ResponseEntity.ok(ApiResponse.ok(taskAttemptService.submitAttempt(user, dto)));
  }

  /**
   * Retrieves all attempts for a specific user and task combination.
   * Students can only view their own attempts. Instructors can only view attempts of students in their classroom.
   *
   * @param userId the user's unique identifier
   * @param taskId the task's unique identifier
   * @param user the authenticated user
   * @return {@code 200 OK} with the list of attempts
   */
  @Operation(summary = "Get attempts for a user/task combination")
  @PreAuthorize("isAuthenticated()")
  @GetMapping("/user/{userId}/task/{taskId}")
  public ResponseEntity<ApiResponse<List<TaskAttemptResponseDTO>>> getAttemptsByUserAndTask(
      @PathVariable UUID userId,
      @PathVariable UUID taskId,
      @AuthenticationPrincipal User user) {
    return ResponseEntity.ok(ApiResponse.ok(taskAttemptService.getAttemptsByUserAndTask(userId, taskId, user)));
  }

  /**
   * Returns the IDs of every task the authenticated user has passed at
   * least once. Backed by {@code task_best_score.first_passed_at IS NOT NULL}.
   *
   * @param user the authenticated user
   * @return {@code 200 OK} with the list of passed task IDs
   */
  @Operation(summary = "List task IDs the authenticated user has passed")
  @PreAuthorize("isAuthenticated()")
  @GetMapping("/me/passed")
  public ResponseEntity<ApiResponse<List<UUID>>> getMyPassedTaskIds(
      @AuthenticationPrincipal User user) {
    return ResponseEntity.ok(ApiResponse.ok(taskAttemptService.getMyPassedTaskIds(user)));
  }

  /**
   * Retrieves all attempts within a classroom.
   * Only instructors of the classroom can access this endpoint.
   *
   * @param classroomId the classroom's unique identifier
   * @param user the authenticated instructor
   * @return {@code 200 OK} with the list of attempts
   */
  @Operation(summary = "Get all attempts in a classroom")
  @PreAuthorize("hasAuthority('instructor')")
  @GetMapping("/classroom/{classroomId}")
  public ResponseEntity<ApiResponse<List<TaskAttemptResponseDTO>>> getAttemptsByClassroom(
      @PathVariable UUID classroomId,
      @AuthenticationPrincipal User user) {
    return ResponseEntity.ok(ApiResponse.ok(taskAttemptService.getAttemptsByClassroom(classroomId, user)));
  }
}