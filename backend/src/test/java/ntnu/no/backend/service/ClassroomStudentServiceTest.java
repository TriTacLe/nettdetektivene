package ntnu.no.backend.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import ntnu.no.backend.dto.classroom.ClassroomStudentResponseDTO;
import ntnu.no.backend.model.Classroom;
import ntnu.no.backend.model.Institution;
import ntnu.no.backend.model.auth.User;
import ntnu.no.backend.model.auth.UserProfile;
import ntnu.no.backend.model.classroom.ClassroomStudent;
import ntnu.no.backend.model.classroom.ClassroomStudentId;
import ntnu.no.backend.model.enums.NameChangeStatus;
import ntnu.no.backend.model.classroom.ClassroomInstructor;
import ntnu.no.backend.model.classroom.ClassroomInstructorId;
import ntnu.no.backend.repository.ClassroomInstructorRepository;
import ntnu.no.backend.repository.ClassroomRepository;
import ntnu.no.backend.repository.ClassroomStudentRepository;
import ntnu.no.backend.repository.ClassroomTaskRepository;
import ntnu.no.backend.repository.NotificationRepository;
import ntnu.no.backend.repository.UserProfileRepository;
import ntnu.no.backend.repository.UserTaskUnlockedRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
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
 * Unit tests for {@link ClassroomStudentService}.
 * Verifies student membership operations and feideId mapping.
 *
 * @author tri, norakirknes
 * @version 0.2
 * @since 0.1
 */
@ExtendWith(MockitoExtension.class)
class ClassroomStudentServiceTest {

  @Mock
  private ClassroomStudentRepository classroomStudentRepository;

  @Mock
  private ClassroomRepository classroomRepository;

  @Mock
  private ClassroomInstructorRepository classroomInstructorRepository;

  @Mock
  private UserProfileRepository userProfileRepository;

  @Mock
  private ClassroomTaskRepository classroomTaskRepository;

  @Mock
  private UserTaskUnlockedRepository userTaskUnlockedRepository;

  @Mock
  private NotificationRepository notificationRepository;

  @Mock
  private JdbcTemplate jdbcTemplate;

  @Mock
  private EntityManager entityManager;

  @InjectMocks
  private ClassroomStudentService classroomStudentService;

  private User createUser(UUID id, String displayName) {
    return User.builder()
      .id(id)
      .displayName(displayName)
      .email("student@ntnu.no")
      .build();
  }

  private UserProfile createUserProfile(User user, String feideId) {
    return UserProfile.builder()
      .user(user)
      .displayName(user.getDisplayName())
      .feideId(feideId)
      .build();
  }

  private Classroom createClassroom(UUID id) {
    return Classroom.builder()
      .id(id)
      .title("Cybersecurity 101")
      .classDesc("Introduction to cybersecurity")
      .joinCode("ABC12")
      .institution(Institution.builder().instName("NTNU").build())
      .createdBy(createUser(UUID.randomUUID(), "Creator"))
      .isGameEnabled(false)
      .build();
  }

  private ClassroomStudent createClassroomStudent(UUID classroomId, UUID userId, String displayName, boolean allowed) {
    User user = createUser(userId, displayName);
    Classroom classroom = createClassroom(classroomId);

    return ClassroomStudent.builder()
      .id(ClassroomStudentId.builder()
        .classroomId(classroomId)
        .userId(userId)
        .build())
      .classroom(classroom)
      .user(user)
      .displayName(displayName)
      .isAllowed(allowed)
      .nameChangeStatus(NameChangeStatus.NONE)
      .build();
  }

  /**
   * Verifies that getMyClassroom only returns classroom when student is approved.
   */
  @Test
  void getMyClassroom_returnsClassroom_whenStudentIsAllowed() {
    UUID classroomId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    User user = createUser(userId, "Nora");
    ClassroomStudent student = createClassroomStudent(classroomId, userId, "Nora", true);

    when(classroomStudentRepository.findByUserAndIsAllowedIsTrue(user)).thenReturn(Optional.of(student));

    var result = classroomStudentService.getMyClassroom(user);

    assertThat(result).isNotNull();
    assertThat(result.id()).isEqualTo(classroomId);
  }

