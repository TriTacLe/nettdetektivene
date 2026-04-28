package ntnu.no.backend.service;

import jakarta.persistence.EntityNotFoundException;
import ntnu.no.backend.dto.notebook.NotebookEntryRequestDTO;
import ntnu.no.backend.dto.notebook.NotebookEntryResponseDTO;
import ntnu.no.backend.model.Classroom;
import ntnu.no.backend.model.NotebookEntry;
import ntnu.no.backend.model.auth.User;
import ntnu.no.backend.model.classroom.ClassroomInstructor;
import ntnu.no.backend.model.classroom.ClassroomInstructorId;
import ntnu.no.backend.model.Notification;
import ntnu.no.backend.repository.ClassroomInstructorRepository;
import ntnu.no.backend.repository.ClassroomRepository;
import ntnu.no.backend.repository.ClassroomStudentRepository;
import ntnu.no.backend.repository.NotebookEntryRepository;
import ntnu.no.backend.repository.NotificationRepository;
import ntnu.no.backend.repository.UserRepository;
import ntnu.no.backend.util.NotebookTestFactory;
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
 * Unit tests for {@link NotebookService}.
 * Verifies CRUD logic and filtering without hitting the database.
 *
 * @author tri
 * @version 0.1
 * @since 0.1
 */
@ExtendWith(MockitoExtension.class)
class NotebookServiceTest {

  @Mock
  NotebookEntryRepository notebookEntryRepository;

  @Mock
  ClassroomRepository classroomRepository;

  @Mock
  UserRepository userRepository;

  @Mock
  ClassroomStudentRepository classroomStudentRepository;

  @Mock
  ClassroomInstructorRepository classroomInstructorRepository;

  @Mock
  NotificationRepository notificationRepository;

  @InjectMocks
  NotebookService notebookService;

  private User buildUser() {
    return User.builder().id(UUID.randomUUID()).displayName("Tri").build();
  }

  private Classroom buildClassroom() {
    return Classroom.builder().id(UUID.randomUUID()).build();
  }

  /**
   * Verifies that a notebook entry is saved and returned as a DTO.
   */
  @Test
  void createNotebook_savesAndReturnsDTO() {
    UUID classroomId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    User user = User.builder().id(userId).displayName("Tri").build();
    Classroom classroom = Classroom.builder().id(classroomId).build();
    NotebookEntryRequestDTO dto = NotebookTestFactory.requestDTO();
    NotebookEntry saved = NotebookTestFactory.entity(UUID.randomUUID(), user, classroom);

    when(classroomRepository.findByIdAndDeletedAtIsNull(classroomId)).thenReturn(Optional.of(classroom));
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(classroomStudentRepository.findById(any())).thenReturn(
        Optional.of(ntnu.no.backend.model.classroom.ClassroomStudent.builder()
            .isAllowed(true)
            .build())
    );
    when(notebookEntryRepository.save(any())).thenReturn(saved);

    NotebookEntryResponseDTO result = notebookService.createNotebook(classroomId, dto, userId);

    assertThat(result.stopLocation()).isEqualTo("stop_3");
    assertThat(result.userId()).isEqualTo(userId);
    verify(notebookEntryRepository).save(any(NotebookEntry.class));
  }

