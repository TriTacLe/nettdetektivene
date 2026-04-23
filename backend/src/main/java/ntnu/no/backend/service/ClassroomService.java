package ntnu.no.backend.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import ntnu.no.backend.dto.classroom.ClassroomRequestDTO;
import ntnu.no.backend.dto.classroom.ClassroomResponseDTO;
import ntnu.no.backend.dto.task.ClassroomTaskResponseDTO;
import ntnu.no.backend.dto.task.ClassroomTaskUpdateRequestDTO;
import ntnu.no.backend.mapper.ClassroomMapper;
import ntnu.no.backend.mapper.ClassroomTaskMapper;
import ntnu.no.backend.model.Classroom;
import ntnu.no.backend.model.ClassroomTask;
import ntnu.no.backend.model.ClassroomTaskId;
import ntnu.no.backend.model.Institution;
import ntnu.no.backend.model.auth.User;
import ntnu.no.backend.repository.ClassroomRepository;
import ntnu.no.backend.repository.ClassroomTaskRepository;
import ntnu.no.backend.repository.InstitutionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing classrooms.
 *
 * @author tri, aninesst
 * @version 0.2
 * @since 0.1
 */
@Service
@RequiredArgsConstructor
public class ClassroomService {

  private final ClassroomRepository classroomRepository;
  private final InstitutionRepository institutionRepository;
  private final ClassroomTaskRepository classroomTaskRepository;

  @Transactional(readOnly = true)
  public List<ClassroomResponseDTO> getAllClassroom() {
    return classroomRepository.findAllByDeletedAtIsNull().stream().map(ClassroomMapper::toDTO).toList();
  }

  @Transactional(readOnly = true)
  public ClassroomResponseDTO getClassroom(UUID id) {
    Classroom classroom = classroomRepository.findByIdAndDeletedAtIsNull(id)
        .orElseThrow(() -> new EntityNotFoundException("Classroom not found with id: " + id));
    return ClassroomMapper.toDTO(classroom);
  }

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

  @Transactional
  public ClassroomResponseDTO updateClassroom(ClassroomRequestDTO dto, UUID id) {
    Classroom classroom = classroomRepository.findByIdAndDeletedAtIsNull(id)
        .orElseThrow(() -> new EntityNotFoundException("Classroom not found with id: " + id));

    classroom.setTitle(dto.getTitle());
    classroom.setClassDesc(dto.getClassDesc());
    classroom.setGameEnabled(dto.isGameEnabled());

    return ClassroomMapper.toDTO(classroom);
  }

  @Transactional
  public void deleteClassroom(UUID id) {
    Classroom classroom = classroomRepository.findByIdAndDeletedAtIsNull(id)
        .orElseThrow(() -> new EntityNotFoundException("Classroom not found with id: " + id));
    classroom.setDeletedAt(LocalDateTime.now());
  }

  @Transactional(readOnly = true)
  public List<ClassroomResponseDTO> getAllClassroomsByInstitution(UUID institutionId) {
    return classroomRepository.findAllByInstitution_IdAndDeletedAtIsNull(institutionId).stream().map(ClassroomMapper::toDTO).toList();
  }

  @Transactional(readOnly = true)
  public List<ClassroomTaskResponseDTO> getClassroomTasks(UUID classroomId) {
    if (!classroomRepository.existsById(classroomId)) {
      throw new EntityNotFoundException("Classroom not found with id: " + classroomId);
    }

    return classroomTaskRepository.findAllByClassroom_IdOrderBySortOrderAsc(classroomId).stream()
        .map(ClassroomTaskMapper::toDTO)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<ClassroomTaskResponseDTO> getActiveClassroomTasks(UUID classroomId) {
    if (!classroomRepository.existsById(classroomId)) {
      throw new EntityNotFoundException("Classroom not found with id: " + classroomId);
    }

    return classroomTaskRepository.findAllByClassroom_IdAndIsActiveTrueOrderBySortOrderAsc(classroomId).stream()
        .map(ClassroomTaskMapper::toDTO)
        .toList();
  }

  @Transactional
  public ClassroomTaskResponseDTO updateClassroomTask(
      UUID classroomId,
      UUID taskId,
      ClassroomTaskUpdateRequestDTO dto
  ) {
    ClassroomTask classroomTask = classroomTaskRepository.findById(new ClassroomTaskId(classroomId, taskId))
        .orElseThrow(() -> new EntityNotFoundException(
            "Classroom task not found for classroomId: " + classroomId + ", taskId: " + taskId
        ));

    if (dto.getIsActive() != null) {
      classroomTask.setIsActive(dto.getIsActive());
    }
    if (dto.getSortOrder() != null) {
      classroomTask.setSortOrder(dto.getSortOrder());
    }

    return ClassroomTaskMapper.toDTO(classroomTask);
  }

  private String generateJoinCode() {
    return UUID.randomUUID().toString().substring(0, 5).toUpperCase();
  }
}