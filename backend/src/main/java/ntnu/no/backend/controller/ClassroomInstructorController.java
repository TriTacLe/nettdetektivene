package ntnu.no.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import ntnu.no.backend.dto.classroom.ClassroomInstructorResponseDTO;
import ntnu.no.backend.model.auth.User;
import ntnu.no.backend.service.ClassroomInstructorService;
import ntnu.no.backend.shared.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for managing instructor assignments to classrooms.
 * All operations require the authenticated user to be an instructor.
 *
 * @author norakirknes
 * @version 0.1
 * @since 0.1
 */
@RestController
@RequestMapping("/api/v1/classrooms")
@RequiredArgsConstructor
@Tag(name = "Classroom Instructors", description = "Endpoints for classroom instructor management")
public class ClassroomInstructorController {

  private final ClassroomInstructorService classroomInstructorService;

  /**
   * Lists all instructors in a classroom.
   *
   * @param classroomId the classroom ID
   * @param user the authenticated instructor
   * @return 200 OK with list of instructors
   */
  @GetMapping("/{classroomId}/instructors")
  @Operation(summary = "List all instructors in a classroom")
  @PreAuthorize("hasAuthority('instructor')")
  public ResponseEntity<ApiResponse<List<ClassroomInstructorResponseDTO>>> getInstructors(
      @PathVariable UUID classroomId,
      @AuthenticationPrincipal User user) {
    return ResponseEntity.ok(ApiResponse.ok(classroomInstructorService.getInstructors(classroomId, user)));
  }

  /**
   * Lists classrooms the authenticated instructor belongs to.
   *
   * @param user the authenticated instructor
   * @return 200 OK with list of assignments
   */
  @GetMapping("/mine")
  @Operation(summary = "List classrooms for the authenticated instructor")
  @PreAuthorize("hasAuthority('instructor')")
  public ResponseEntity<ApiResponse<List<ClassroomInstructorResponseDTO>>> getMyClassrooms(
      @AuthenticationPrincipal User user) {
    return ResponseEntity.ok(ApiResponse.ok(classroomInstructorService.getMyClassrooms(user)));
  }

  /**
   * Adds an instructor to a classroom.
   *
   * @param classroomId the classroom ID
   * @param userId the user ID of the instructor to add
   * @param user the authenticated instructor
   * @return 201 Created with the created assignment
   */
  @PostMapping("/{classroomId}/instructors/{userId}")
  @Operation(summary = "Add an instructor to a classroom")
  @PreAuthorize("hasAuthority('instructor')")
  public ResponseEntity<ApiResponse<ClassroomInstructorResponseDTO>> addInstructor(
      @PathVariable UUID classroomId,
      @PathVariable UUID userId,
      @AuthenticationPrincipal User user) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.ok(classroomInstructorService.addInstructor(classroomId, userId, user)));
  }

  /**
   * Removes an instructor from a classroom.
   *
   * @param classroomId the classroom ID
   * @param userId the user ID of the instructor to remove
   * @param user the authenticated instructor
   * @return 204 No Content
   */
  @DeleteMapping("/{classroomId}/instructors/{userId}")
  @Operation(summary = "Remove an instructor from a classroom")
  @PreAuthorize("hasAuthority('instructor')")
  public ResponseEntity<Void> removeInstructor(
      @PathVariable UUID classroomId,
      @PathVariable UUID userId,
      @AuthenticationPrincipal User user) {
    classroomInstructorService.removeInstructor(classroomId, userId, user);
    return ResponseEntity.noContent().build();
  }
}
