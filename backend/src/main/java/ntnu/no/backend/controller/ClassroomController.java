package ntnu.no.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ntnu.no.backend.dto.classroom.ClassroomRequestDTO;
import ntnu.no.backend.dto.classroom.ClassroomResponseDTO;
import ntnu.no.backend.model.auth.User;
import ntnu.no.backend.service.ClassroomService;
import ntnu.no.backend.shared.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
 * REST controller for classroom endpoints.
 *
 * @author tri
 * @version 0.1
 * @since 0.1
 */
@RestController
@RequestMapping("/api/v1/classrooms")
@RequiredArgsConstructor
@Tag(name = "Classrooms", description = "Endpoints for classrooms")
public class ClassroomController {

  private final ClassroomService classroomService;

  /**
   * Returns all active classrooms.
   *
   * @return 200 OK with list of classrooms
   */
  @GetMapping
  @Operation(summary = "Get all classrooms")
  public ResponseEntity<ApiResponse<List<ClassroomResponseDTO>>> getAllClassrooms() {
    return ResponseEntity.ok(ApiResponse.ok(classroomService.getAllClassroom()));
  }

  /**
   * Returns a single classroom by ID.
   *
   * @param id the classroom ID
   * @return 200 OK with the classroom
   */
  @GetMapping("/{id}")
  @Operation(summary = "Get classroom by ID")
  public ResponseEntity<ApiResponse<ClassroomResponseDTO>> getClassroom(@PathVariable UUID id) {
    return ResponseEntity.ok(ApiResponse.ok(classroomService.getClassroom(id)));
  }

  /**
   * Creates a new classroom. Only accessible by instructors and admins.
   *
   * @param dto         the request body
   * @param currentUser the authenticated user who becomes the creator
   * @return 201 Created with the created classroom
   */
  @PostMapping
  @Operation(summary = "Create a new classroom")
  @PreAuthorize("hasAnyRole('instructor', 'inst_admin', 'sys_admin')")
  public ResponseEntity<ApiResponse<ClassroomResponseDTO>> createClassroom(
      @RequestBody @Valid ClassroomRequestDTO dto,
      @AuthenticationPrincipal User currentUser) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.ok(classroomService.createClassroom(dto, currentUser)));
  }

  /**
   * Updates an existing classroom. Only accessible by instructors and admins.
   *
   * @param id  the classroom ID
   * @param dto the request body with updated values
   * @return 200 OK with the updated classroom
   */
  @PatchMapping("/{id}")
  @Operation(summary = "Update a classroom")
  @PreAuthorize("hasAnyRole('instructor', 'inst_admin', 'sys_admin')")
  public ResponseEntity<ApiResponse<ClassroomResponseDTO>> updateClassroom(
      @PathVariable UUID id,
      @RequestBody @Valid ClassroomRequestDTO dto) {
    return ResponseEntity.ok(ApiResponse.ok(classroomService.updateClassroom(dto, id)));
  }

  /**
   * Soft-deletes a classroom by ID. Only accessible by instructors and admins.
   *
   * @param id the classroom ID
   * @return 204 No Content
   */
  @DeleteMapping("/{id}")
  @Operation(summary = "Delete a classroom")
  @PreAuthorize("hasAnyRole('instructor', 'inst_admin', 'sys_admin')")
  public ResponseEntity<Void> deleteClassroom(@PathVariable UUID id) {
    classroomService.deleteClassroom(id);
    return ResponseEntity.noContent().build();
  }

  /**
   * Returns all active classrooms belonging to an institution.
   *
   * @param institutionId the institution ID
   * @return 200 OK with list of classrooms for the institution
   */
  @GetMapping("/institution/{institutionId}")
  @Operation(summary = "Get classrooms by institution")
  public ResponseEntity<ApiResponse<List<ClassroomResponseDTO>>> getAllClassroomsByInstitution(
      @PathVariable UUID institutionId) {
    return ResponseEntity.ok(ApiResponse.ok(classroomService.getAllClassroomsByInstitution(institutionId)));
  }
}
