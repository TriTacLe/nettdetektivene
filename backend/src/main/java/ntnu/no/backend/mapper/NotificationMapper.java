package ntnu.no.backend.mapper;

import ntnu.no.backend.dto.notification.NotificationResponseDTO;
import ntnu.no.backend.model.Notification;

/**
 * Utility class for mapping between {@link Notification} entities and related DTOs.
 *
 * @author tri
 * @version 0.1
 * @since 0.1
 * @see Notification
 * @see NotificationResponseDTO
 */
public class NotificationMapper {

  private NotificationMapper() {}

  /**
   * Converts a {@link Notification} entity to a {@link NotificationResponseDTO}.
   *
   * @param notification the notification entity to convert
   * @return a {@link NotificationResponseDTO}, or {@code null} if {@code notification} is null
   */
  public static NotificationResponseDTO toDTO(Notification notification) {
    if (notification == null) return null;
    return new NotificationResponseDTO(
      notification.getId(),
      notification.getUser().getId(),
      notification.getClassroom() != null ? notification.getClassroom().getId() : null,
      notification.getMsg(),
      notification.getRefType(),
      notification.getRefId(),
      notification.isRead(),
      notification.getCreatedAt()
    );
  }
}
