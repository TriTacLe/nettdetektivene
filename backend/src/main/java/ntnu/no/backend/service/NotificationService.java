package ntnu.no.backend.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import ntnu.no.backend.dto.notification.NotificationResponseDTO;
import ntnu.no.backend.mapper.NotificationMapper;
import ntnu.no.backend.model.Notification;
import ntnu.no.backend.model.auth.User;
import ntnu.no.backend.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service for reading and managing notifications.
 *
 * <p>Notifications are created automatically by the {@code trg_mystery_approved} trigger.
 * This service only reads and marks them as read — never creates them.</p>
 *
 * @author tri
 * @version 0.1
 * @since 0.1
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

  private final NotificationRepository notificationRepository;

  /**
   * Returns all notifications for the given user.
   *
   * @param user the authenticated user
   * @return list of notifications as DTOs
   */
  @Transactional(readOnly = true)
  public List<NotificationResponseDTO> getNotificationsForUser(User user) {
    return notificationRepository.findByUserId(user.getId()).stream()
        .map(NotificationMapper::toDTO)
        .toList();
  }

  /**
   * Marks a notification as read.
   *
   * @param id   the notification ID
   * @param user the authenticated user — must be the notification recipient
   * @return the updated notification as a DTO
   * @throws EntityNotFoundException  if no notification found with the given ID
   * @throws IllegalArgumentException if the notification does not belong to the user
   */
  @Transactional
  public NotificationResponseDTO markAsRead(UUID id, User user) {
    Notification notification = notificationRepository.findById(id)
        .orElseThrow(() -> new EntityNotFoundException("Notification not found with id: " + id));

    if (!notification.getUser().getId().equals(user.getId())) {
      throw new IllegalArgumentException("Notification does not belong to user");
    }

    notification.setRead(true);
    return NotificationMapper.toDTO(notification);
  }
}
