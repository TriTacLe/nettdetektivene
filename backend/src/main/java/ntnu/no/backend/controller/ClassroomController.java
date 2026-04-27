package ntnu.no.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ntnu.no.backend.dto.classroom.ClassroomRequestDTO;
import ntnu.no.backend.dto.classroom.ClassroomResponseDTO;
import ntnu.no.backend.dto.task.ClassroomTaskCreateRequestDTO;
import ntnu.no.backend.dto.task.ClassroomTaskResponseDTO;
import ntnu.no.backend.dto.task.ClassroomTaskUpdateRequestDTO;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for classroom endpoints.
 *
 * @author tri, aninesst
 * @version 0.2
 * @since 0.1
 */
@RestController
@RequestMapping("/api/v1/classrooms")
@RequiredArgsConstructor
@Tag(name = "Classrooms", description = "Endpoints for classrooms")
public class ClassroomController {

  private final ClassroomService classroomService;

  @GetMapping
  @Operation(summary = "Get classrooms for the authenticated instructor")
  @PreAuthorize("hasAnyAuthority('instructor', 'inst_admin', 'sys_admin')")
  public ResponseEntity<ApiResponse<List<ClassroomResponseDTO>>> getAllClassrooms(
      @AuthenticationPrincipal User user) {
    return ResponseEntity.ok(ApiResponse.ok(classroomService.getClassroomsForInstructor(user)));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get classroom by ID")
  public ResponseEntity<ApiResponse<ClassroomResponseDTO>> getClassroom(@PathVariable UUID id) {
    return ResponseEntity.ok(ApiResponse.ok(classroomService.getClassroom(id)));
  }

  @PostMapping
  @Operation(summary = "Create a new classroom")
  @PreAuthorize("hasAnyAuthority('instructor', 'inst_admin', 'sys_admin')")
  public ResponseEntity<ApiResponse<ClassroomResponseDTO>> createClassroom(
      @RequestBody @Valid ClassroomRequestDTO dto,
      @AuthenticationPrincipal User currentUser) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.ok(classroomService.createClassroom(dto, currentUser)));
  }

  @PatchMapping("/{id}")
  @Operation(summary = "Update a classroom")
  @PreAuthorize("hasAnyAuthority('instructor', 'inst_admin', 'sys_admin')")
  public ResponseEntity<ApiResponse<ClassroomResponseDTO>> updateClassroom(
      @PathVariable UUID id,
      @RequestBody @Valid ClassroomRequestDTO dto) {
    return ResponseEntity.ok(ApiResponse.ok(classroomService.updateClassroom(dto, id)));
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "Delete a classroom")
  @PreAuthorize("hasAnyAuthority('instructor', 'inst_admin', 'sys_admin')")
  public ResponseEntity<Void> deleteClassroom(@PathVariable UUID id) {
    classroomService.deleteClassroom(id);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/institution/{institutionId}")
  @Operation(summary = "Get classrooms by institution")
  public ResponseEntity<ApiResponse<List<ClassroomResponseDTO>>> getAllClassroomsByInstitution(
      @PathVariable UUID institutionId) {
    return ResponseEntity.ok(ApiResponse.ok(classroomService.getAllClassroomsByInstitution(institutionId)));
  }

  @GetMapping("/{classroomId}/tasks")
  @Operation(summary = "Get tasks assigned to a classroom")
  public ResponseEntity<ApiResponse<List<ClassroomTaskResponseDTO>>> getClassroomTasks(
      @PathVariable UUID classroomId,
      @RequestParam(defaultValue = "false") boolean activeOnly,
      @RequestParam(defaultValue = "false") boolean includeUnlocked,
      @RequestParam(defaultValue = "false") boolean testMode,
      @AuthenticationPrincipal User currentUser) {
    if (testMode) {
      if (currentUser == null || currentUser.getAuthorities().stream()
          .noneMatch(a -> a.getAuthority().equals("instructor")
              || a.getAuthority().equals("inst_admin")
              || a.getAuthority().equals("sys_admin"))) {
        return ResponseEntity.status(403).body(ApiResponse.error("Access denied"));
      }
      return ResponseEntity.ok(ApiResponse.ok(classroomService.getActiveClassroomTasksTestMode(classroomId, currentUser.getId())));
    }
    if (activeOnly) {
      if (includeUnlocked && currentUser != null) {
        return ResponseEntity.ok(ApiResponse.ok(classroomService.getActiveClassroomTasksWithUnlocked(classroomId, currentUser.getId())));
      }
      return ResponseEntity.ok(ApiResponse.ok(classroomService.getActiveClassroomTasks(classroomId)));
    }
    return ResponseEntity.ok(ApiResponse.ok(classroomService.getClassroomTasks(classroomId)));
  }

  @GetMapping("/{classroomId}/tasks/unlocked")
  @Operation(summary = "Get unlocked tasks for authenticated user in a classroom")
  public ResponseEntity<ApiResponse<List<ClassroomTaskResponseDTO>>> getUnlockedClassroomTasks(
      @PathVariable UUID classroomId,
      @AuthenticationPrincipal User currentUser) {
    return ResponseEntity.ok(ApiResponse.ok(classroomService.getUnlockedClassroomTasks(classroomId, currentUser.getId())));
  }

  @GetMapping("/{classroomId}/tasks/unlocked/{userId}")
  @Operation(summary = "Get unlocked tasks for a specific user (instructor only)")
  @PreAuthorize("hasAnyAuthority('instructor', 'inst_admin', 'sys_admin')")
  public ResponseEntity<ApiResponse<List<ClassroomTaskResponseDTO>>> getUnlockedClassroomTasksForUser(
      @PathVariable UUID classroomId,
      @PathVariable UUID userId) {
    return ResponseEntity.ok(ApiResponse.ok(classroomService.getUnlockedClassroomTasks(classroomId, userId)));
  }

  @PostMapping("/{classroomId}/tasks")
  @Operation(summary = "Assign a task to a classroom")
  @PreAuthorize("hasAnyAuthority('instructor', 'inst_admin', 'sys_admin')")
  public ResponseEntity<ApiResponse<ClassroomTaskResponseDTO>> createClassroomTask(
      @PathVariable UUID classroomId,
      @RequestBody @Valid ClassroomTaskCreateRequestDTO dto) {
    return ResponseEntity.status(HttpStatus.CREATED)
      .body(ApiResponse.ok(classroomService.createClassroomTask(classroomId, dto)));
  }

  @PostMapping("/{classroomId}/tasks/{taskId}/unlock")
  @Operation(summary = "Unlock a task for a user in a classroom")
  @PreAuthorize("hasAnyAuthority('instructor', 'inst_admin', 'sys_admin')")
  public ResponseEntity<Void> unlockTaskForUser(
      @PathVariable UUID classroomId,
      @PathVariable UUID taskId,
      @RequestParam UUID userId) {
    classroomService.unlockTaskForUser(classroomId, taskId, userId);
    return ResponseEntity.ok().build();
  }

  @PatchMapping("/{classroomId}/tasks/{taskId}")
  @Operation(summary = "Update a task assignment in a classroom")
  @PreAuthorize("hasAnyAuthority('instructor', 'inst_admin', 'sys_admin')")
  public ResponseEntity<ApiResponse<ClassroomTaskResponseDTO>> updateClassroomTask(
      @PathVariable UUID classroomId,
      @PathVariable UUID taskId,
      @RequestBody ClassroomTaskUpdateRequestDTO dto) {
    return ResponseEntity.ok(ApiResponse.ok(
        classroomService.updateClassroomTask(classroomId, taskId, dto)
    ));
  }
}
