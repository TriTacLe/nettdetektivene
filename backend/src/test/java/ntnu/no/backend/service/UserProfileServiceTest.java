package ntnu.no.backend.service;

import jakarta.persistence.EntityNotFoundException;
import ntnu.no.backend.dto.profile.UserProfileResponseDTO;
import ntnu.no.backend.dto.profile.UserProfileUpdateRequestDTO;
import ntnu.no.backend.model.Classroom;
import ntnu.no.backend.model.auth.User;
import ntnu.no.backend.model.auth.UserProfile;
import ntnu.no.backend.model.classroom.ClassroomInstructor;
import ntnu.no.backend.model.classroom.ClassroomStudent;
import ntnu.no.backend.model.Notification;
import ntnu.no.backend.repository.ClassroomInstructorRepository;
import ntnu.no.backend.repository.ClassroomStudentRepository;
import ntnu.no.backend.repository.NotificationRepository;
import ntnu.no.backend.repository.UserProfileRepository;
import ntnu.no.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link UserProfileService}.
 * Verifies profile retrieval, update logic and censored word validation.
 *
 * @author norakirknes
 * @version 0.1
 * @since 0.1
 */
@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

  @Mock
  private UserProfileRepository userProfileRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private ClassroomStudentRepository classroomStudentRepository;

  @Mock
  private ClassroomInstructorRepository classroomInstructorRepository;

  @Mock
  private NotificationRepository notificationRepository;

  @Mock
  private JdbcTemplate jdbcTemplate;

  @InjectMocks
  private UserProfileService userProfileService;

  private User createUser(UUID id, String displayName) {
    return User.builder()
        .id(id)
        .displayName(displayName)
        .email("test@ntnu.no")
        .build();
  }

  private UserProfile createProfile(User user) {
    return UserProfile.builder()
        .id(UUID.randomUUID())
        .user(user)
        .displayName(user.getDisplayName())
        .feideId("test@ntnu.no")
        .build();
  }

  /**
   * Verifies that getByUser returns the profile as a DTO.
   */
  @Test
  void getByUser_returnsDTO_whenProfileExists() {
    User user = createUser(UUID.randomUUID(), "Test");
    UserProfile profile = createProfile(user);

    when(userProfileRepository.findByUser(user)).thenReturn(Optional.of(profile));

    UserProfileResponseDTO result = userProfileService.getByUser(user);

    assertThat(result.displayName()).isEqualTo("Test");
  }

  /**
   * Verifies that getByUser throws when no profile exists.
   */
  @Test
  void getByUser_throwsEntityNotFoundException_whenNoProfile() {
    User user = createUser(UUID.randomUUID(), "Test");

    when(userProfileRepository.findByUser(user)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> userProfileService.getByUser(user))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessageContaining("No profile found");
  }

  /**
   * Verifies that getByFeideId returns the profile as a DTO.
   */
  @Test
  void getByFeideId_returnsDTO_whenProfileExists() {
    User user = createUser(UUID.randomUUID(), "Test");
    UserProfile profile = createProfile(user);

    when(userProfileRepository.findByFeideId("test@ntnu.no")).thenReturn(Optional.of(profile));

    UserProfileResponseDTO result = userProfileService.getByFeideId("test@ntnu.no");

    assertThat(result.displayName()).isEqualTo("Test");
  }

  /**
   * Verifies that getByFeideId throws when no profile exists.
   */
  @Test
  void getByFeideId_throwsEntityNotFoundException_whenNoProfile() {
    when(userProfileRepository.findByFeideId("unknown@ntnu.no")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> userProfileService.getByFeideId("unknown@ntnu.no"))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessageContaining("No profile found");
  }

  /**
   * Verifies that update sets display name directly when user is not in a classroom.
   */
  @Test
  void update_setsDisplayNameDirectly_whenNotInClassroom() {
    User user = createUser(UUID.randomUUID(), "Test");
    UserProfile profile = createProfile(user);
    UserProfileUpdateRequestDTO dto = UserProfileUpdateRequestDTO.builder()
        .displayName("NewTest")
        .build();

    when(userProfileRepository.findByUser(user)).thenReturn(Optional.of(profile));

    UserProfileResponseDTO result = userProfileService.update(user, dto);

    assertThat(result.displayName()).isEqualTo("NewTest");
    verify(userRepository).save(user);
  }

  /**
   * Verifies that update calls stored procedure when user is in a classroom.
   */
  @Test
  void update_callsStoredProcedure_whenInClassroom() {
    UUID userId = UUID.randomUUID();
    User user = createUser(userId, "Test");
    UserProfile profile = createProfile(user);
    Classroom classroom = Classroom.builder().id(UUID.randomUUID()).build();
    ClassroomStudent membership = ClassroomStudent.builder()
        .classroom(classroom)
        .user(user)
        .isAllowed(true)
        .build();
    UserProfileUpdateRequestDTO dto = UserProfileUpdateRequestDTO.builder()
        .displayName("NewTest")
        .build();

    when(userProfileRepository.findByUser(user)).thenReturn(Optional.of(profile));
    when(classroomStudentRepository.findByUser(user)).thenReturn(Optional.of(membership));

    userProfileService.update(user, dto);

    verify(jdbcTemplate).update("CALL sp_request_display_name_change(?, ?)",
        userId.toString(), "NewTest");
  }

  /**
   * Verifies that update throws IllegalArgumentException when display name contains censored word.
   */
  @Test
  void update_throwsIllegalArgument_whenDisplayNameContainsCensoredWord() {
    User user = createUser(UUID.randomUUID(), "Test");
    UserProfile profile = createProfile(user);
    UserProfileUpdateRequestDTO dto = UserProfileUpdateRequestDTO.builder()
        .displayName("helvete")
        .build();

    when(userProfileRepository.findByUser(user)).thenReturn(Optional.of(profile));
    when(classroomStudentRepository.findByUser(user)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> userProfileService.update(user, dto))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("upassende ord");
  }

  /**
   * Verifies that instructors are notified when a student in a classroom uses a censored display name.
   */
  @Test
  void update_notifiesInstructors_whenCensoredWordAndInClassroom() {
    User user = createUser(UUID.randomUUID(), "Student");
    UserProfile profile = createProfile(user);
    Classroom classroom = Classroom.builder().id(UUID.randomUUID()).build();
    ClassroomStudent membership = ClassroomStudent.builder()
        .classroom(classroom)
        .user(user)
        .isAllowed(true)
        .build();
    User teacher = createUser(UUID.randomUUID(), "Teacher");
    ClassroomInstructor instructor = ClassroomInstructor.builder()
        .classroom(classroom)
        .user(teacher)
        .build();
    UserProfileUpdateRequestDTO dto = UserProfileUpdateRequestDTO.builder()
        .displayName("helvete")
        .build();

    when(userProfileRepository.findByUser(user)).thenReturn(Optional.of(profile));
    when(classroomStudentRepository.findByUser(user)).thenReturn(Optional.of(membership));
    when(classroomInstructorRepository.findAllByClassroom(classroom)).thenReturn(List.of(instructor));

    assertThatThrownBy(() -> userProfileService.update(user, dto))
        .isInstanceOf(IllegalArgumentException.class);

    verify(notificationRepository).save(any(Notification.class));
  }

  /**
   * Verifies that no notification is sent when a censored word is used but the user is not in a classroom.
   */
  @Test
  void update_doesNotNotify_whenCensoredWordAndNotInClassroom() {
    User user = createUser(UUID.randomUUID(), "Student");
    UserProfile profile = createProfile(user);
    UserProfileUpdateRequestDTO dto = UserProfileUpdateRequestDTO.builder()
        .displayName("helvete")
        .build();

    when(userProfileRepository.findByUser(user)).thenReturn(Optional.of(profile));
    when(classroomStudentRepository.findByUser(user)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> userProfileService.update(user, dto))
        .isInstanceOf(IllegalArgumentException.class);

    verify(notificationRepository, never()).save(any());
  }

  /**
   * Verifies that update without display name change only updates avatar config.
   */
  @Test
  void update_updatesAvatarOnly_whenNoDisplayNameProvided() {
    User user = createUser(UUID.randomUUID(), "Test");
    UserProfile profile = createProfile(user);
    UserProfileUpdateRequestDTO dto = UserProfileUpdateRequestDTO.builder()
        .avatarConfig("{\"hair\": \"short\"}")
        .build();

    when(userProfileRepository.findByUser(user)).thenReturn(Optional.of(profile));

    UserProfileResponseDTO result = userProfileService.update(user, dto);

    assertThat(result).isNotNull();
    verify(classroomStudentRepository, never()).existsByUser(any());
  }
}
