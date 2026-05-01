package ntnu.no.backend.service;

import jakarta.persistence.EntityNotFoundException;
import ntnu.no.backend.dto.classroom.ClassroomInstructorResponseDTO;
import ntnu.no.backend.dto.classroom.ClassroomResponseDTO;
import ntnu.no.backend.dto.task.ClassroomTaskResponseDTO;
import ntnu.no.backend.exception.ConflictException;
import ntnu.no.backend.model.Classroom;
import ntnu.no.backend.model.ClassroomTask;
import ntnu.no.backend.model.Task;
import ntnu.no.backend.model.auth.User;
import ntnu.no.backend.model.classroom.ClassroomInstructor;
import ntnu.no.backend.repository.ClassroomInstructorRepository;
import ntnu.no.backend.repository.ClassroomRepository;
import ntnu.no.backend.repository.ClassroomStudentRepository;
import ntnu.no.backend.repository.ClassroomTaskRepository;
import ntnu.no.backend.repository.InstitutionRepository;
import ntnu.no.backend.repository.TaskBestScoreRepository;
import ntnu.no.backend.repository.TaskRepository;
import ntnu.no.backend.repository.UserRepository;
import ntnu.no.backend.repository.UserTaskUnlockedRepository;
import ntnu.no.backend.util.ClassroomTestFactory;
import ntnu.no.backend.util.TaskTestFactory;
import org.springframework.security.access.AccessDeniedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ClassroomService}.
 *
 * @author tri
 * @version 0.1
 * @since 0.1
 */
@ExtendWith(MockitoExtension.class)
class ClassroomServiceTest {

  @Mock
  private ClassroomRepository classroomRepository;

  @Mock
  private InstitutionRepository institutionRepository;

  @Mock
  private ClassroomTaskRepository classroomTaskRepository;

  @Mock
  private UserTaskUnlockedRepository userTaskUnlockedRepository;

  @Mock
  private TaskRepository taskRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private ClassroomStudentRepository classroomStudentRepository;

  @Mock
  private ClassroomInstructorRepository classroomInstructorRepository;

  @Mock
  private TaskBestScoreRepository taskBestScoreRepository;

  @Mock
  private NotificationService notificationService;

  @InjectMocks
  private ClassroomService classroomService;

  @Test
  void getAllClassroom_returnsMappedDTOs() {
    when(classroomRepository.findAllByDeletedAtIsNull()).thenReturn(List.of(ClassroomTestFactory.classroom()));

    List<ClassroomResponseDTO> result = classroomService.getAllClassroom();

    assertThat(result).hasSize(1);
    assertThat(result.get(0).title()).isEqualTo("Cybersecurity 101");
  }

  @Test
  void getClassroom_returnsDTO_whenFound() {
    UUID id = UUID.randomUUID();
    when(classroomRepository.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.of(ClassroomTestFactory.classroom()));

    ClassroomResponseDTO result = classroomService.getClassroom(id);

    assertThat(result.title()).isEqualTo("Cybersecurity 101");
  }

  @Test
  void getClassroom_throwsEntityNotFoundException_whenNotFound() {
    UUID id = UUID.randomUUID();
    when(classroomRepository.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> classroomService.getClassroom(id))
      .isInstanceOf(EntityNotFoundException.class)
      .hasMessageContaining(id.toString());
  }

  @Test
  void createClassroom_savesAndReturnsDTO() {
    User currentUser = ClassroomTestFactory.user();
    when(institutionRepository.findById(any())).thenReturn(Optional.of(ClassroomTestFactory.institution()));
    when(classroomRepository.save(any(Classroom.class))).thenAnswer(i -> i.getArgument(0));

    ClassroomResponseDTO result = classroomService.createClassroom(ClassroomTestFactory.requestDTO(), currentUser);

    assertThat(result.title()).isEqualTo("Cybersecurity 101");
    verify(classroomRepository).save(any(Classroom.class));
  }

  @Test
  void createClassroom_throwsEntityNotFoundException_whenInstitutionNotFound() {
    when(institutionRepository.findById(any())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> classroomService.createClassroom(ClassroomTestFactory.requestDTO(), ClassroomTestFactory.user()))
      .isInstanceOf(EntityNotFoundException.class);
  }

