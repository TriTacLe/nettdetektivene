package ntnu.no.backend.service;

import jakarta.persistence.EntityNotFoundException;
import ntnu.no.backend.dto.classroom.ClassroomInstructorResponseDTO;
import ntnu.no.backend.mapper.ClassroomInstructorMapper;
import ntnu.no.backend.model.Classroom;
import ntnu.no.backend.model.auth.Role;
import ntnu.no.backend.model.auth.User;
import ntnu.no.backend.model.classroom.ClassroomInstructor;
import ntnu.no.backend.model.classroom.ClassroomInstructorId;
import ntnu.no.backend.model.enums.RoleName;
import ntnu.no.backend.repository.ClassroomInstructorRepository;
import ntnu.no.backend.repository.ClassroomRepository;
import ntnu.no.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClassroomInstructorServiceTest {

  @Mock
  private ClassroomInstructorRepository classroomInstructorRepository;

  @Mock
  private ClassroomRepository classroomRepository;

  @Mock
  private UserRepository userRepository;

  @InjectMocks
  private ClassroomInstructorService classroomInstructorService;

  private final UUID classroomId = UUID.randomUUID();
  private final UUID userId = UUID.randomUUID();
  private final UUID instructorId = UUID.randomUUID();

  private User createInstructorUser() {
    return createInstructorUser(instructorId, "Test Instructor", "instructor@example.com");
  }

  private User createInstructorUser(UUID id, String displayName, String email) {
    Role instructorRole = Role.builder()
        .id(UUID.randomUUID())
        .roleName(RoleName.INSTRUCTOR.getValue())
        .build();

    return User.builder()
        .id(id)
        .displayName(displayName)
        .email(email)
        .roles(List.of(instructorRole))
        .build();
  }

  private User createStudentUser() {
    Role studentRole = Role.builder()
        .id(UUID.randomUUID())
        .roleName(RoleName.STUDENT.getValue())
        .build();

    return User.builder()
        .id(userId)
        .displayName("Test Student")
        .email("student@example.com")
        .roles(List.of(studentRole))
        .build();
  }

  private Classroom createClassroom() {
    return Classroom.builder()
        .id(classroomId)
        .title("Test Classroom")
        .classDesc("A test classroom")
        .build();
  }

  @Test
  void getInstructors_returnsListOfInstructors() {
    User instructor = createInstructorUser();
    Classroom classroom = createClassroom();

    ClassroomInstructor assignment = ClassroomInstructor.builder()
        .id(ClassroomInstructorId.builder().classroomId(classroomId).userId(instructorId).build())
        .classroom(classroom)
        .user(instructor)
        .build();

    ClassroomInstructorId id = ClassroomInstructorId.builder()
        .classroomId(classroomId)
        .userId(instructorId)
        .build();

    when(classroomInstructorRepository.existsById(id)).thenReturn(true);
    when(classroomRepository.findByIdAndDeletedAtIsNull(classroomId)).thenReturn(Optional.of(classroom));
    when(classroomInstructorRepository.findAllByClassroom(classroom)).thenReturn(List.of(assignment));

    List<ClassroomInstructorResponseDTO> result = classroomInstructorService.getInstructors(classroomId, instructor);

    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals(classroomId, result.get(0).classroomId());
    verify(classroomInstructorRepository).existsById(id);
    verify(classroomRepository).findByIdAndDeletedAtIsNull(classroomId);
    verify(classroomInstructorRepository).findAllByClassroom(classroom);
  }

  @Test
  void getInstructors_throwsAccessDeniedException_whenNotInstructor() {
    User nonInstructor = createStudentUser();

    ClassroomInstructorId id = ClassroomInstructorId.builder()
        .classroomId(classroomId)
        .userId(userId)
        .build();

    when(classroomInstructorRepository.existsById(id)).thenReturn(false);

    assertThrows(AccessDeniedException.class, () ->
        classroomInstructorService.getInstructors(classroomId, nonInstructor));

    verify(classroomInstructorRepository).existsById(id);
    verify(classroomRepository, never()).findByIdAndDeletedAtIsNull(any());
  }

  @Test
  void getInstructors_throwsAccessDeniedException_whenStudentIsUser() {
    User instructor = createInstructorUser();
    User student = createStudentUser();

    ClassroomInstructorId id = ClassroomInstructorId.builder()
        .classroomId(classroomId)
        .userId(student.getId())
        .build();

    when(classroomInstructorRepository.existsById(id)).thenReturn(false);

    assertThrows(AccessDeniedException.class, () ->
        classroomInstructorService.getInstructors(classroomId, student));

    verify(classroomInstructorRepository).existsById(id);
  }

  @Test
  void getInstructors_throwsEntityNotFoundException_whenClassroomNotFound() {
    User instructor = createInstructorUser();

    ClassroomInstructorId id = ClassroomInstructorId.builder()
        .classroomId(classroomId)
        .userId(instructorId)
        .build();

    when(classroomInstructorRepository.existsById(id)).thenReturn(true);
    when(classroomRepository.findByIdAndDeletedAtIsNull(classroomId)).thenReturn(Optional.empty());

    assertThrows(EntityNotFoundException.class, () ->
        classroomInstructorService.getInstructors(classroomId, instructor));

    verify(classroomInstructorRepository).existsById(id);
    verify(classroomRepository).findByIdAndDeletedAtIsNull(classroomId);
  }

  @Test
  void getMyClassrooms_returnsListOfAssignments() {
    User instructor = createInstructorUser();
    Classroom classroom = createClassroom();

    ClassroomInstructor assignment = ClassroomInstructor.builder()
        .id(ClassroomInstructorId.builder().classroomId(classroomId).userId(instructorId).build())
        .classroom(classroom)
        .user(instructor)
        .build();

    when(classroomInstructorRepository.findAllByUser(instructor)).thenReturn(List.of(assignment));

    List<ClassroomInstructorResponseDTO> result = classroomInstructorService.getMyClassrooms(instructor);

    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals(classroomId, result.get(0).classroomId());
    verify(classroomInstructorRepository).findAllByUser(instructor);
  }

  @Test
  void getMyClassrooms_returnsEmptyList_whenNoClassrooms() {
    User instructor = createInstructorUser();

    when(classroomInstructorRepository.findAllByUser(instructor)).thenReturn(List.of());

    List<ClassroomInstructorResponseDTO> result = classroomInstructorService.getMyClassrooms(instructor);

    assertNotNull(result);
    assertTrue(result.isEmpty());
    verify(classroomInstructorRepository).findAllByUser(instructor);
  }

  @Test
  void addInstructor_savesNewInstructor() {
    User requestingInstructor = createInstructorUser();
    UUID newInstructorId = UUID.randomUUID();
    User newInstructor = createInstructorUser(newInstructorId, "New Instructor", "new.instructor@example.com");
    Classroom classroom = createClassroom();

    ClassroomInstructorId requestingId = ClassroomInstructorId.builder()
        .classroomId(classroomId)
        .userId(requestingInstructor.getId())
        .build();

    ClassroomInstructorId newId = ClassroomInstructorId.builder()
        .classroomId(classroomId)
        .userId(newInstructor.getId())
        .build();

    when(classroomInstructorRepository.existsById(requestingId)).thenReturn(true);
    when(classroomRepository.findByIdAndDeletedAtIsNull(classroomId)).thenReturn(Optional.of(classroom));
    when(userRepository.findById(newInstructor.getId())).thenReturn(Optional.of(newInstructor));
    when(classroomInstructorRepository.existsById(newId)).thenReturn(false);
    when(classroomInstructorRepository.save(any(ClassroomInstructor.class))).thenAnswer(i -> i.getArgument(0));

    ClassroomInstructorResponseDTO result = classroomInstructorService.addInstructor(
        classroomId, newInstructor.getId(), requestingInstructor);

    assertNotNull(result);
    verify(classroomInstructorRepository).existsById(requestingId);
    verify(classroomRepository).findByIdAndDeletedAtIsNull(classroomId);
    verify(userRepository).findById(newInstructor.getId());
    verify(classroomInstructorRepository).existsById(newId);
    verify(classroomInstructorRepository).save(any(ClassroomInstructor.class));
  }

  @Test
  void addInstructor_throwsAccessDeniedException_whenNotInstructor() {
    User student = createStudentUser();
    User newInstructor = createInstructorUser();

    ClassroomInstructorId id = ClassroomInstructorId.builder()
        .classroomId(classroomId)
        .userId(student.getId())
        .build();

    when(classroomInstructorRepository.existsById(id)).thenReturn(false);

    assertThrows(AccessDeniedException.class, () ->
        classroomInstructorService.addInstructor(classroomId, newInstructor.getId(), student));

    verify(classroomInstructorRepository).existsById(id);
    verify(classroomRepository, never()).findByIdAndDeletedAtIsNull(any());
  }

  @Test
  void addInstructor_throwsEntityNotFoundException_whenClassroomNotFound() {
    User instructor = createInstructorUser();
    User newInstructor = createInstructorUser();

    ClassroomInstructorId id = ClassroomInstructorId.builder()
        .classroomId(classroomId)
        .userId(instructor.getId())
        .build();

    when(classroomInstructorRepository.existsById(id)).thenReturn(true);
    when(classroomRepository.findByIdAndDeletedAtIsNull(classroomId)).thenReturn(Optional.empty());

    assertThrows(EntityNotFoundException.class, () ->
        classroomInstructorService.addInstructor(classroomId, newInstructor.getId(), instructor));

    verify(classroomInstructorRepository).existsById(id);
    verify(classroomRepository).findByIdAndDeletedAtIsNull(classroomId);
  }

  @Test
  void addInstructor_throwsEntityNotFoundException_whenUserNotFound() {
    User instructor = createInstructorUser();
    Classroom classroom = createClassroom();
    UUID nonExistentUserId = UUID.randomUUID();

    ClassroomInstructorId id = ClassroomInstructorId.builder()
        .classroomId(classroomId)
        .userId(instructor.getId())
        .build();

    when(classroomInstructorRepository.existsById(id)).thenReturn(true);
    when(classroomRepository.findByIdAndDeletedAtIsNull(classroomId)).thenReturn(Optional.of(classroom));
    when(userRepository.findById(nonExistentUserId)).thenReturn(Optional.empty());

    assertThrows(EntityNotFoundException.class, () ->
        classroomInstructorService.addInstructor(classroomId, nonExistentUserId, instructor));

    verify(classroomInstructorRepository).existsById(id);
    verify(classroomRepository).findByIdAndDeletedAtIsNull(classroomId);
    verify(userRepository).findById(nonExistentUserId);
  }

  @Test
  void addInstructor_throwsIllegalStateException_whenUserNotInstructor() {
    User requestingInstructor = createInstructorUser();
    User student = createStudentUser();
    Classroom classroom = createClassroom();

    ClassroomInstructorId id = ClassroomInstructorId.builder()
        .classroomId(classroomId)
        .userId(requestingInstructor.getId())
        .build();

    when(classroomInstructorRepository.existsById(id)).thenReturn(true);
    when(classroomRepository.findByIdAndDeletedAtIsNull(classroomId)).thenReturn(Optional.of(classroom));
    when(userRepository.findById(student.getId())).thenReturn(Optional.of(student));

    assertThrows(IllegalStateException.class, () ->
        classroomInstructorService.addInstructor(classroomId, student.getId(), requestingInstructor));

    verify(classroomInstructorRepository).existsById(id);
    verify(classroomRepository).findByIdAndDeletedAtIsNull(classroomId);
    verify(userRepository).findById(student.getId());
  }

  @Test
  void addInstructor_throwsIllegalStateException_whenAlreadyInstructor() {
    User requestingInstructor = createInstructorUser();
    UUID existingInstructorId = UUID.randomUUID();
    User existingInstructor = createInstructorUser(existingInstructorId, "Existing Instructor", "existing.instructor@example.com");
    Classroom classroom = createClassroom();

    ClassroomInstructorId requestingId = ClassroomInstructorId.builder()
        .classroomId(classroomId)
        .userId(requestingInstructor.getId())
        .build();

    ClassroomInstructorId existingId = ClassroomInstructorId.builder()
        .classroomId(classroomId)
        .userId(existingInstructor.getId())
        .build();

    when(classroomInstructorRepository.existsById(requestingId)).thenReturn(true);
    when(classroomRepository.findByIdAndDeletedAtIsNull(classroomId)).thenReturn(Optional.of(classroom));
    when(userRepository.findById(existingInstructor.getId())).thenReturn(Optional.of(existingInstructor));
    when(classroomInstructorRepository.existsById(existingId)).thenReturn(true);

    assertThrows(IllegalStateException.class, () ->
        classroomInstructorService.addInstructor(classroomId, existingInstructor.getId(), requestingInstructor));

    verify(classroomInstructorRepository).existsById(requestingId);
    verify(classroomRepository).findByIdAndDeletedAtIsNull(classroomId);
    verify(userRepository).findById(existingInstructor.getId());
    verify(classroomInstructorRepository).existsById(existingId);
  }

  @Test
  void removeInstructor_deletesInstructor() {
    User requestingInstructor = createInstructorUser();
    UUID instructorToRemoveId = UUID.randomUUID();
    User instructorToRemove = createInstructorUser(instructorToRemoveId, "Instructor To Remove", "remove.instructor@example.com");
    Classroom classroom = createClassroom();

    ClassroomInstructorId requestingId = ClassroomInstructorId.builder()
        .classroomId(classroomId)
        .userId(requestingInstructor.getId())
        .build();

    ClassroomInstructorId targetId = ClassroomInstructorId.builder()
        .classroomId(classroomId)
        .userId(instructorToRemove.getId())
        .build();

    ClassroomInstructor requestingAssignment = ClassroomInstructor.builder()
        .id(requestingId)
        .classroom(classroom)
        .user(requestingInstructor)
        .build();

    ClassroomInstructor targetAssignment = ClassroomInstructor.builder()
        .id(targetId)
        .classroom(classroom)
        .user(instructorToRemove)
        .build();

    when(classroomInstructorRepository.existsById(requestingId)).thenReturn(true);
    when(classroomRepository.findByIdAndDeletedAtIsNull(classroomId)).thenReturn(Optional.of(classroom));
    when(classroomInstructorRepository.findById(targetId)).thenReturn(Optional.of(targetAssignment));
    when(classroomInstructorRepository.findAllByClassroom(classroom)).thenReturn(List.of(requestingAssignment, targetAssignment));

    classroomInstructorService.removeInstructor(classroomId, instructorToRemove.getId(), requestingInstructor);

    verify(classroomInstructorRepository).existsById(requestingId);
    verify(classroomRepository).findByIdAndDeletedAtIsNull(classroomId);
    verify(classroomInstructorRepository).findById(targetId);
    verify(classroomInstructorRepository).findAllByClassroom(classroom);
    verify(classroomInstructorRepository).delete(targetAssignment);
  }

  @Test
  void removeInstructor_throwsAccessDeniedException_whenNotInstructor() {
    User student = createStudentUser();
    User instructor = createInstructorUser();

    ClassroomInstructorId id = ClassroomInstructorId.builder()
        .classroomId(classroomId)
        .userId(student.getId())
        .build();

    when(classroomInstructorRepository.existsById(id)).thenReturn(false);

    assertThrows(AccessDeniedException.class, () ->
        classroomInstructorService.removeInstructor(classroomId, instructor.getId(), student));

    verify(classroomInstructorRepository).existsById(id);
    verify(classroomRepository, never()).findByIdAndDeletedAtIsNull(any());
  }

  @Test
  void removeInstructor_throwsEntityNotFoundException_whenClassroomNotFound() {
    User instructor = createInstructorUser();
    UUID instructorToRemoveId = UUID.randomUUID();
    User instructorToRemove = createInstructorUser(instructorToRemoveId, "Non Existent Instructor", "nonexistent@example.com");

    ClassroomInstructorId id = ClassroomInstructorId.builder()
        .classroomId(classroomId)
        .userId(instructor.getId())
        .build();

    when(classroomInstructorRepository.existsById(id)).thenReturn(true);
    when(classroomRepository.findByIdAndDeletedAtIsNull(classroomId)).thenReturn(Optional.empty());

    assertThrows(EntityNotFoundException.class, () ->
        classroomInstructorService.removeInstructor(classroomId, instructorToRemove.getId(), instructor));

    verify(classroomInstructorRepository).existsById(id);
    verify(classroomRepository).findByIdAndDeletedAtIsNull(classroomId);
  }

  @Test
  void removeInstructor_throwsEntityNotFoundException_whenInstructorNotFound() {
    User requestingInstructor = createInstructorUser();
    UUID instructorToRemoveId = UUID.randomUUID();
    User instructorToRemove = createInstructorUser(instructorToRemoveId, "Instructor Not In Classroom", "notinclassroom@example.com");
    Classroom classroom = createClassroom();

    ClassroomInstructorId requestingId = ClassroomInstructorId.builder()
        .classroomId(classroomId)
        .userId(requestingInstructor.getId())
        .build();

    ClassroomInstructorId targetId = ClassroomInstructorId.builder()
        .classroomId(classroomId)
        .userId(instructorToRemove.getId())
        .build();

    when(classroomInstructorRepository.existsById(requestingId)).thenReturn(true);
    when(classroomRepository.findByIdAndDeletedAtIsNull(classroomId)).thenReturn(Optional.of(classroom));
    when(classroomInstructorRepository.findById(targetId)).thenReturn(Optional.empty());

    assertThrows(EntityNotFoundException.class, () ->
        classroomInstructorService.removeInstructor(classroomId, instructorToRemove.getId(), requestingInstructor));

    verify(classroomInstructorRepository).existsById(requestingId);
    verify(classroomRepository).findByIdAndDeletedAtIsNull(classroomId);
    verify(classroomInstructorRepository).findById(targetId);
  }

@Test
  void removeInstructor_throwsIllegalStateException_whenRemovingLastInstructor() {
    User requestingInstructor = createInstructorUser();
    UUID instructorToRemoveId = UUID.randomUUID();
    User instructorToRemove = createInstructorUser(instructorToRemoveId, "Last Instructor", "last.instructor@example.com");
    Classroom classroom = createClassroom();

    ClassroomInstructorId requestingId = ClassroomInstructorId.builder()
        .classroomId(classroomId)
        .userId(requestingInstructor.getId())
        .build();

    ClassroomInstructorId targetId = ClassroomInstructorId.builder()
        .classroomId(classroomId)
        .userId(instructorToRemove.getId())
        .build();

    ClassroomInstructor assignment = ClassroomInstructor.builder()
        .id(targetId)
        .classroom(classroom)
        .user(instructorToRemove)
        .build();

    when(classroomInstructorRepository.existsById(requestingId)).thenReturn(true);
    when(classroomRepository.findByIdAndDeletedAtIsNull(classroomId)).thenReturn(Optional.of(classroom));
    when(classroomInstructorRepository.findById(targetId)).thenReturn(Optional.of(assignment));
    when(classroomInstructorRepository.findAllByClassroom(classroom)).thenReturn(List.of(assignment));

    assertThrows(IllegalStateException.class, () ->
        classroomInstructorService.removeInstructor(classroomId, instructorToRemove.getId(), requestingInstructor));

    verify(classroomInstructorRepository).existsById(requestingId);
    verify(classroomRepository).findByIdAndDeletedAtIsNull(classroomId);
    verify(classroomInstructorRepository).findById(targetId);
    verify(classroomInstructorRepository).findAllByClassroom(classroom);
    verify(classroomInstructorRepository, never()).delete(any());
  }
}