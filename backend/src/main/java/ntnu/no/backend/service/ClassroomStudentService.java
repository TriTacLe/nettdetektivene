package ntnu.no.backend.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import ntnu.no.backend.dto.classroom.ClassroomResponseDTO;
import ntnu.no.backend.repository.ClassroomInstructorRepository;
import ntnu.no.backend.repository.ClassroomRepository;
import ntnu.no.backend.repository.ClassroomStudentRepository;
import ntnu.no.backend.repository.UserProfileRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import ntnu.no.backend.dto.classroom.ClassroomStudentResponseDTO;
import ntnu.no.backend.mapper.ClassroomMapper;
import ntnu.no.backend.mapper.ClassroomStudentMapper;
import ntnu.no.backend.model.Classroom;
import ntnu.no.backend.model.auth.User;
import ntnu.no.backend.model.auth.UserProfile;
import ntnu.no.backend.model.classroom.ClassroomStudent;
import ntnu.no.backend.model.classroom.ClassroomStudentId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ntnu.no.backend.model.classroom.ClassroomInstructorId;
import ntnu.no.backend.model.enums.NameChangeStatus;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing student classroom membership.
 * Handles join requests, approval, removal and listing.
 *
 * @author norakirknes
 * @version 0.1
 * @since 0.1
 * @see ClassroomStudentRepository
 * @see ClassroomRepository
 */
@Service
@RequiredArgsConstructor
public class ClassroomStudentService {

  private final ClassroomStudentRepository classroomStudentRepository;
  private final ClassroomRepository classroomRepository;
  private final ClassroomInstructorRepository classroomInstructorRepository;
  private final UserProfileRepository userProfileRepository;
  private final JdbcTemplate jdbcTemplate;
  private final EntityManager entityManager;

  /**
   * Gets the classroom the student belongs to
   *
   * @param user the user to fetch the classroom for
   * @return the classroom as DTO
   * @throws EntityNotFoundException if the student does not have classroom membership
   */
  @Transactional(readOnly = true)
  public ClassroomResponseDTO getMyClassroom(User user) {
    ClassroomStudent membership = classroomStudentRepository.findByUser(user)
        .orElseThrow(() -> new EntityNotFoundException("Student is not in any classroom"));
    return ClassroomMapper.toDTO(membership.getClassroom());
  }
  /**
   * Joins a student to a classroom via join code. The student's display name
   * is set from their user profile. Membership is pending until approved
   * by an instructor.
   *
   * @param joinCode the join code of the classroom
   * @param user the authenticated student
   * @return the created membership as a DTO
   * @throws EntityNotFoundException if no active classroom matches the join code
   * @throws IllegalStateException if the student is already in a classroom
   * @throws EntityNotFoundException if the student has no user profile
   */
  @Transactional
  public ClassroomStudentResponseDTO join(String joinCode, User user) {
    if (classroomStudentRepository.existsByUser(user)) {
      throw new IllegalStateException("Student is already in a classroom");
    }
    Classroom classroom = classroomRepository.findByJoinCodeAndDeletedAtIsNull(joinCode)
      .orElseThrow(() -> new EntityNotFoundException("No classroom found with join code: " + joinCode));
    UserProfile profile = userProfileRepository.findByUser(user)
      .orElseThrow(() -> new EntityNotFoundException("No profile found for user: " + user.getId()));

    ClassroomStudent student = ClassroomStudent.builder()
      .id(ClassroomStudentId.builder()
        .classroomId(classroom.getId())
        .userId(user.getId())
        .build())
      .classroom(classroom)
      .user(user)
      .displayName(profile.getDisplayName())
      .build();

    classroomStudentRepository.save(student);
    return ClassroomStudentMapper.toDTO(student);
  }

  /**
   * Lists all students in a classroom.
   *
   * @param classroomId the classroom ID
   * @param instructor the authenticated instructor
   * @return a list of student memberships
   * @throws AccessDeniedException if user is not an instructor of the classroom
   * @throws EntityNotFoundException if no active classroom found
   */
  @Transactional(readOnly = true)
  public List<ClassroomStudentResponseDTO> getStudents(UUID classroomId, User instructor) {
    verifyInstructor(classroomId, instructor);
    Classroom classroom = findClassroom(classroomId);
    return classroomStudentRepository.findAllByClassroom(classroom).stream()
      .map(ClassroomStudentMapper::toDTO)
      .toList();
  }

  /**
   * Lists approved students in a classroom.
   *
   * @param classroomId the classroom ID
   * @param instructor the authenticated instructor
   * @return a list of approved student memberships
   * @throws AccessDeniedException if user is not an instructor of the classroom
   * @throws EntityNotFoundException if no active classroom found
   */
  @Transactional(readOnly = true)
  public List<ClassroomStudentResponseDTO> getApprovedStudents(UUID classroomId, User instructor) {
    verifyInstructor(classroomId, instructor);
    Classroom classroom = findClassroom(classroomId);
    return classroomStudentRepository.findAllByClassroomAndIsAllowedIsTrue(classroom).stream()
      .map(ClassroomStudentMapper::toDTO)
      .toList();
  }