  /**
   * Verifies that {@link EntityNotFoundException} is thrown when the classroom does not exist.
   */
  @Test
  void createNotebook_throwsEntityNotFoundException_whenClassroomNotFound() {
    UUID classroomId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    when(classroomRepository.findByIdAndDeletedAtIsNull(classroomId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> notebookService.createNotebook(classroomId, NotebookTestFactory.requestDTO(), userId))
        .isInstanceOf(EntityNotFoundException.class);
  }

  /**
   * Verifies that the updated fields are persisted and returned.
   */
  @Test
  void updateNotebook_updatesAndReturnsDTO() {
    UUID entryId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    User user = User.builder().id(userId).displayName("Tri").build();
    Classroom classroom = buildClassroom();
    NotebookEntry existing = NotebookTestFactory.entity(entryId, user, classroom);
    NotebookEntryRequestDTO dto = NotebookEntryRequestDTO.builder()
        .stopLocation("stop_5")
        .studentReflection("Updated reflection.")
        .build();
    NotebookEntry updated = NotebookTestFactory.entity(entryId, user, classroom);
    updated.setStopLocation("stop_5");
    updated.setStudentReflection("Updated reflection.");

    when(notebookEntryRepository.findById(entryId)).thenReturn(Optional.of(existing));
    when(notebookEntryRepository.save(any())).thenReturn(updated);

    NotebookEntryResponseDTO result = notebookService.updateNotebook(dto, entryId, userId);

    assertThat(result.stopLocation()).isEqualTo("stop_5");
    verify(notebookEntryRepository).save(existing);
  }

  /**
   * Verifies that {@link EntityNotFoundException} is thrown when the entry does not exist.
   */
  @Test
  void updateNotebook_throwsEntityNotFoundException_whenNotFound() {
    UUID entryId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    when(notebookEntryRepository.findById(entryId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> notebookService.updateNotebook(NotebookTestFactory.requestDTO(), entryId, userId))
        .isInstanceOf(EntityNotFoundException.class);
  }

  /**
   * Verifies that the entry is deleted when found.
   */
  @Test
  void deleteNotebook_deletesEntry_whenFound() {
    UUID entryId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    NotebookEntry entry = NotebookTestFactory.entity(entryId, User.builder().id(userId).build(), buildClassroom());

    when(notebookEntryRepository.findById(entryId)).thenReturn(Optional.of(entry));

    notebookService.deleteNotebook(entryId, userId);

    verify(notebookEntryRepository).delete(entry);
  }

  /**
   * Verifies that {@link EntityNotFoundException} is thrown when deleting a non-existent entry.
   */
  @Test
  void deleteNotebook_throwsEntityNotFoundException_whenNotFound() {
    UUID entryId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    when(notebookEntryRepository.findById(entryId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> notebookService.deleteNotebook(entryId, userId))
        .isInstanceOf(EntityNotFoundException.class);
  }

  /**
   * Verifies that only the target student's entries are returned.
   */
  @Test
  void getNotebookByClassroomAndUser_returnsOnlyStudentEntries() {
    UUID classroomId = UUID.randomUUID();
    User tri = User.builder().id(UUID.randomUUID()).displayName("Tri").build();
    User nora = User.builder().id(UUID.randomUUID()).displayName("Nora").build();
    Classroom classroom = Classroom.builder().id(classroomId).build();

    NotebookEntry triEntry = NotebookTestFactory.entity(UUID.randomUUID(), tri, classroom);
    NotebookEntry noraEntry = NotebookTestFactory.entity(UUID.randomUUID(), nora, classroom);

    when(notebookEntryRepository.findByClassroom_IdAndUser_Id(classroomId, tri.getId()))
        .thenReturn(List.of(triEntry));

    List<NotebookEntryResponseDTO> result = notebookService.getNotebookByClassroomAndUser(classroomId, tri.getId());

    assertThat(result).hasSize(1);
    assertThat(result.get(0).userId()).isEqualTo(tri.getId());
  }

  /**
   * Verifies that all entries in the classroom are returned for the instructor view.
   */
  @Test
  void getNotebooksByClassroom_returnsAllEntries() {
    UUID classroomId = UUID.randomUUID();
    User tri = User.builder().id(UUID.randomUUID()).displayName("Tri").build();
    User nora = User.builder().id(UUID.randomUUID()).displayName("Nora").build();
    Classroom classroom = Classroom.builder().id(classroomId).build();

    when(notebookEntryRepository.findByClassroom_Id(classroomId))
        .thenReturn(List.of(
            NotebookTestFactory.entity(UUID.randomUUID(), tri, classroom),
            NotebookTestFactory.entity(UUID.randomUUID(), nora, classroom)
        ));

    List<NotebookEntryResponseDTO> result = notebookService.getNotebooksByClassroom(classroomId);

    assertThat(result).hasSize(2);
  }

  /**
   * Verifies that createNotebook throws IllegalArgumentException when reflection contains censored word.
   */
  @Test
  void createNotebook_throwsIllegalArgument_whenReflectionContainsCensoredWord() {
    UUID classroomId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    User user = User.builder().id(userId).displayName("Tri").build();
    Classroom classroom = Classroom.builder().id(classroomId).build();
    NotebookEntryRequestDTO dto = NotebookEntryRequestDTO.builder()
        .stopLocation("stop_3")
        .studentReflection("dette var faen meg vanskelig")
        .build();

    when(classroomRepository.findByIdAndDeletedAtIsNull(classroomId)).thenReturn(Optional.of(classroom));
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(classroomStudentRepository.findById(any())).thenReturn(
        Optional.of(ntnu.no.backend.model.classroom.ClassroomStudent.builder()
            .isAllowed(true)
            .build())
    );

    assertThatThrownBy(() -> notebookService.createNotebook(classroomId, dto, userId))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("upassende ord");
  }

  /**
   * Verifies that createNotebook notifies instructors when censored word is used.
   */
  @Test
  void createNotebook_notifiesInstructors_whenReflectionContainsCensoredWord() {
    UUID classroomId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    UUID instructorId = UUID.randomUUID();
    User user = User.builder().id(userId).displayName("Tri").build();
    Classroom classroom = Classroom.builder().id(classroomId).build();
    User instructorUser = User.builder().id(instructorId).displayName("Nora").build();
    NotebookEntryRequestDTO dto = NotebookEntryRequestDTO.builder()
        .stopLocation("stop_3")
        .studentReflection("dette var faen meg vanskelig")
        .build();

    ClassroomInstructor instructor = ClassroomInstructor.builder()
        .id(ClassroomInstructorId.builder()
            .classroomId(classroomId)
            .userId(instructorId)
            .build())
        .classroom(classroom)
        .user(instructorUser)
        .build();

    when(classroomRepository.findByIdAndDeletedAtIsNull(classroomId)).thenReturn(Optional.of(classroom));
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(classroomStudentRepository.findById(any())).thenReturn(
        Optional.of(ntnu.no.backend.model.classroom.ClassroomStudent.builder()
            .isAllowed(true)
            .build())
    );
    when(classroomInstructorRepository.findAllByClassroom(classroom)).thenReturn(List.of(instructor));

    assertThatThrownBy(() -> notebookService.createNotebook(classroomId, dto, userId))
        .isInstanceOf(IllegalArgumentException.class);

    verify(notificationRepository).save(any(Notification.class));
  }

  /**
   * Verifies that updateNotebook throws IllegalArgumentException when reflection contains censored word.
   */
  @Test
  void updateNotebook_throwsIllegalArgument_whenReflectionContainsCensoredWord() {
    UUID entryId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    User user = User.builder().id(userId).displayName("Tri").build();
    Classroom classroom = buildClassroom();
    NotebookEntry existing = NotebookTestFactory.entity(entryId, user, classroom);
    NotebookEntryRequestDTO dto = NotebookEntryRequestDTO.builder()
        .stopLocation("stop_3")
        .studentReflection("dette var helvete vanskelig")
        .build();

    when(notebookEntryRepository.findById(entryId)).thenReturn(Optional.of(existing));

    assertThatThrownBy(() -> notebookService.updateNotebook(dto, entryId, userId))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("upassende ord");
  }
}
