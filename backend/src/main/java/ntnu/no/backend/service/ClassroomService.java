package ntnu.no.backend.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import ntnu.no.backend.dto.classroom.ClassroomRequestDTO;
import ntnu.no.backend.dto.classroom.ClassroomResponseDTO;
import ntnu.no.backend.mapper.ClassroomMapper;
import ntnu.no.backend.model.Classroom;
import ntnu.no.backend.model.Institution;
import ntnu.no.backend.model.auth.User;
import ntnu.no.backend.repository.ClassroomRepository;
import ntnu.no.backend.repository.InstitutionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing classrooms.
 *
 * @author tri
 * @version 0.1
 * @since 0.1
 */
@Service
@RequiredArgsConstructor
public class ClassroomService {

  private final ClassroomRepository classroomRepository;
  private final InstitutionRepository institutionRepository;

  /**
   * Returns all active classrooms.
   *
   * @return list of all non-deleted classrooms as DTOs
   */
  @Transactional(readOnly = true)
  public List<ClassroomResponseDTO> getAllClassroom() {
    return classroomRepository.findAllByDeletedAtIsNull().stream().map(ClassroomMapper::toDTO).toList();
  }

  /**
   * Returns a single active classroom by ID.
   *
   * @param id the classroom ID
   * @return the classroom as a DTO
   * @throws EntityNotFoundException if no active classroom found with the given ID
   */
  @Transactional(readOnly = true)
  public ClassroomResponseDTO getClassroom(UUID id) {
    Classroom classroom = classroomRepository.findByIdAndDeletedAtIsNull(id)
      .orElseThrow(() -> new EntityNotFoundException("Classroom not found with id: " + id));
    return ClassroomMapper.toDTO(classroom);
  }

  /**
   * Creates a new classroom.
   *
   * @param dto         the request DTO
   * @param currentUser the authenticated user who becomes the creator
   * @return the created classroom as a DTO
   * @throws EntityNotFoundException if no institution found with the given ID
   */
  @Transactional
  public ClassroomResponseDTO createClassroom(ClassroomRequestDTO dto, User currentUser) {
    Institution institution = institutionRepository.findById(dto.getInstitutionId())
      .orElseThrow(() -> new EntityNotFoundException("Institution not found with id: " + dto.getInstitutionId()));

    Classroom classroom = Classroom.builder()
      .title(dto.getTitle())
      .classDesc(dto.getClassDesc())
      .institution(institution)
      .createdBy(currentUser)
      .joinCode(generateJoinCode())
      .isGameEnabled(dto.isGameEnabled())
      .build();

    classroomRepository.save(classroom);
    return ClassroomMapper.toDTO(classroom);
  }

  /**
   * Updates an existing classroom.
   *
   * @param dto the request DTO with updated values
   * @param id  the classroom ID
   * @return the updated classroom as a DTO
   * @throws EntityNotFoundException if no active classroom found with the given ID
   */
  @Transactional
  public ClassroomResponseDTO updateClassroom(ClassroomRequestDTO dto, UUID id) {
    Classroom classroom = classroomRepository.findByIdAndDeletedAtIsNull(id)
      .orElseThrow(() -> new EntityNotFoundException("Classroom not found with id: " + id));

    classroom.setTitle(dto.getTitle());
    classroom.setClassDesc(dto.getClassDesc());
    classroom.setGameEnabled(dto.isGameEnabled());

    return ClassroomMapper.toDTO(classroom);
  }

  /**
   * Soft deletes a classroom by setting its {@code deletedAt} timestamp.
   *
   * @param id the classroom ID
   * @throws EntityNotFoundException if no active classroom found with the given ID
   */
  @Transactional
  public void deleteClassroom(UUID id) {
    Classroom classroom = classroomRepository.findByIdAndDeletedAtIsNull(id)
      .orElseThrow(() -> new EntityNotFoundException("Classroom not found with id: " + id));
    classroom.setDeletedAt(LocalDateTime.now());
  }

  /**
   * Returns all active classrooms belonging to an institution.
   *
   * @param institutionId the institution ID
   * @return list of active classrooms for the institution as DTOs
   */
  @Transactional(readOnly = true)
  public List<ClassroomResponseDTO> getAllClassroomsByInstitution(UUID institutionId) {
    return classroomRepository.findAllByInstitution_IdAndDeletedAtIsNull(institutionId).stream().map(ClassroomMapper::toDTO).toList();
  }

  // TODO: Blocked until ClassroomStudent entity and repository are implemented (Nora's area)
  //
  // /**
  //  * Adds the current user to a classroom matching the given join code.
  //  *
  //  * @param joinCode    the classroom join code
  //  * @param currentUser the authenticated student joining the classroom
  //  * @return the joined classroom as a DTO
  //  * @throws EntityNotFoundException if no active classroom found with the given join code
  //  */
  // @Transactional
  // public ClassroomResponseDTO joinClassroom(String joinCode, User currentUser) {
  //   Classroom classroom = classroomRepository.findByJoinCodeAndDeletedAtIsNull(joinCode)
  //     .orElseThrow(() -> new EntityNotFoundException("No classroom found with join code: " + joinCode));
  //   ClassroomStudent member = ClassroomStudent.builder()
  //     .classroom(classroom)
  //     .user(currentUser)
  //     .build();
  //   classroomStudentRepository.save(member);
  //   return ClassroomMapper.toDTO(classroom);
  // }

  /**
   * Generates a random 5 character uppercase join code.
   *
   * @return a join code string
   */
  private String generateJoinCode() {
    return UUID.randomUUID().toString().substring(0, 5).toUpperCase();
  }
}