  /**
   * Lists pending students in a classroom.
   *
   * @param classroomId the classroom ID
   * @param instructor the authenticated instructor
   * @return a list of pending student memberships
   * @throws AccessDeniedException if user is not an instructor of the classroom
   * @throws EntityNotFoundException if no active classroom found
   */
  @Transactional(readOnly = true)
  public List<ClassroomStudentResponseDTO> getPendingStudents(UUID classroomId, User instructor) {
    verifyInstructor(classroomId, instructor);
    Classroom classroom = findClassroom(classroomId);
    return classroomStudentRepository.findAllByClassroomAndIsAllowedIsFalse(classroom).stream()
      .map(ClassroomStudentMapper::toDTO)
      .toList();
  }

  /**
   * Lists students with pending name change requests in a classroom.
   *
   * @param classroomId the classroom ID
   * @param instructor the authenticated instructor
   * @return a list of students with pending name changes
   * @throws AccessDeniedException if user is not an instructor of the classroom
   * @throws EntityNotFoundException if no active classroom found
   */
  @Transactional(readOnly = true)
  public List<ClassroomStudentResponseDTO> getPendingNameChanges(UUID classroomId, User instructor) {
    verifyInstructor(classroomId, instructor);
    Classroom classroom = findClassroom(classroomId);
    return classroomStudentRepository.findAllByClassroomAndNameChangeStatus(classroom, NameChangeStatus.PENDING).stream()
      .map(ClassroomStudentMapper::toDTO)
      .toList();
  }

  /**
   * Approves a student's membership in a classroom.
   *
   * @param classroomId the classroom ID
   * @param userId the student's user ID
   * @param instructor the authenticated instructor
   * @return the updated membership as a DTO
   * @throws AccessDeniedException if user is not an instructor of the classroom
   * @throws EntityNotFoundException if the student is not in the classroom
   */
  @Transactional
  public ClassroomStudentResponseDTO approve(UUID classroomId, UUID userId, User instructor) {
    verifyInstructor(classroomId, instructor);
    ClassroomStudent student = findMembership(classroomId, userId);
    student.setAllowed(true);
    return ClassroomStudentMapper.toDTO(student);
  }

  /**
   * Rejects and removes a student's membership from a classroom.
   *
   * @param classroomId the classroom ID
   * @param userId the student's user ID
   * @param instructor the authenticated instructor
   * @throws AccessDeniedException if user is not an instructor of the classroom
   * @throws EntityNotFoundException if the student is not in the classroom
   */
  @Transactional
  public void remove(UUID classroomId, UUID userId, User instructor) {
    verifyInstructor(classroomId, instructor);
    ClassroomStudent student = findMembership(classroomId, userId);
    classroomStudentRepository.delete(student);
  }

  /**
   * Approves a student's display name change request by calling
   * {@code sp_review_display_name_change}. After the SP updates the classroom
   * membership, the student's user profile display name is synced in Java
   * since the SP does not update {@code user_profile}.
   *
   * @param classroomId the classroom ID
   * @param userId the student's user ID
   * @param instructor the authenticated instructor
   * @return the updated membership as a DTO
   * @throws AccessDeniedException if user is not an instructor of the classroom
   * @throws EntityNotFoundException if the student is not in the classroom
   */
  @Transactional
  public ClassroomStudentResponseDTO approveNameChange(UUID classroomId, UUID userId, User instructor) {
    verifyInstructor(classroomId, instructor);
    jdbcTemplate.update("CALL sp_review_display_name_change(?, ?, ?)",
        instructor.getId().toString(), userId.toString(), "APPROVED");
    entityManager.flush();
    entityManager.clear();

    ClassroomStudent student = findMembership(classroomId, userId);
    UserProfile profile = userProfileRepository.findByUser(student.getUser())
      .orElseThrow(() -> new EntityNotFoundException("No profile found for user: " + userId));
    profile.setDisplayName(student.getDisplayName());

    return ClassroomStudentMapper.toDTO(student);
  }

  /**
   * Rejects a student's display name change request by calling
   * {@code sp_review_display_name_change}.
   *
   * @param classroomId the classroom ID
   * @param userId the student's user ID
   * @param instructor the authenticated instructor
   * @return the updated membership as a DTO
   * @throws AccessDeniedException if user is not an instructor of the classroom
   * @throws EntityNotFoundException if the student is not in the classroom
   */
  @Transactional
  public ClassroomStudentResponseDTO rejectNameChange(UUID classroomId, UUID userId, User instructor) {
    verifyInstructor(classroomId, instructor);
    jdbcTemplate.update("CALL sp_review_display_name_change(?, ?, ?)",
        instructor.getId().toString(), userId.toString(), "REJECTED");
    entityManager.flush();
    entityManager.clear();
    ClassroomStudent student = findMembership(classroomId, userId);
    return ClassroomStudentMapper.toDTO(student);
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
        .userId(user.getId())
        .build())) {
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

  /**
   * Looks up a students membership by classroom and user ID.
   *
   * @param classroomId the classroom ID
   * @param userId the student's user ID
   * @return the membership entity
   * @throws EntityNotFoundException if the student is not in the classroom
   */
  private ClassroomStudent findMembership(UUID classroomId, UUID userId) {
    return classroomStudentRepository.findById(
        ClassroomStudentId.builder()
          .classroomId(classroomId)
          .userId(userId)
          .build())
      .orElseThrow(() -> new EntityNotFoundException("Student not found in classroom"));
  }
}
