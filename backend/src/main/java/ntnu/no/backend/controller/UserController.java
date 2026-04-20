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
   * Updates a student's account. Currently any instructor can update any user by UUID.
   * TODO: restrict to students only and scope to the instructor's own classroom(s)
   *  when classroom membership repositories and services are implemented.
   *
   * @param id  the UUID of the student to update
   * @param dto the update request containing new values
   * @return {@code 200 OK} with the updated user
   */
  @Operation(summary = "Update a student's account (instructor only)")
  @ApiResponse(responseCode = "200", description = "User updated successfully")
  @ApiResponse(responseCode = "400", description = "Invalid input")
  @ApiResponse(responseCode = "401", description = "Not authenticated")
  @ApiResponse(responseCode = "403", description = "Not authorized")
  @PreAuthorize("hasAuthority('instructor')")
  @PatchMapping("/{id}")
  public ResponseEntity<UserResponseDTO> updateUserById(@PathVariable UUID id,
                                                        @Valid @RequestBody UserUpdateDTO dto) {
    return ResponseEntity.ok(userService.updateUser(id, dto));
  }
}
