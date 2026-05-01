package ntnu.no.backend.service;

import jakarta.persistence.EntityNotFoundException;
import ntnu.no.backend.dto.notification.NotificationResponseDTO;
import ntnu.no.backend.model.Classroom;
import ntnu.no.backend.model.Notification;
import ntnu.no.backend.model.auth.User;
import ntnu.no.backend.repository.NotificationRepository;
import ntnu.no.backend.util.ClassroomTestFactory;
import ntnu.no.backend.util.NotificationTestFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link NotificationService}.
 *
 * @author tri
 * @version 0.1
 * @since 0.1
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

  @Mock
  private NotificationRepository notificationRepository;

  @InjectMocks
  private NotificationService notificationService;

  @Test
  void getNotificationsForUser_returnsMappedDTOs() {
    UUID userId = UUID.randomUUID();
    User user = NotificationTestFactory.user(userId);
    Notification notification = NotificationTestFactory.notification(user);

    when(notificationRepository.findByUserId(userId)).thenReturn(List.of(notification));

    List<NotificationResponseDTO> result = notificationService.getNotificationsForUser(user);

    assertThat(result).hasSize(1);
    assertThat(result.getFirst().msg()).isEqualTo("Din eleveksempel er godkjent");
    assertThat(result.getFirst().isRead()).isFalse();
  }

  @Test
  void getNotificationsForUser_returnsEmptyList_whenNoneExist() {
    UUID userId = UUID.randomUUID();
    User user = NotificationTestFactory.user(userId);
    when(notificationRepository.findByUserId(userId)).thenReturn(List.of());

    List<NotificationResponseDTO> result = notificationService.getNotificationsForUser(user);

    assertThat(result).isEmpty();
  }

  @Test
  void markAsRead_returnsUpdatedDTO_whenOwnerRequests() {
    UUID notificationId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    User user = NotificationTestFactory.user(userId);
    Notification notification = NotificationTestFactory.notification(user);

    when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));

    NotificationResponseDTO result = notificationService.markAsRead(notificationId, user);

    assertThat(result.isRead()).isTrue();
  }

  @Test
  void markAsRead_throwsEntityNotFoundException_whenNotFound() {
    UUID notificationId = UUID.randomUUID();
    User user = NotificationTestFactory.user(UUID.randomUUID());
    when(notificationRepository.findById(notificationId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> notificationService.markAsRead(notificationId, user))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessageContaining(notificationId.toString());
  }

  @Test
  void markAsRead_throwsIllegalArgumentException_whenWrongUser() {
    UUID notificationId = UUID.randomUUID();
    User owner = NotificationTestFactory.user(UUID.randomUUID());
    User otherUser = NotificationTestFactory.user(UUID.randomUUID());
    Notification notification = NotificationTestFactory.notification(owner);

    when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));

    assertThatThrownBy(() -> notificationService.markAsRead(notificationId, otherUser))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void createTeacherAddedNotification_savesNotification() {
    UUID userId = UUID.randomUUID();
    User recipient = NotificationTestFactory.user(userId);
    Classroom classroom = ClassroomTestFactory.classroom();

    when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));

    notificationService.createTeacherAddedNotification(recipient, classroom);

    verify(notificationRepository).save(any(Notification.class));
  }
}
