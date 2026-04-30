package ntnu.no.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ntnu.no.backend.dto.mystery.MysteryCompletionRequestDTO;
import ntnu.no.backend.dto.mystery.MysteryCompletionResponseDTO;
import ntnu.no.backend.dto.mystery.MysteryPlayDTO;
import ntnu.no.backend.dto.mystery.MysteryReviewActionDTO;
import ntnu.no.backend.dto.mystery.MysteryReviewDTO;
import ntnu.no.backend.dto.mystery.MysterySubmissionRequestDTO;
import ntnu.no.backend.model.auth.User;
import ntnu.no.backend.service.MysteryService;
import ntnu.no.backend.shared.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * Endpoints for the weekly mystery feature.
 *
 * <p>Student-facing endpoints return {@link MysteryPlayDTO} which omits the
 * correct answer index. Teacher-facing endpoints return
 * {@link MysteryReviewDTO} which includes it.</p>
 */
@Tag(name = "Mysteries", description = "Endpoints for mystery submissions")
@RestController
@RequestMapping("/api/v1/mysteries")
@RequiredArgsConstructor
public class MysteryController {

  private final MysteryService mysteryService;

  @Operation(summary = "Submit a mystery (multipart with image)")
  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasAuthority('student')")
  public ResponseEntity<ApiResponse<MysteryReviewDTO>> submitMystery(
      @RequestPart("submission") @Valid MysterySubmissionRequestDTO dto,
      @RequestPart("image") MultipartFile image,
      @AuthenticationPrincipal User user) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.ok(mysteryService.submitMystery(dto, image, user.getId())));
  }

  @Operation(summary = "Get approved mysteries available for play (excludes my own)")
  @GetMapping("/classroom/{classroomId}/play")
  @PreAuthorize("hasAuthority('student')")
  public ResponseEntity<ApiResponse<List<MysteryPlayDTO>>> getPlayList(
      @PathVariable UUID classroomId,
      @AuthenticationPrincipal User user) {
    return ResponseEntity.ok(ApiResponse.ok(mysteryService.getPlayList(classroomId, user.getId())));
  }

  @Operation(summary = "Get a single mystery for play")
  @GetMapping("/{submissionId}/play")
  @PreAuthorize("hasAuthority('student')")
  public ResponseEntity<ApiResponse<MysteryPlayDTO>> getMysteryForPlay(
      @PathVariable UUID submissionId,
      @AuthenticationPrincipal User user) {
    return ResponseEntity.ok(ApiResponse.ok(mysteryService.getMysteryForPlay(submissionId, user.getId())));
  }

  @Operation(summary = "Submit a pick for a mystery")
  @PostMapping("/{submissionId}/completion")
  @PreAuthorize("hasAuthority('student')")
  public ResponseEntity<ApiResponse<MysteryCompletionResponseDTO>> completeMystery(
      @PathVariable UUID submissionId,
      @RequestBody @Valid MysteryCompletionRequestDTO dto,
      @AuthenticationPrincipal User user) {
    return ResponseEntity.ok(
        ApiResponse.ok(mysteryService.completeMystery(submissionId, dto.getSelectedIndex(), user.getId())));
  }

  @Operation(summary = "Get all mystery submissions in a classroom (teacher)")
  @GetMapping("/classroom/{classroomId}")
  @PreAuthorize("hasAuthority('instructor')")
  public ResponseEntity<ApiResponse<List<MysteryReviewDTO>>> getSubmissionsByClassroom(
      @PathVariable UUID classroomId) {
    return ResponseEntity.ok(ApiResponse.ok(mysteryService.getSubmissionsByClassroom(classroomId)));
  }

  @Operation(summary = "Get my mystery submissions in a classroom (student)")
  @GetMapping("/classroom/{classroomId}/mine")
  @PreAuthorize("hasAuthority('student')")
  public ResponseEntity<ApiResponse<List<MysteryReviewDTO>>> getMySubmissions(
      @PathVariable UUID classroomId,
      @AuthenticationPrincipal User user) {
    return ResponseEntity.ok(ApiResponse.ok(mysteryService.getMySubmissions(classroomId, user.getId())));
  }

  @Operation(summary = "Get pending mystery submissions in a classroom (teacher)")
  @GetMapping("/classroom/{classroomId}/pending")
  @PreAuthorize("hasAuthority('instructor')")
  public ResponseEntity<ApiResponse<List<MysteryReviewDTO>>> getPendingSubmissions(
      @PathVariable UUID classroomId) {
    return ResponseEntity.ok(ApiResponse.ok(mysteryService.getPendingSubmissions(classroomId)));
  }

  @Operation(summary = "Approve or reject a mystery submission (teacher)")
  @PatchMapping("/{submissionId}/review")
  @PreAuthorize("hasAuthority('instructor')")
  public ResponseEntity<ApiResponse<MysteryReviewDTO>> reviewSubmission(
      @PathVariable UUID submissionId,
      @RequestBody @Valid MysteryReviewActionDTO dto,
      @AuthenticationPrincipal User user) {
    return ResponseEntity.ok(ApiResponse.ok(mysteryService.reviewSubmission(submissionId, dto, user.getId())));
  }
}
