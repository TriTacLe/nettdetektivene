package ntnu.no.backend.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import ntnu.no.backend.dto.user.UserResponseDTO;
import ntnu.no.backend.dto.user.UserUpdateDTO;
import ntnu.no.backend.mapper.UserMapper;
import ntnu.no.backend.model.Classroom;
import ntnu.no.backend.model.auth.User;
import ntnu.no.backend.model.classroom.ClassroomStudent;
import ntnu.no.backend.repository.ClassroomRepository;
import ntnu.no.backend.repository.ClassroomStudentRepository;
import ntnu.no.backend.repository.ClassroomInstructorRepository;
import ntnu.no.backend.repository.UserProfileRepository;
import ntnu.no.backend.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service for user account management.
 *
 * @author norakirknes
 * @version 0.1
 * @since 0.1
 */
@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;
  private final UserProfileRepository userProfileRepository;
  private final ClassroomRepository classroomRepository;
  private final ClassroomStudentRepository classroomStudentRepository;
  private final ClassroomInstructorRepository classroomInstructorRepository;


  @Transactional(readOnly = true)
  public UserResponseDTO getUser(UUID id) {
    User user = userRepository.findById(id)
      .orElseThrow(() -> new EntityNotFoundException("No user found with id: " + id));
    return UserMapper.toDTO(user);
  }


  @Transactional
  public UserResponseDTO updateUser(UUID id, UserUpdateDTO dto) {
    User user = userRepository.findById(id)
      .orElseThrow(() -> new EntityNotFoundException("No user found with id: " + id));
    UserMapper.updateEntity(user, dto);
    userProfileRepository.findByUser(user)
      .ifPresent(profile -> profile.setDisplayName(user.getDisplayName()));
    return UserMapper.toDTO(user);
  }

  /**
   * Updates a student's account by an instructor.
   * Instructors can only update students from their own classrooms.
   *
   * @param targetUserId the UUID of the student to update
   * @param dto the update request containing new values
   * @param requestingInstructor the authenticated instructor
   * @return the updated user as a DTO
   * @throws EntityNotFoundException if the target user is not found
   * @throws AccessDeniedException if the instructor is not in the same classroom as the student
   */
  @Transactional
  public UserResponseDTO updateStudentByInstructor(UUID targetUserId, UserUpdateDTO dto, User requestingInstructor) {
    boolean isInstructor = requestingInstructor.getAuthorities().stream()
        .anyMatch(a -> "instructor".equals(a.getAuthority()));

    if (!isInstructor) {
      throw new AccessDeniedException("Only instructors can update student accounts");
    }

    ClassroomStudent studentMembership = getStudentClassroom(targetUserId);
    Classroom classroom = classroomRepository.findByIdAndDeletedAtIsNull(studentMembership.getClassroom().getId())
        .orElseThrow(() -> new AccessDeniedException("Classroom not found"));

    boolean isInstructorOfClassroom = classroomInstructorRepository.existsByClassroomAndUser(classroom, requestingInstructor);

    if (!isInstructorOfClassroom) {
      throw new AccessDeniedException("You can only update students in your own classrooms");
    }

    User targetUser = userRepository.findById(targetUserId)
      .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + targetUserId));

    UserMapper.updateEntity(targetUser, dto);
    userProfileRepository.findByUser(targetUser)
      .ifPresent(profile -> profile.setDisplayName(targetUser.getDisplayName()));

    return UserMapper.toDTO(targetUser);
  }

  /**
   * Gets the classroom membership for a student user.
   *
   * @param userId the student user ID
   * @return the classroom student membership
   * @throws EntityNotFoundException if the student is not in a classroom
   */
  private ClassroomStudent getStudentClassroom(UUID userId) {
    User studentUser = userRepository.findById(userId)
      .orElseThrow(() -> new EntityNotFoundException("Student not found with id: " + userId));
    return classroomStudentRepository.findByUser(studentUser)
      .orElseThrow(() -> new EntityNotFoundException("Student is not in any classroom"));
  }
}
