package ntnu.no.backend.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import ntnu.no.backend.dto.classroom.ClassroomInstructorResponseDTO;
import ntnu.no.backend.mapper.ClassroomInstructorMapper;
import ntnu.no.backend.model.Classroom;
import ntnu.no.backend.model.auth.User;
import ntnu.no.backend.model.classroom.ClassroomInstructor;
import ntnu.no.backend.model.classroom.ClassroomInstructorId;
import ntnu.no.backend.repository.ClassroomInstructorRepository;
import ntnu.no.backend.repository.ClassroomRepository;
import ntnu.no.backend.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing instructor assignments to classrooms.
 *
 * @author norakirknes
 * @version 0.1
 * @since 0.1
 * @see ClassroomInstructorRepository
 * @see ClassroomRepository
 */
@Service
@RequiredArgsConstructor
public class ClassroomInstructorService {

  private final ClassroomInstructorRepository classroomInstructorRepository;
  private final ClassroomRepository classroomRepository;
  private final UserRepository userRepository;

  /**
   * Lists all instructors in a classroom.
   *
   * @param classroomId the classroom ID
   * @param instructor the authenticated instructor
   * @return a list of instructor assignments
   * @throws AccessDeniedException if user is not an instructor of the classroom
   * @throws EntityNotFoundException if no active classroom found
   */
  @Transactional(readOnly = true)
  public List<ClassroomInstructorResponseDTO> getInstructors(UUID classroomId, User instructor) {
    verifyInstructor(classroomId, instructor);
    Classroom classroom = findClassroom(classroomId);
    return classroomInstructorRepository.findAllByClassroom(classroom).stream()
      .map(ClassroomInstructorMapper::toDTO)
      .toList();
  }

  /**
   * Lists all classrooms the authenticated instructor belongs to.
   *
   * @param instructor the authenticated instructor
   * @return a list of classroom IDs and assignment details
   */
  @Transactional(readOnly = true)
  public List<ClassroomInstructorResponseDTO> getMyClassrooms(User instructor) {
    return classroomInstructorRepository.findAllByUser(instructor).stream()
      .map(ClassroomInstructorMapper::toDTO)
      .toList();
  }

  /**
   * Adds an instructor to a classroom. Only an existing instructor of the
   * classroom can add another instructor.
   *
   * @param classroomId the classroom ID
   * @param userId the user ID of the instructor to add
   * @param instructor the authenticated instructor performing the action
   * @return the created assignment as a DTO
   * @throws AccessDeniedException if the authenticated user is not an instructor of the classroom
   * @throws EntityNotFoundException if the classroom or target user is not found
   * @throws IllegalStateException if the target user is already an instructor of the classroom
   */
  @Transactional
  public ClassroomInstructorResponseDTO addInstructor(UUID classroomId, UUID userId, User instructor) {
    verifyInstructor(classroomId, instructor);
    Classroom classroom = findClassroom(classroomId);
    User targetUser = userRepository.findById(userId)
      .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

    if (classroomInstructorRepository.existsById(
      ClassroomInstructorId.builder()
        .classroomId(classroomId)
        .userId(userId).build())) {
      throw new IllegalStateException("User is already an instructor of this classroom");
    }

    ClassroomInstructor newInstructor = ClassroomInstructor.builder()
      .id(ClassroomInstructorId.builder()
        .classroomId(classroomId)
        .userId(userId)
        .build())
      .classroom(classroom)
      .user(targetUser)
      .build();

    classroomInstructorRepository.save(newInstructor);
    return ClassroomInstructorMapper.toDTO(newInstructor);
  }

  /**
   * Removes an instructor from a classroom. Cannot remove the last instructor.
   *
   * @param classroomId the classroom ID
   * @param userId the user ID of the instructor to remove
   * @param instructor the authenticated instructor performing the action
   * @throws AccessDeniedException if the authenticated user is not an instructor of the classroom
   * @throws EntityNotFoundException if the target instructor is not in the classroom
   * @throws IllegalStateException if attempting to remove the last instructor
   */
  @Transactional
  public void removeInstructor(UUID classroomId, UUID userId, User instructor) {
    verifyInstructor(classroomId, instructor);
    Classroom classroom = findClassroom(classroomId);

    ClassroomInstructor target =
      classroomInstructorRepository.findById(
        ClassroomInstructorId.builder()
        .classroomId(classroomId)
        .userId(userId).build())
      .orElseThrow(() -> new EntityNotFoundException("Instructor not found in classroom"));

    if (classroomInstructorRepository.findAllByClassroom(classroom).size() <= 1) {
      throw new IllegalStateException("Cannot remove the last instructor from a classroom");
    }
    classroomInstructorRepository.delete(target);
  }

  /**
   * Verifies that the user is an instructor of the given classroom.
   *
   * @param classroomId the classroom ID
   * @param user the user to verify
   * @throws AccessDeniedException if the user is not an instructor of the classroom
   */
  private void verifyInstructor(UUID classroomId, User user) {
    if (!classroomInstructorRepository.existsById(
      ClassroomInstructorId.builder()
        .classroomId(classroomId)
        .userId(user.getId()).build())) {
      throw new AccessDeniedException("You are not an instructor of this classroom");
    }
  }

  /**
   * Looks up an active classroom by ID.
   *
   * @param classroomId the classroom ID
   * @return the classroom entity
   * @throws EntityNotFoundException if no active classroom found
   */
  private Classroom findClassroom(UUID classroomId) {
    return classroomRepository.findByIdAndDeletedAtIsNull(classroomId)
      .orElseThrow(() -> new EntityNotFoundException("Classroom not found with id: " + classroomId));
  }
}