  /**
   * Verifies that getMyClassroom throws when student is not approved.
   */
  @Test
  void getMyClassroom_throwsEntityNotFoundException_whenStudentNotAllowed() {
    UUID userId = UUID.randomUUID();
    User user = createUser(userId, "Nora");

    when(classroomStudentRepository.findByUserAndIsAllowedIsTrue(user)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> classroomStudentService.getMyClassroom(user))
      .isInstanceOf(EntityNotFoundException.class)
      .hasMessageContaining("not in any classroom");
  }

  /**
   * Verifies that getMyMembership returns membership regardless of approval status.
   */
  @Test
  void getMyMembership_returnsDTO_whenMembershipExists() {
    UUID classroomId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    User user = createUser(userId, "Anine");
    ClassroomStudent student = createClassroomStudent(classroomId, userId, "Anine", false);
    UserProfile profile = createUserProfile(student.getUser(), "anine@ntnu.no");

    when(classroomStudentRepository.findByUser(user)).thenReturn(Optional.of(student));
    when(userProfileRepository.findByUser(student.getUser())).thenReturn(Optional.of(profile));

    ClassroomStudentResponseDTO result = classroomStudentService.getMyMembership(user);

    assertThat(result.userId()).isEqualTo(userId);
    assertThat(result.displayName()).isEqualTo("Anine");
    assertThat(result.isAllowed()).isFalse();
  }

  /**
   * Verifies that getMyMembership throws when no membership exists.
   */
  @Test
  void getMyMembership_throwsEntityNotFoundException_whenNoMembership() {
    UUID userId = UUID.randomUUID();
    User user = createUser(userId, "Anine");

    when(classroomStudentRepository.findByUser(user)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> classroomStudentService.getMyMembership(user))
      .isInstanceOf(EntityNotFoundException.class)
      .hasMessageContaining("no membership");
  }

  /**
   * Verifies that join notifies all instructors of the classroom.
   */
  @Test
  void join_notifiesInstructors() {
    UUID classroomId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    UUID instructorId = UUID.randomUUID();
    String joinCode = "ABC12";

    User user = createUser(userId, "Tri");
    User instructorUser = createUser(instructorId, "Nora");
    Classroom classroom = createClassroom(classroomId);
    UserProfile profile = createUserProfile(user, "tri@ntnu.no");

    ClassroomInstructor instructor = ClassroomInstructor.builder()
      .id(ClassroomInstructorId.builder()
        .classroomId(classroomId)
        .userId(instructorId)
        .build())
      .classroom(classroom)
      .user(instructorUser)
      .build();

    when(classroomStudentRepository.existsByUser(user)).thenReturn(false);
    when(classroomRepository.findByJoinCodeAndDeletedAtIsNull(joinCode)).thenReturn(Optional.of(classroom));
    when(userProfileRepository.findByUser(user)).thenReturn(Optional.of(profile));
    when(classroomStudentRepository.save(any(ClassroomStudent.class))).thenAnswer(i -> i.getArgument(0));
    when(classroomInstructorRepository.findAllByClassroom(classroom)).thenReturn(List.of(instructor));

    classroomStudentService.join(joinCode, user);

    verify(notificationRepository).save(any(ntnu.no.backend.model.Notification.class));
  }

