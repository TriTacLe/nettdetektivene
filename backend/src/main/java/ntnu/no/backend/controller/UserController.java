package ntnu.no.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ntnu.no.backend.dto.user.UserResponseDTO;
import ntnu.no.backend.dto.user.UserUpdateDTO;
import ntnu.no.backend.model.auth.User;
import ntnu.no.backend.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Endpoints for user account management.
 *
 * @author norakirknes
 * @version 0.1
 * @since 0.1
 * @see UserService
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Endpoints for user account management")
public class UserController {
  private final UserService userService;

  /**
   * Retrieves the account details of the authenticated user.
   *
   * @param user the authenticated user
   * @return {@code 200 OK} with the user's account details
   */
  @Operation(summary = "Get the authenticated user")
  @ApiResponse(responseCode = "200", description = "User retrieved successfully")
  @ApiResponse(responseCode = "401", description = "Not authenticated")
  @GetMapping("/me")
  public ResponseEntity<UserResponseDTO> getUser(@AuthenticationPrincipal User user) {
    return ResponseEntity.ok(userService.getUser(user.getId()));
  }

  /**
   * Updates the authenticated user's own account.
   *
   * @param user the authenticated user
   * @param dto  the update request containing new values
   * @return {@code 200 OK} with the updated user
   */
  @Operation(summary = "Update the authenticated user's account")
  @ApiResponse(responseCode = "200", description = "User updated successfully")
  @ApiResponse(responseCode = "400", description = "Invalid input")
  @ApiResponse(responseCode = "401", description = "Not authenticated")
  @PatchMapping("/me")
  public ResponseEntity<UserResponseDTO> updateUser(@AuthenticationPrincipal User user,
                                                    @Valid @RequestBody UserUpdateDTO dto) {
    return ResponseEntity.ok(userService.updateUser(user.getId(), dto));
  }

/**
   * Updates a student's account by instructor.
   * Instructors can only update students from their own classrooms.
   *
   * @param targetUserId the UUID of the student to update
   * @param dto the update request containing new values
   * @param requestingInstructor the authenticated instructor
   * @return {@code 200 OK} with the updated user
   */
  @Operation(summary = "Update a student's account (instructor own classroom only)")
  @ApiResponse(responseCode = "200", description = "User updated successfully")
  @ApiResponse(responseCode = "400", description = "Invalid input")
  @ApiResponse(responseCode = "401", description = "Not authenticated")
  @ApiResponse(responseCode = "403", description = "Not authorized - instructor can only update students in their own classrooms")
  @PreAuthorize("hasAuthority('instructor')")
  @PatchMapping("/{targetUserId}")
  public ResponseEntity<UserResponseDTO> updateUserById(
      @PathVariable UUID targetUserId,
      @Valid @RequestBody UserUpdateDTO dto,
      @AuthenticationPrincipal User requestingInstructor) {
    return ResponseEntity.ok(userService.updateStudentByInstructor(targetUserId, dto, requestingInstructor));
  }
}
