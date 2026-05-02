package ntnu.no.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import ntnu.no.backend.dto.notification.NotificationResponseDTO;
import ntnu.no.backend.model.auth.User;
import ntnu.no.backend.service.NotificationService;
import ntnu.no.backend.shared.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for notification endpoints.
 *
 * <p>Notifications are created by the {@code trg_mystery_approved} trigger.
 * This controller only exposes read and mark-as-read operations.</p>
 *
 * @author tri
 * @version 0.1
 * @since 0.1
 */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Endpoints for reading and managing notifications")
public class NotificationController {

  private final NotificationService notificationService;

  /**
   * Returns all notifications for the authenticated user.
   *
   * @param user the authenticated user
   * @return 200 OK with list of notifications
   */
  @GetMapping
  @Operation(summary = "Get all notifications for the authenticated user")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<ApiResponse<List<NotificationResponseDTO>>> getMyNotifications(
      @AuthenticationPrincipal User user) {
    return ResponseEntity.ok(ApiResponse.ok(notificationService.getNotificationsForUser(user)));
  }

  /**
   * Marks a notification as read. Only the recipient can mark their own notifications.
   *
   * @param id   the notification ID
   * @param user the authenticated user
   * @return 200 OK with the updated notification
   */
  @PatchMapping("/{id}/read")
  @Operation(summary = "Mark a notification as read")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<ApiResponse<NotificationResponseDTO>> markAsRead(
      @PathVariable UUID id,
      @AuthenticationPrincipal User user) {
    return ResponseEntity.ok(ApiResponse.ok(notificationService.markAsRead(id, user)));
  }
}