  @Test
  void updateClassroom_updatesAndReturnsDTO() {
    UUID id = UUID.randomUUID();
    when(classroomRepository.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.of(ClassroomTestFactory.classroom()));

    ClassroomResponseDTO result = classroomService.updateClassroom(
      ClassroomTestFactory.requestDTO(), id);

    assertThat(result.title()).isEqualTo("Cybersecurity 101");
  }

  @Test
  void updateClassroom_throwsEntityNotFoundException_whenNotFound() {
    UUID id = UUID.randomUUID();
    when(classroomRepository.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> classroomService.updateClassroom(ClassroomTestFactory.requestDTO(), id))
      .isInstanceOf(EntityNotFoundException.class)
      .hasMessageContaining(id.toString());
  }

  @Test
  void deleteClassroom_softDeletes_whenFound() {
    UUID id = UUID.randomUUID();
    Classroom classroom = ClassroomTestFactory.classroom();
    when(classroomRepository.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.of(classroom));

    classroomService.deleteClassroom(id);

    assertThat(classroom.getDeletedAt()).isNotNull();
    assertThat(classroom.getDeletedAt()).isBeforeOrEqualTo(LocalDateTime.now());
  }

  @Test
  void deleteClassroom_throwsEntityNotFoundException_whenNotFound() {
    UUID id = UUID.randomUUID();
    when(classroomRepository.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> classroomService.deleteClassroom(id))
      .isInstanceOf(EntityNotFoundException.class)
      .hasMessageContaining(id.toString());
  }

