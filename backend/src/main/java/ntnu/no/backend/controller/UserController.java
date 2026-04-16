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
 * @author norakirknes
 * @version 0.1
 * @since 0.1
 */
@RestController
@RequestMapping("api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Endpoints for user account management")
public class UserController {
  private final UserService userService;

  @Operation(summary = "Get the authenticated user")
  @ApiResponse(responseCode = "200", description = "User retrieved successfully")
  @ApiResponse(responseCode = "401", description = "Not authenticated")
  @GetMapping("/me")
  public ResponseEntity<UserResponseDTO> getUser(@AuthenticationPrincipal User user) {
    return ResponseEntity.ok(userService.getUser(user.getId()));
  }

  /**
   * user update
   *
   * @param user
   * @param dto
   * @return
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
   * teacher update
   *
   * @param id
   * @param dto
   * @return
   */
  @Operation(summary = "Update a student's account (teacher only)")
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