  /**
   * Verifies that feideId is correctly mapped in the DTO when a student has a UserProfile.
   */
  @Test
  void getStudents_returnsDTOs_withFeideId() {
    UUID classroomId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    UUID instructorId = UUID.randomUUID();
    String expectedFeideId = "feide123@ntnu.no";

    User instructor = createUser(instructorId, "Nora");
    ClassroomStudent student = createClassroomStudent(classroomId, userId, "Tri", true);
    UserProfile profile = createUserProfile(student.getUser(), expectedFeideId);

    when(classroomInstructorRepository.existsById(any())).thenReturn(true);
    when(classroomRepository.findByIdAndDeletedAtIsNull(classroomId)).thenReturn(Optional.of(student.getClassroom()));
    when(classroomStudentRepository.findAllByClassroom(student.getClassroom())).thenReturn(List.of(student));
    when(userProfileRepository.findByUser(student.getUser())).thenReturn(Optional.of(profile));

    List<ClassroomStudentResponseDTO> result = classroomStudentService.getStudents(classroomId, instructor);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).feideId()).isEqualTo(expectedFeideId);
    assertThat(result.get(0).userId()).isEqualTo(userId);
    assertThat(result.get(0).displayName()).isEqualTo("Tri");
  }

  /**
   * Verifies that feideId is null in the DTO when a student has no UserProfile.
   */
  @Test
  void getStudents_returnsDTOs_withNullFeideId_whenNoProfile() {
    UUID classroomId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    UUID instructorId = UUID.randomUUID();

    User instructor = createUser(instructorId, "Nora");
    ClassroomStudent student = createClassroomStudent(classroomId, userId, "Tri", true);

    when(classroomInstructorRepository.existsById(any())).thenReturn(true);
    when(classroomRepository.findByIdAndDeletedAtIsNull(classroomId)).thenReturn(Optional.of(student.getClassroom()));
    when(classroomStudentRepository.findAllByClassroom(student.getClassroom())).thenReturn(List.of(student));
    when(userProfileRepository.findByUser(student.getUser())).thenReturn(Optional.empty());

    List<ClassroomStudentResponseDTO> result = classroomStudentService.getStudents(classroomId, instructor);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).feideId()).isNull();
  }

  /**
   * Verifies that approved students are returned with feideId mapped.
   */
  @Test
  void getApprovedStudents_returnsApprovedOnly_withFeideId() {
    UUID classroomId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    UUID instructorId = UUID.randomUUID();
    String expectedFeideId = "feide456@ntnu.no";

    User instructor = createUser(instructorId, "Nora");
    ClassroomStudent student = createClassroomStudent(classroomId, userId, "Anine", true);
    UserProfile profile = createUserProfile(student.getUser(), expectedFeideId);

    when(classroomInstructorRepository.existsById(any())).thenReturn(true);
    when(classroomRepository.findByIdAndDeletedAtIsNull(classroomId)).thenReturn(Optional.of(student.getClassroom()));
    when(classroomStudentRepository.findAllByClassroomAndIsAllowedIsTrue(student.getClassroom())).thenReturn(List.of(student));
    when(userProfileRepository.findByUser(student.getUser())).thenReturn(Optional.of(profile));

    List<ClassroomStudentResponseDTO> result = classroomStudentService.getApprovedStudents(classroomId, instructor);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).isAllowed()).isTrue();
    assertThat(result.get(0).feideId()).isEqualTo(expectedFeideId);
  }

  /**
   * Verifies that pending students are returned with feideId mapped.
   */
  @Test
  void getPendingStudents_returnsPendingOnly_withFeideId() {
    UUID classroomId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    UUID instructorId = UUID.randomUUID();
    String expectedFeideId = "pending@ntnu.no";

    User instructor = createUser(instructorId, "Nora");
    ClassroomStudent student = createClassroomStudent(classroomId, userId, "PendingStudent", false);
    UserProfile profile = createUserProfile(student.getUser(), expectedFeideId);

    when(classroomInstructorRepository.existsById(any())).thenReturn(true);
    when(classroomRepository.findByIdAndDeletedAtIsNull(classroomId)).thenReturn(Optional.of(student.getClassroom()));
    when(classroomStudentRepository.findAllByClassroomAndIsAllowedIsFalse(student.getClassroom())).thenReturn(List.of(student));
    when(userProfileRepository.findByUser(student.getUser())).thenReturn(Optional.of(profile));

    List<ClassroomStudentResponseDTO> result = classroomStudentService.getPendingStudents(classroomId, instructor);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).isAllowed()).isFalse();
    assertThat(result.get(0).feideId()).isEqualTo(expectedFeideId);
  }

  /**
   * Verifies that join creates a student and returns DTO with feideId.
   */
  @Test
  void join_createsStudentAndReturnsDTO_withFeideId() {
    UUID classroomId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    String joinCode = "ABC12";
    String expectedFeideId = "newstudent@ntnu.no";

    User user = createUser(userId, "NewStudent");
    Classroom classroom = createClassroom(classroomId);
    UserProfile profile = createUserProfile(user, expectedFeideId);

    when(classroomStudentRepository.existsByUser(user)).thenReturn(false);
    when(classroomRepository.findByJoinCodeAndDeletedAtIsNull(joinCode)).thenReturn(Optional.of(classroom));
    when(userProfileRepository.findByUser(user)).thenReturn(Optional.of(profile));
    when(classroomStudentRepository.save(any(ClassroomStudent.class))).thenAnswer(i -> i.getArgument(0));

    ClassroomStudentResponseDTO result = classroomStudentService.join(joinCode, user);

    assertThat(result.userId()).isEqualTo(userId);
    assertThat(result.classroomId()).isEqualTo(classroomId);
    assertThat(result.displayName()).isEqualTo("NewStudent");
    assertThat(result.feideId()).isEqualTo(expectedFeideId);
    assertThat(result.isAllowed()).isFalse();
    verify(classroomStudentRepository).save(any(ClassroomStudent.class));
  }

  /**
   * Verifies that approve returns DTO with feideId mapped.
   */
  @Test
  void approve_returnsDTO_withFeideId() {
    UUID classroomId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    UUID instructorId = UUID.randomUUID();
    String expectedFeideId = "approved@ntnu.no";

    User instructor = createUser(instructorId, "Nora");
    ClassroomStudent student = createClassroomStudent(classroomId, userId, "ToApprove", false);
    UserProfile profile = createUserProfile(student.getUser(), expectedFeideId);

    when(classroomInstructorRepository.existsById(any())).thenReturn(true);
    when(classroomStudentRepository.findById(any(ClassroomStudentId.class))).thenReturn(Optional.of(student));
    when(classroomTaskRepository.findAllByClassroom_IdAndIsActiveTrueOrderBySortOrderAsc(any())).thenReturn(List.of());
    when(userProfileRepository.findByUser(student.getUser())).thenReturn(Optional.of(profile));

    ClassroomStudentResponseDTO result = classroomStudentService.approve(classroomId, userId, instructor);

    assertThat(result.isAllowed()).isTrue();
    assertThat(result.feideId()).isEqualTo(expectedFeideId);
  }

  /**
   * Verifies that remove deletes the student.
   */
  @Test
  void remove_deletesStudent() {
    UUID classroomId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    UUID instructorId = UUID.randomUUID();

    User instructor = createUser(instructorId, "Nora");
    ClassroomStudent student = createClassroomStudent(classroomId, userId, "ToRemove", true);

    when(classroomInstructorRepository.existsById(any())).thenReturn(true);
    when(classroomStudentRepository.findById(any(ClassroomStudentId.class))).thenReturn(Optional.of(student));

    classroomStudentService.remove(classroomId, userId, instructor);

    verify(classroomStudentRepository).delete(student);
  }

  /**
   * Verifies that access is denied when user is not an instructor.
   */
  @Test
  void getStudents_throwsAccessDeniedException_whenNotInstructor() {
    UUID classroomId = UUID.randomUUID();
    UUID instructorId = UUID.randomUUID();
    User instructor = createUser(instructorId, "NotAnInstructor");

    when(classroomInstructorRepository.existsById(any())).thenReturn(false);

    assertThatThrownBy(() -> classroomStudentService.getStudents(classroomId, instructor))
      .isInstanceOf(AccessDeniedException.class)
      .hasMessageContaining("not an instructor");
  }

  /**
   * Verifies that EntityNotFoundException is thrown when classroom not found.
   */
  @Test
  void getStudents_throwsEntityNotFoundException_whenClassroomNotFound() {
    UUID classroomId = UUID.randomUUID();
    UUID instructorId = UUID.randomUUID();
    User instructor = createUser(instructorId, "Nora");

    when(classroomInstructorRepository.existsById(any())).thenReturn(true);
    when(classroomRepository.findByIdAndDeletedAtIsNull(classroomId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> classroomStudentService.getStudents(classroomId, instructor))
      .isInstanceOf(EntityNotFoundException.class)
      .hasMessageContaining("Classroom not found");
  }
}
