package ntnu.no.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ntnu.no.backend.dto.classroom.ClassroomResponseDTO;
import ntnu.no.backend.dto.classroom.ClassroomStudentResponseDTO;
import ntnu.no.backend.dto.classroom.JoinClassroomRequestDTO;
import ntnu.no.backend.model.auth.User;
import ntnu.no.backend.service.ClassroomStudentService;
import ntnu.no.backend.shared.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for managing classroom student membership.
 * Join is open to students; all other operations require
 * the authenticated instructor to belong to the classroom.
 *
 * @author norakirknes
 * @version 0.1
 * @since 0.1
 */
@RestController
@RequestMapping("/api/v1/classrooms")
@RequiredArgsConstructor
@Tag(name = "Classroom Students", description = "Endpoints for classroom student membership")
public class ClassroomStudentController {

  private final ClassroomStudentService classroomStudentService;


  /**
   * Gets the classroom the authenticated student belongs to.
   *
   * @param user the authenticated student
   * @return 200 OK with the classroom
   */
  @GetMapping("/me")
  @Operation(summary = "Get the classroom the authenticated student belongs to")
  @PreAuthorize("hasAuthority('student')")
  public ResponseEntity<ApiResponse<ClassroomResponseDTO>> getMyClassroom(
      @AuthenticationPrincipal User user) {
    return ResponseEntity.ok(ApiResponse.ok(classroomStudentService.getMyClassroom(user)));
  }

  /**
   * Joins the authenticated student to a classroom via join code.
   *
   * @param dto the request body containing the join code
   * @param user the authenticated student
   * @return 201 Created with the created membership
   */
  @PostMapping("/join")
  @Operation(summary = "Join a classroom using a join code")
  @PreAuthorize("hasAuthority('student')")
  public ResponseEntity<ApiResponse<ClassroomStudentResponseDTO>> join(
      @RequestBody @Valid JoinClassroomRequestDTO dto,
      @AuthenticationPrincipal User user) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.ok(classroomStudentService.join(dto.getJoinCode(), user)));
  }

  /**
   * Lists all students in a classroom.
   *
   * @param classroomId the classroom ID
   * @param user the authenticated instructor
   * @return 200 OK with list of students
   */
  @GetMapping("/{classroomId}/students")
  @Operation(summary = "List all students in a classroom")
  @PreAuthorize("hasAuthority('instructor')")
  public ResponseEntity<ApiResponse<List<ClassroomStudentResponseDTO>>> getStudents(
      @PathVariable UUID classroomId,
      @AuthenticationPrincipal User user) {
    return ResponseEntity.ok(ApiResponse.ok(classroomStudentService.getStudents(classroomId, user)));
  }

  /**
   * Lists approved students in a classroom.
   *
   * @param classroomId the classroom ID
   * @param user the authenticated instructor
   * @return 200 OK with list of approved students
   */
  @GetMapping("/{classroomId}/students/approved")
  @Operation(summary = "List approved students in a classroom")
  @PreAuthorize("hasAuthority('instructor')")
  public ResponseEntity<ApiResponse<List<ClassroomStudentResponseDTO>>> getApprovedStudents(
      @PathVariable UUID classroomId,
      @AuthenticationPrincipal User user) {
    return ResponseEntity.ok(ApiResponse.ok(classroomStudentService.getApprovedStudents(classroomId, user)));
  }

  /**
   * Lists pending students in a classroom.
   *
   * @param classroomId the classroom ID
   * @param user the authenticated instructor
   * @return 200 OK with list of pending students
   */
  @GetMapping("/{classroomId}/students/pending")
  @Operation(summary = "List pending students in a classroom")
  @PreAuthorize("hasAuthority('instructor')")
  public ResponseEntity<ApiResponse<List<ClassroomStudentResponseDTO>>> getPendingStudents(
      @PathVariable UUID classroomId,
      @AuthenticationPrincipal User user) {
    return ResponseEntity.ok(ApiResponse.ok(classroomStudentService.getPendingStudents(classroomId, user)));
  }

  /**
   * Lists students with pending name change requests in a classroom.
   *
   * @param classroomId the classroom ID
   * @param user the authenticated instructor
   * @return 200 OK with list of students with pending name changes
   */
  @GetMapping("/{classroomId}/students/pending-names")
  @Operation(summary = "List students with pending name change requests")
  @PreAuthorize("hasAuthority('instructor')")
  public ResponseEntity<ApiResponse<List<ClassroomStudentResponseDTO>>> getPendingNameChanges(
      @PathVariable UUID classroomId,
      @AuthenticationPrincipal User user) {
    return ResponseEntity.ok(ApiResponse.ok(classroomStudentService.getPendingNameChanges(classroomId, user)));
  }

  /**
   * Approves a pending student in a classroom.
   *
   * @param classroomId the classroom ID
   * @param userId the student's user ID
   * @param user the authenticated instructor
   * @return 200 OK with the updated membership
   */
  @PostMapping("/{classroomId}/students/{userId}/approve")
  @Operation(summary = "Approve a pending student")
  @PreAuthorize("hasAuthority('instructor')")
  public ResponseEntity<ApiResponse<ClassroomStudentResponseDTO>> approve(
      @PathVariable UUID classroomId,
      @PathVariable UUID userId,
      @AuthenticationPrincipal User user) {
    return ResponseEntity.ok(ApiResponse.ok(classroomStudentService.approve(classroomId, userId, user)));
  }

  /**
   * Approves a student's display name change request.
   *
   * @param classroomId the classroom ID
   * @param userId the student's user ID
   * @param user the authenticated instructor
   * @return 200 OK with the updated membership
   */
  @PostMapping("/{classroomId}/students/{userId}/approve-name")
  @Operation(summary = "Approve a display name change")
  @PreAuthorize("hasAuthority('instructor')")
  public ResponseEntity<ApiResponse<ClassroomStudentResponseDTO>> approveNameChange(
      @PathVariable UUID classroomId,
      @PathVariable UUID userId,
      @AuthenticationPrincipal User user) {
    return ResponseEntity.ok(
        ApiResponse.ok(classroomStudentService.approveNameChange(classroomId, userId, user)));
  }

  /**
   * Rejects a student's display name change request.
   *
   * @param classroomId the classroom ID
   * @param userId the student's user ID
   * @param user the authenticated instructor
   * @return 200 OK with the updated membership
   */
  @PostMapping("/{classroomId}/students/{userId}/reject-name")
  @Operation(summary = "Reject a display name change")
  @PreAuthorize("hasAuthority('instructor')")
  public ResponseEntity<ApiResponse<ClassroomStudentResponseDTO>> rejectNameChange(
      @PathVariable UUID classroomId,
      @PathVariable UUID userId,
      @AuthenticationPrincipal User user) {
    return ResponseEntity.ok(
        ApiResponse.ok(classroomStudentService.rejectNameChange(classroomId, userId, user)));
  }

  /**
   * Removes a student from a classroom.
   *
   * @param classroomId the classroom ID
   * @param userId the student's user ID
   * @param user the authenticated instructor
   * @return 204 No Content
   */
  @DeleteMapping("/{classroomId}/students/{userId}")
  @Operation(summary = "Remove a student from a classroom")
  @PreAuthorize("hasAuthority('instructor')")
  public ResponseEntity<Void> remove(
      @PathVariable UUID classroomId,
      @PathVariable UUID userId,
      @AuthenticationPrincipal User user) {
    classroomStudentService.remove(classroomId, userId, user);
    return ResponseEntity.noContent().build();
  }
}
