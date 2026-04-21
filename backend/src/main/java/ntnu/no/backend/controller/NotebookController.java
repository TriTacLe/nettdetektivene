package ntnu.no.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ntnu.no.backend.dto.notebook.NotebookEntryRequestDTO;
import ntnu.no.backend.dto.notebook.NotebookEntryResponseDTO;
import ntnu.no.backend.service.NotebookService;
import ntnu.no.backend.shared.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import ntnu.no.backend.model.auth.User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
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
 * Endpoints for notebook entry management.
 *
 * @author tri
 * @version 0.1
 * @since 0.1
 */
@Tag(name = "Notebooks", description = "Endpoints for notebook entries")
@RestController
@RequestMapping("/api/v1/classrooms/{classroomId}/notebooks")
@RequiredArgsConstructor
public class NotebookController {
  private final NotebookService notebookService;

  /**
   * Returns all notebook entries in a classroom. Restricted to instructors.
   *
   * @param classroomId the classroom ID
   * @return list of all notebook entries
   */
  @Operation(summary = "Get all notebook entries in a classroom")
  @GetMapping
  @PreAuthorize("hasAuthority('instructor')")
  public ResponseEntity<ApiResponse<List<NotebookEntryResponseDTO>>> getAllNotebooks(@PathVariable UUID classroomId) {
    return ResponseEntity.ok(ApiResponse.ok(notebookService.getNotebooksByClassroom(classroomId)));
  }

  /**
   * Returns the authenticated student's notebook entries for a classroom.
   *
   * @param classroomId the classroom ID
   * @param userDetails the authenticated user
   * @return list of the student's notebook entries
   */
  @Operation(summary = "Get the authenticated student's notebook entries for a classroom")
  @GetMapping("/me")
  public ResponseEntity<ApiResponse<List<NotebookEntryResponseDTO>>> getMyNotebooks(
      @PathVariable UUID classroomId,
      @AuthenticationPrincipal User user) {
    return ResponseEntity.ok(ApiResponse.ok(notebookService.getNotebookByClassroomAndUser(classroomId, user.getId())));
  }

  /**
   * Creates a new notebook entry for the authenticated student.
   *
   * @param classroomId the classroom ID
   * @param dto the notebook entry data
   * @param userDetails the authenticated user
   * @return the created notebook entry
   */
  @Operation(summary = "Create a new notebook entry")
  @PostMapping
  public ResponseEntity<ApiResponse<NotebookEntryResponseDTO>> createNotebook(
      @PathVariable UUID classroomId,
      @RequestBody @Valid NotebookEntryRequestDTO dto,
      @AuthenticationPrincipal User user) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.ok(notebookService.createNotebook(classroomId, dto, user.getId())));
  }

  /**
   * Updates an existing notebook entry.
   *
   * @param classroomId the classroom ID
   * @param notebookId the entry ID
   * @param dto the updated notebook entry data
   * @return the updated notebook entry
   */
  @Operation(summary = "Update a notebook entry")
  @PatchMapping("/{notebookId}")
  public ResponseEntity<ApiResponse<NotebookEntryResponseDTO>> updateNotebook(
      @PathVariable UUID classroomId,
      @PathVariable UUID notebookId,
      @RequestBody @Valid NotebookEntryRequestDTO dto) {
    return ResponseEntity.ok(ApiResponse.ok(notebookService.updateNotebook(dto, notebookId)));
  }

  /**
   * Deletes a notebook entry.
   *
   * @param classroomId the classroom ID
   * @param notebookId the entry ID
   */
  @Operation(summary = "Delete a notebook entry")
  @DeleteMapping("/{notebookId}")
  public ResponseEntity<Void> deleteNotebook(
      @PathVariable UUID classroomId,
      @PathVariable UUID notebookId) {
    notebookService.deleteNotebook(notebookId);
    return ResponseEntity.noContent().build();
  }
}
