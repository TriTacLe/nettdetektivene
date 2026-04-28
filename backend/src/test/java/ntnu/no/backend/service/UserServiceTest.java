package ntnu.no.backend.service;

import jakarta.persistence.EntityNotFoundException;
import ntnu.no.backend.dto.user.UserResponseDTO;
import ntnu.no.backend.dto.user.UserUpdateDTO;
import ntnu.no.backend.model.Classroom;
import ntnu.no.backend.model.auth.Role;
import ntnu.no.backend.model.auth.User;
import ntnu.no.backend.model.auth.UserProfile;
import ntnu.no.backend.model.classroom.ClassroomStudent;
import ntnu.no.backend.model.classroom.ClassroomInstructor;
import ntnu.no.backend.model.classroom.ClassroomInstructorId;
import ntnu.no.backend.repository.ClassroomInstructorRepository;
import ntnu.no.backend.repository.ClassroomRepository;
import ntnu.no.backend.repository.ClassroomStudentRepository;
import ntnu.no.backend.repository.UserProfileRepository;
import ntnu.no.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link UserService}.
 *
 * @author tri
 * @author aninesst
 * @author norakirknes
 * @version 0.1
 * @since 0.1
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private UserProfileRepository userProfileRepository;

  @Mock
  private ClassroomRepository classroomRepository;

  @Mock
  private ClassroomStudentRepository classroomStudentRepository;

  @Mock
  private ClassroomInstructorRepository classroomInstructorRepository;

  @InjectMocks
  private UserService userService;

  private User sampleUser() {
    User user = new User();
    user.setDisplayName("Test User");
    user.setEmail("test@example.com");
    return user;
  }

  @Test
  void getUser_returnsDTO_whenFound() {
    UUID id = UUID.randomUUID();
    User user = sampleUser();
    when(userRepository.findById(id)).thenReturn(Optional.of(user));

    UserResponseDTO result = userService.getUser(id);

    assertThat(result.getDisplayName()).isEqualTo("Test User");
    assertThat(result.getEmail()).isEqualTo("test@example.com");
  }

  @Test
  void getUser_throwsEntityNotFoundException_whenNotFound() {
    UUID id = UUID.randomUUID();
    when(userRepository.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> userService.getUser(id))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessageContaining(id.toString());
  }

  @Test
  void updateUser_returnsUpdatedDTO_whenFound() {
    UUID id = UUID.randomUUID();
    User user = sampleUser();
    when(userRepository.findById(id)).thenReturn(Optional.of(user));

    UserUpdateDTO dto = UserUpdateDTO.builder()
        .displayName("Updated Name")
        .build();

    UserResponseDTO result = userService.updateUser(id, dto);

    assertThat(result.getDisplayName()).isEqualTo("Updated Name");
  }

  @Test
  void updateUser_throwsEntityNotFoundException_whenNotFound() {
    UUID id = UUID.randomUUID();
    when(userRepository.findById(id)).thenReturn(Optional.empty());

    UserUpdateDTO dto = UserUpdateDTO.builder()
        .displayName("Updated Name")
        .build();

    assertThatThrownBy(() -> userService.updateUser(id, dto))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessageContaining(id.toString());
  }

  @Test
  void updateUser_partialUpdate_preservesUnchangedFields() {
    UUID id = UUID.randomUUID();
    User user = sampleUser();
    when(userRepository.findById(id)).thenReturn(Optional.of(user));

    UserUpdateDTO dto = UserUpdateDTO.builder()
        .displayName("Partially Updated")
        .build();

    UserResponseDTO result = userService.updateUser(id, dto);

    assertThat(result.getDisplayName()).isEqualTo("Partially Updated");
    assertThat(result.getEmail()).isEqualTo("test@example.com");
  }

  @Test
  void updateUser_syncsDisplayNameToProfile() {
    UUID id = UUID.randomUUID();
    User user = sampleUser();
    UserProfile profile = new UserProfile();
    profile.setDisplayName("Old Name");

    when(userRepository.findById(id)).thenReturn(Optional.of(user));
    when(userProfileRepository.findByUser(user)).thenReturn(Optional.of(profile));

    UserUpdateDTO dto = UserUpdateDTO.builder()
        .displayName("New Name")
        .build();

    userService.updateUser(id, dto);

    assertThat(profile.getDisplayName()).isEqualTo("New Name");
    verify(userProfileRepository).save(profile);
  }

  @Test
  void updateUser_doesNotThrow_whenProfileMissing() {
    UUID id = UUID.randomUUID();
    User user = sampleUser();

    when(userRepository.findById(id)).thenReturn(Optional.of(user));
    when(userProfileRepository.findByUser(user)).thenReturn(Optional.empty());

    UserUpdateDTO dto = UserUpdateDTO.builder()
        .displayName("Updated Name")
        .build();

    assertThatCode(() -> userService.updateUser(id, dto))
        .doesNotThrowAnyException();

    verify(userRepository).save(user);
    verify(userProfileRepository, never()).save(any());
  }

  @Test
  void updateStudentByInstructor_returnsUpdatedDTO_whenAuthorized() {
    UUID instructorId = UUID.randomUUID();
    UUID studentId = UUID.randomUUID();
    UUID classroomId = UUID.randomUUID();

    Role instructorRole = Role.builder()
        .id(UUID.randomUUID())
        .roleName("instructor")
        .build();

    User instructor = User.builder()
        .id(instructorId)
        .displayName("Instructor")
        .email("instructor@ntnu.no")
        .roles(List.of(instructorRole))
        .build();

    User student = User.builder()
        .id(studentId)
        .displayName("Student")
        .email("student@ntnu.no")
        .roles(List.of())
        .build();

    UserProfile studentProfile = new UserProfile();
    studentProfile.setDisplayName("Old Name");

    Classroom classroom = Classroom.builder()
        .id(classroomId)
        .title("Test Classroom")
        .build();

    ClassroomStudent studentMembership = ClassroomStudent.builder()
        .classroom(classroom)
        .user(student)
        .build();

    ClassroomInstructor instructorMembership = ClassroomInstructor.builder()
        .id(ClassroomInstructorId.builder()
            .classroomId(classroomId)
            .userId(instructorId)
            .build())
        .classroom(classroom)
        .user(instructor)
        .build();

    when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
    when(classroomStudentRepository.findByUser(student)).thenReturn(Optional.of(studentMembership));
    when(classroomRepository.findByIdAndDeletedAtIsNull(classroomId)).thenReturn(Optional.of(classroom));
    when(classroomInstructorRepository.existsByClassroomAndUser(classroom, instructor)).thenReturn(true);
    when(userProfileRepository.findByUser(student)).thenReturn(Optional.of(studentProfile));

    UserUpdateDTO dto = UserUpdateDTO.builder()
        .displayName("Updated Student Name")
        .build();

    UserResponseDTO result = userService.updateStudentByInstructor(studentId, dto, instructor);

    assertThat(result.getDisplayName()).isEqualTo("Updated Student Name");
    assertThat(studentProfile.getDisplayName()).isEqualTo("Updated Student Name");
  }

  @Test
  void updateStudentByInstructor_throwsAccessDeniedException_whenNotInstructor() {
    UUID studentId = UUID.randomUUID();

    Role studentRole = Role.builder()
        .id(UUID.randomUUID())
        .roleName("student")
        .build();

    User student = User.builder()
        .id(studentId)
        .displayName("Student")
        .roles(List.of(studentRole))
        .build();

    User nonInstructor = User.builder()
        .id(UUID.randomUUID())
        .roles(List.of(studentRole))
        .build();

    UserUpdateDTO dto = UserUpdateDTO.builder()
        .displayName("Updated")
        .build();

    assertThatThrownBy(() -> userService.updateStudentByInstructor(studentId, dto, nonInstructor))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("Only instructors");
  }

  @Test
  void updateStudentByInstructor_throwsAccessDeniedException_whenNotInSameClassroom() {
    UUID instructorId = UUID.randomUUID();
    UUID studentId = UUID.randomUUID();
    UUID classroomId = UUID.randomUUID();

    Role instructorRole = Role.builder()
        .id(UUID.randomUUID())
        .roleName("instructor")
        .build();

    User instructor = User.builder()
        .id(instructorId)
        .displayName("Instructor")
        .roles(List.of(instructorRole))
        .build();

    User student = User.builder()
        .id(studentId)
        .displayName("Student")
        .roles(List.of())
        .build();

    Classroom classroom = Classroom.builder()
        .id(classroomId)
        .title("Test Classroom")
        .build();

    ClassroomStudent studentMembership = ClassroomStudent.builder()
        .classroom(classroom)
        .user(student)
        .build();

    when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
    when(classroomStudentRepository.findByUser(student)).thenReturn(Optional.of(studentMembership));
    when(classroomRepository.findByIdAndDeletedAtIsNull(classroomId)).thenReturn(Optional.of(classroom));
    when(classroomInstructorRepository.existsByClassroomAndUser(classroom, instructor)).thenReturn(false);

    UserUpdateDTO dto = UserUpdateDTO.builder()
        .displayName("Updated")
        .build();

    assertThatThrownBy(() -> userService.updateStudentByInstructor(studentId, dto, instructor))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("own classrooms");
  }

  @Test
  void updateStudentByInstructor_throwsAccessDeniedException_whenClassroomNotFound() {
    UUID instructorId = UUID.randomUUID();
    UUID studentId = UUID.randomUUID();
    UUID classroomId = UUID.randomUUID();

    Role instructorRole = Role.builder()
        .id(UUID.randomUUID())
        .roleName("instructor")
        .build();

    User instructor = User.builder()
        .id(instructorId)
        .displayName("Instructor")
        .roles(List.of(instructorRole))
        .build();

    User student = User.builder()
        .id(studentId)
        .displayName("Student")
        .roles(List.of())
        .build();

    Classroom classroom = Classroom.builder()
        .id(classroomId)
        .title("Test Classroom")
        .build();

    ClassroomStudent studentMembership = ClassroomStudent.builder()
        .classroom(classroom)
        .user(student)
        .build();

    when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
    when(classroomStudentRepository.findByUser(student)).thenReturn(Optional.of(studentMembership));
    when(classroomRepository.findByIdAndDeletedAtIsNull(classroomId)).thenReturn(Optional.empty());

    UserUpdateDTO dto = UserUpdateDTO.builder()
        .displayName("Updated")
        .build();

    assertThatThrownBy(() -> userService.updateStudentByInstructor(studentId, dto, instructor))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("Classroom not found");
  }

  @Test
  void updateStudentByInstructor_throwsEntityNotFoundException_whenStudentNotFound() {
    UUID studentId = UUID.randomUUID();

    Role instructorRole = Role.builder()
        .id(UUID.randomUUID())
        .roleName("instructor")
        .build();

    User instructor = User.builder()
        .id(UUID.randomUUID())
        .displayName("Instructor")
        .roles(List.of(instructorRole))
        .build();

    when(userRepository.findById(studentId)).thenReturn(Optional.empty());

    UserUpdateDTO dto = UserUpdateDTO.builder()
        .displayName("Updated")
        .build();

    assertThatThrownBy(() -> userService.updateStudentByInstructor(studentId, dto, instructor))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessageContaining(studentId.toString());
  }

  @Test
  void updateStudentByInstructor_throwsEntityNotFoundException_whenStudentNotInClassroom() {
    UUID studentId = UUID.randomUUID();

    Role instructorRole = Role.builder()
        .id(UUID.randomUUID())
        .roleName("instructor")
        .build();

    User instructor = User.builder()
        .id(UUID.randomUUID())
        .displayName("Instructor")
        .roles(List.of(instructorRole))
        .build();

    User student = User.builder()
        .id(studentId)
        .displayName("Student")
        .roles(List.of())
        .build();

    when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
    when(classroomStudentRepository.findByUser(student)).thenReturn(Optional.empty());

    UserUpdateDTO dto = UserUpdateDTO.builder()
        .displayName("Updated")
        .build();

    assertThatThrownBy(() -> userService.updateStudentByInstructor(studentId, dto, instructor))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessageContaining("not in any classroom");
  }
}