  @Test
  void getAllClassroomsByInstitution_returnsMappedDTOs() {
    UUID institutionId = UUID.randomUUID();
    when(classroomRepository.findAllByInstitution_IdAndDeletedAtIsNull(institutionId))
      .thenReturn(List.of(ClassroomTestFactory.classroom()));

    List<ClassroomResponseDTO> result = classroomService.getAllClassroomsByInstitution(institutionId);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).title()).isEqualTo("Cybersecurity 101");
  }

  @Test
  void createClassroomTask_savesAndReturnsDTO() {
    UUID classroomId = UUID.randomUUID();
    UUID taskId = UUID.randomUUID();
    Classroom classroom = Classroom.builder()
      .id(classroomId)
      .title("Cybersecurity 101")
      .classDesc("Introduction to cybersecurity")
      .joinCode("ABC12")
      .institution(ClassroomTestFactory.institution())
      .createdBy(ClassroomTestFactory.user())
      .isGameEnabled(false)
      .build();
    Task task = Task.builder()
      .id(taskId)
      .title("Spot the phishing email")
      .taskType(TaskTestFactory.task().getTaskType())
      .difficulty(1)
      .content("{}")
      .build();

    when(classroomRepository.findByIdAndDeletedAtIsNull(classroomId)).thenReturn(Optional.of(classroom));
    when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
    when(classroomTaskRepository.existsById(any())).thenReturn(false);
    when(classroomTaskRepository.save(any(ClassroomTask.class))).thenAnswer(i -> i.getArgument(0));

    ClassroomTaskResponseDTO result = classroomService.createClassroomTask(
      classroomId,
      ClassroomTestFactory.classroomTaskCreateRequestDTO(taskId)
    );

    assertThat(result.classroomId()).isEqualTo(classroomId);
    assertThat(result.taskId()).isEqualTo(taskId);
    assertThat(result.sortOrder()).isEqualTo((short) 1);
    verify(classroomTaskRepository).save(any(ClassroomTask.class));
  }

  @Test
  void createClassroomTask_throwsWhenAlreadyAssigned() {
    UUID classroomId = UUID.randomUUID();
    UUID taskId = UUID.randomUUID();
    when(classroomRepository.findByIdAndDeletedAtIsNull(classroomId))
      .thenReturn(Optional.of(ClassroomTestFactory.classroom()));
    when(taskRepository.findById(taskId)).thenReturn(Optional.of(TaskTestFactory.task()));
    when(classroomTaskRepository.existsById(any())).thenReturn(true);

    assertThatThrownBy(() -> classroomService.createClassroomTask(
      classroomId,
      ClassroomTestFactory.classroomTaskCreateRequestDTO(taskId)
    ))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("already assigned");
  }

  @Test
  void addInstructor_savesAndReturnsDTO() {
    UUID classroomId = UUID.randomUUID();
    Classroom classroom = Classroom.builder()
      .id(classroomId).title("Cybersecurity 101").joinCode("ABC12")
      .institution(ClassroomTestFactory.institution()).createdBy(ClassroomTestFactory.user())
      .build();
    User caller = ClassroomTestFactory.teacher("Nora", "nora@ntnu.no");
    User target = ClassroomTestFactory.teacher("Anine", "anine@ntnu.no");

    when(classroomRepository.findByIdAndDeletedAtIsNull(classroomId)).thenReturn(Optional.of(classroom));
    when(classroomInstructorRepository.existsByClassroomAndUser(classroom, caller)).thenReturn(true);
    when(userRepository.findByEmail("anine@ntnu.no")).thenReturn(Optional.of(target));
    when(classroomInstructorRepository.existsByClassroomAndUser(classroom, target)).thenReturn(false);
    when(classroomInstructorRepository.save(any(ClassroomInstructor.class))).thenAnswer(i -> i.getArgument(0));

    ClassroomInstructorResponseDTO result = classroomService.addInstructor(classroomId, "anine@ntnu.no", caller);

    assertThat(result.classroomId()).isEqualTo(classroomId);
    verify(classroomInstructorRepository).save(any(ClassroomInstructor.class));
    verify(notificationService).createTeacherAddedNotification(target, classroom);
  }

  @Test
  void addInstructor_throwsAccessDenied_whenCallerNotInstructor() {
    UUID classroomId = UUID.randomUUID();
    Classroom classroom = Classroom.builder()
      .id(classroomId).title("Cybersecurity 101").joinCode("ABC12")
      .institution(ClassroomTestFactory.institution()).createdBy(ClassroomTestFactory.user())
      .build();
    User caller = ClassroomTestFactory.teacher("Tri", "tri@ntnu.no");

    when(classroomRepository.findByIdAndDeletedAtIsNull(classroomId)).thenReturn(Optional.of(classroom));
    when(classroomInstructorRepository.existsByClassroomAndUser(classroom, caller)).thenReturn(false);

    assertThatThrownBy(() -> classroomService.addInstructor(classroomId, "anine@ntnu.no", caller))
      .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void addInstructor_throwsEntityNotFound_whenTargetNotFound() {
    UUID classroomId = UUID.randomUUID();
    Classroom classroom = Classroom.builder()
      .id(classroomId).title("Cybersecurity 101").joinCode("ABC12")
      .institution(ClassroomTestFactory.institution()).createdBy(ClassroomTestFactory.user())
      .build();
    User caller = ClassroomTestFactory.teacher("Nora", "nora@ntnu.no");

    when(classroomRepository.findByIdAndDeletedAtIsNull(classroomId)).thenReturn(Optional.of(classroom));
    when(classroomInstructorRepository.existsByClassroomAndUser(classroom, caller)).thenReturn(true);
    when(userRepository.findByEmail("unknown@ntnu.no")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> classroomService.addInstructor(classroomId, "unknown@ntnu.no", caller))
      .isInstanceOf(EntityNotFoundException.class);
  }

  @Test
  void addInstructor_throwsIllegalArgument_whenTargetNotTeacher() {
    UUID classroomId = UUID.randomUUID();
    Classroom classroom = Classroom.builder()
      .id(classroomId).title("Cybersecurity 101").joinCode("ABC12")
      .institution(ClassroomTestFactory.institution()).createdBy(ClassroomTestFactory.user())
      .build();
    User caller = ClassroomTestFactory.teacher("Nora", "nora@ntnu.no");
    User target = ClassroomTestFactory.student("Tri", "tri@ntnu.no");

    when(classroomRepository.findByIdAndDeletedAtIsNull(classroomId)).thenReturn(Optional.of(classroom));
    when(classroomInstructorRepository.existsByClassroomAndUser(classroom, caller)).thenReturn(true);
    when(userRepository.findByEmail("tri@ntnu.no")).thenReturn(Optional.of(target));

    assertThatThrownBy(() -> classroomService.addInstructor(classroomId, "tri@ntnu.no", caller))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("not a teacher");
  }

  @Test
  void addInstructor_throwsConflict_whenAlreadyInstructor() {
    UUID classroomId = UUID.randomUUID();
    Classroom classroom = Classroom.builder()
      .id(classroomId).title("Cybersecurity 101").joinCode("ABC12")
      .institution(ClassroomTestFactory.institution()).createdBy(ClassroomTestFactory.user())
      .build();
    User caller = ClassroomTestFactory.teacher("Nora", "nora@ntnu.no");
    User target = ClassroomTestFactory.teacher("Anine", "anine@ntnu.no");

    when(classroomRepository.findByIdAndDeletedAtIsNull(classroomId)).thenReturn(Optional.of(classroom));
    when(classroomInstructorRepository.existsByClassroomAndUser(classroom, caller)).thenReturn(true);
    when(userRepository.findByEmail("anine@ntnu.no")).thenReturn(Optional.of(target));
    when(classroomInstructorRepository.existsByClassroomAndUser(classroom, target)).thenReturn(true);

    assertThatThrownBy(() -> classroomService.addInstructor(classroomId, "anine@ntnu.no", caller))
      .isInstanceOf(ConflictException.class);
  }

  @Test
  void leaveClassroom_deletesInstructorRow() {
    UUID classroomId = UUID.randomUUID();
    Classroom classroom = Classroom.builder()
      .id(classroomId).title("Cybersecurity 101").joinCode("ABC12")
      .institution(ClassroomTestFactory.institution()).createdBy(ClassroomTestFactory.user())
      .build();
    User caller = ClassroomTestFactory.teacher("Nora", "nora@ntnu.no");

    when(classroomRepository.findByIdAndDeletedAtIsNull(classroomId)).thenReturn(Optional.of(classroom));
    when(classroomInstructorRepository.existsByClassroomAndUser(classroom, caller)).thenReturn(true);
    when(classroomInstructorRepository.countByClassroom(classroom)).thenReturn(2L);

    classroomService.leaveClassroom(classroomId, caller);

    verify(classroomInstructorRepository).deleteByClassroomAndUser(classroom, caller);
  }

  @Test
  void leaveClassroom_throwsConflict_whenLastInstructor() {
    UUID classroomId = UUID.randomUUID();
    Classroom classroom = Classroom.builder()
      .id(classroomId).title("Cybersecurity 101").joinCode("ABC12")
      .institution(ClassroomTestFactory.institution()).createdBy(ClassroomTestFactory.user())
      .build();
    User caller = ClassroomTestFactory.teacher("Nora", "nora@ntnu.no");

    when(classroomRepository.findByIdAndDeletedAtIsNull(classroomId)).thenReturn(Optional.of(classroom));
    when(classroomInstructorRepository.existsByClassroomAndUser(classroom, caller)).thenReturn(true);
    when(classroomInstructorRepository.countByClassroom(classroom)).thenReturn(1L);

    assertThatThrownBy(() -> classroomService.leaveClassroom(classroomId, caller))
      .isInstanceOf(ConflictException.class)
      .hasMessageContaining("at least one teacher required");
  }

  @Test
  void getInstructors_returnsMappedDTOs() {
    UUID classroomId = UUID.randomUUID();
    Classroom classroom = Classroom.builder()
      .id(classroomId).title("Cybersecurity 101").joinCode("ABC12")
      .institution(ClassroomTestFactory.institution()).createdBy(ClassroomTestFactory.user())
      .build();
    User instructor = ClassroomTestFactory.teacher("Nora", "nora@ntnu.no");
    ClassroomInstructor ci = ClassroomTestFactory.classroomInstructor(classroom, instructor);

    when(classroomRepository.findByIdAndDeletedAtIsNull(classroomId)).thenReturn(Optional.of(classroom));
    when(classroomInstructorRepository.findAllByClassroom(classroom)).thenReturn(List.of(ci));

    List<ClassroomInstructorResponseDTO> result = classroomService.getInstructors(classroomId);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).classroomId()).isEqualTo(classroomId);
  }
}
