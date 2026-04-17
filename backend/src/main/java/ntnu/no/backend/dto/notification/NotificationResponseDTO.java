package ntnu.no.backend.dto.notification;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO representing a notification sent to a user.
 * Notifications are created automatically by the {@code trg_mystery_approved} trigger.
 *
 * @param id unique identifier of the notification
 * @param recipientId unique identifier of the recipient user
 * @param classroomId unique identifier of the related classroom, may be null
 * @param msg the notification message
 * @param refType optional type of the referenced entity
 * @param refId optional ID of the referenced entity
 * @param isRead whether the user has read this notification
 * @param createdAt timestamp of when the notification was created
 *
 * @author tri
 * @version 0.1
 * @since 0.1
 */
@Schema(description = "Represents a notification sent to a user")
public record NotificationResponseDTO(
    UUID id,
    UUID recipientId,
    UUID classroomId,
    String msg,
    String refType,
    UUID refId,
    boolean isRead,
    LocalDateTime createdAt
) {}
