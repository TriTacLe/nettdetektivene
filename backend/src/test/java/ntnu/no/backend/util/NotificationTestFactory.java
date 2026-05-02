package ntnu.no.backend.util;

import ntnu.no.backend.dto.notification.NotificationResponseDTO;
import ntnu.no.backend.model.Notification;
import ntnu.no.backend.model.auth.User;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.UUID;

public class NotificationTestFactory {

  public static User user(UUID id) {
    User user = User.builder()
        .displayName("Tri")
        .email("tri@ntnu.no")
        .build();
    ReflectionTestUtils.setField(user, "id", id);
    return user;
  }

  public static Notification notification(User recipient) {
    return Notification.builder()
        .user(recipient)
        .msg("Din eleveksempel er godkjent")
        .isRead(false)
        .build();
  }

  public static NotificationResponseDTO responseDTO(UUID id, UUID recipientId) {
    return new NotificationResponseDTO(
        id, recipientId, null,
        "Din eleveksempel er godkjent", null, null,
        false, LocalDateTime.now()
    );
  }
}
