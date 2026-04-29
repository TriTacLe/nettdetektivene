package ntnu.no.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ntnu.no.backend.dto.mystery.MysteryReviewDTO;
import ntnu.no.backend.dto.mystery.MysterySubmissionRequestDTO;
import ntnu.no.backend.dto.mystery.MysterySubmissionResponseDTO;
import ntnu.no.backend.dto.mystery.MysteryTaskCreateRequestDTO;
import ntnu.no.backend.dto.task.ClassroomTaskResponseDTO;
import ntnu.no.backend.model.auth.User;
import ntnu.no.backend.service.MysteryService;
import ntnu.no.backend.shared.ApiResponse;
import org.springframework.http.HttpStatus;
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

import java.util.List;
import java.util.UUID;

/**
 * Endpoints for mystery submission management.
 *
 * @author aninesst
 * @version 0.2
 * @since 0.1
 */
@Tag(name = "Mysteries", description = "Endpoints for mystery submissions")
@RestController
@RequestMapping("/api/v1/mysteries")
@RequiredArgsConstructor
public class MysteryController {
  private final MysteryService mysteryService;

  /**
   * Submits a mystery as the authenticated student.
   *
   * @param dto the submission request
   * @param user the authenticated student
   * @return the created submission
   */
  @Operation(summary = "Submit a mystery")
  @PostMapping
  @PreAuthorize("hasAuthority('student')")
  public ResponseEntity<ApiResponse<MysterySubmissionResponseDTO>> submitMystery(
      @RequestBody @Valid MysterySubmissionRequestDTO dto,
      @AuthenticationPrincipal User user) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.ok(mysteryService.submitMystery(dto, user.getId())));
  }

  /**
   * Returns all submissions in a classroom. Restricted to instructors.
   *
   * @param classroomId the classroom id
   * @return submissions in the classroom
   */
  @Operation(summary = "Get mystery submissions by classroom")
  @GetMapping("/classroom/{classroomId}")
  @PreAuthorize("hasAuthority('instructor')")
  public ResponseEntity<ApiResponse<List<MysterySubmissionResponseDTO>>> getSubmissionsByClassroom(
      @PathVariable UUID classroomId) {
    return ResponseEntity.ok(ApiResponse.ok(mysteryService.getSubmissionsByClassroom(classroomId)));
  }

  /**
   * Returns the authenticated student's own submissions in a classroom.
   *
   * @param classroomId the classroom id
   * @param user the authenticated student
   * @return submissions made by the authenticated student
   */
  @Operation(summary = "Get my mystery submissions by classroom")
  @GetMapping("/classroom/{classroomId}/mine")
  @PreAuthorize("hasAuthority('student')")
  public ResponseEntity<ApiResponse<List<MysterySubmissionResponseDTO>>> getMySubmissions(
      @PathVariable UUID classroomId,
      @AuthenticationPrincipal User user) {
    return ResponseEntity.ok(ApiResponse.ok(mysteryService.getMySubmissions(classroomId, user.getId())));
  }

  /**
   * Returns pending submissions in a classroom. Restricted to instructors.
   *
   * @param classroomId the classroom id
   * @return pending submissions in the classroom
   */
  @Operation(summary = "Get pending mystery submissions by classroom")
  @GetMapping("/classroom/{classroomId}/pending")
  @PreAuthorize("hasAuthority('instructor')")
  public ResponseEntity<ApiResponse<List<MysterySubmissionResponseDTO>>> getPendingSubmissions(
      @PathVariable UUID classroomId) {
    return ResponseEntity.ok(ApiResponse.ok(mysteryService.getPendingSubmissions(classroomId)));
  }

  /**
   * Reviews a submission as an instructor.
   *
   * @param submissionId the submission id
   * @param dto the review request
   * @param user the authenticated instructor
   * @return the reviewed submission
   */
  @Operation(summary = "Review a mystery submission")
  @PatchMapping("/{submissionId}/review")
  @PreAuthorize("hasAuthority('instructor')")
  public ResponseEntity<ApiResponse<MysterySubmissionResponseDTO>> reviewSubmission(
      @PathVariable UUID submissionId,
      @RequestBody @Valid MysteryReviewDTO dto,
      @AuthenticationPrincipal User user) {
    return ResponseEntity.ok(ApiResponse.ok(mysteryService.reviewSubmission(submissionId, dto, user.getId())));
  }

  /**
   * Creates and assigns a classroom task from a mystery submission.
   *
   * @param submissionId the submission id
   * @param dto task creation options
   * @param user the authenticated instructor
   * @return created classroom task
   */
  @Operation(summary = "Create a classroom task from a mystery submission")
  @PostMapping("/{submissionId}/task")
  @PreAuthorize("hasAuthority('instructor')")
  public ResponseEntity<ApiResponse<ClassroomTaskResponseDTO>> createTaskFromSubmission(
      @PathVariable UUID submissionId,
      @RequestBody @Valid MysteryTaskCreateRequestDTO dto,
      @AuthenticationPrincipal User user) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.ok(mysteryService.createTaskFromSubmission(submissionId, dto, user.getId())));
  }

  /**
   * Returns the featured submission in a classroom.
   *
   * @param classroomId the classroom id
   * @return featured submission
   */
  @Operation(summary = "Get featured mystery submission by classroom")
  @GetMapping("/classroom/{classroomId}/featured")
  @PreAuthorize("hasAnyAuthority('student', 'instructor')")
  public ResponseEntity<ApiResponse<MysterySubmissionResponseDTO>> getFeaturedSubmission(
      @PathVariable UUID classroomId) {
    return ResponseEntity.ok(ApiResponse.ok(mysteryService.getFeaturedSubmission(classroomId)));
  }

  @Operation(summary = "Get all featured mystery submissions by classroom, newest first")
  @GetMapping("/classroom/{classroomId}/featured/all")
  @PreAuthorize("hasAnyAuthority('student', 'instructor')")
  public ResponseEntity<ApiResponse<List<MysterySubmissionResponseDTO>>> getAllFeaturedSubmissions(
      @PathVariable UUID classroomId) {
    return ResponseEntity.ok(ApiResponse.ok(mysteryService.getAllFeaturedSubmissions(classroomId)));
  }
}
