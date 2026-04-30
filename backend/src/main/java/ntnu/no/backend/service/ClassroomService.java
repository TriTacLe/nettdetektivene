package ntnu.no.backend.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import ntnu.no.backend.dto.classroom.ClassroomRequestDTO;
import ntnu.no.backend.dto.classroom.ClassroomResponseDTO;
import ntnu.no.backend.dto.task.ClassroomTaskCreateRequestDTO;
import ntnu.no.backend.dto.task.ClassroomTaskResponseDTO;
import ntnu.no.backend.dto.task.ClassroomTaskUpdateRequestDTO;
import ntnu.no.backend.mapper.ClassroomMapper;
import ntnu.no.backend.mapper.ClassroomTaskMapper;
import ntnu.no.backend.model.Classroom;
import ntnu.no.backend.model.ClassroomTask;
import ntnu.no.backend.model.ClassroomTaskId;
import ntnu.no.backend.model.Institution;
import ntnu.no.backend.model.Task;
import ntnu.no.backend.model.UserTaskUnlocked;
import ntnu.no.backend.model.UserTaskUnlockedId;
import ntnu.no.backend.model.auth.User;
import ntnu.no.backend.model.classroom.ClassroomStudent;
import ntnu.no.backend.repository.ClassroomRepository;
import ntnu.no.backend.repository.ClassroomInstructorRepository;
import ntnu.no.backend.repository.ClassroomStudentRepository;
import ntnu.no.backend.repository.ClassroomTaskRepository;
import ntnu.no.backend.repository.InstitutionRepository;
import ntnu.no.backend.repository.TaskBestScoreRepository;
import ntnu.no.backend.repository.TaskRepository;
import ntnu.no.backend.repository.UserRepository;
import ntnu.no.backend.repository.UserTaskUnlockedRepository;
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
  private final UserTaskUnlockedRepository userTaskUnlockedRepository;
  private final TaskRepository taskRepository;
  private final UserRepository userRepository;
  private final ClassroomStudentRepository classroomStudentRepository;
  private final ClassroomInstructorRepository classroomInstructorRepository;
  private final TaskBestScoreRepository taskBestScoreRepository;

  @Transactional(readOnly = true)
  public List<ClassroomResponseDTO> getClassroomsForInstructor(User instructor) {
    return classroomInstructorRepository.findAllByUser(instructor).stream()
        .map(ci -> ci.getClassroom())
        .filter(c -> c.getDeletedAt() == null)
        .map(ClassroomMapper::toDTO)
        .toList();
  }

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

    boolean wasDisabled = !classroom.isGameEnabled();
    classroom.setGameEnabled(dto.isGameEnabled());

    if (wasDisabled && dto.isGameEnabled()) {
      unlockFirstTaskForApprovedStudents(classroom);
    }

    return ClassroomMapper.toDTO(classroom);
  }

  private void unlockFirstTaskForApprovedStudents(Classroom classroom) {
    List<ClassroomTask> activeTasks = classroomTaskRepository
        .findAllByClassroom_IdAndIsActiveTrueOrderBySortOrderAsc(classroom.getId());
    if (activeTasks.isEmpty()) return;

    ClassroomTask firstTask = activeTasks.get(0);
    List<ClassroomStudent> approvedStudents = classroomStudentRepository
        .findAllByClassroomAndIsAllowedIsTrue(classroom);

    for (ClassroomStudent student : approvedStudents) {
      UUID userId = student.getUser().getId();
      if (!userTaskUnlockedRepository.existsByUser_IdAndClassroom_IdAndTask_Id(
          userId, classroom.getId(), firstTask.getTask().getId())) {
        userTaskUnlockedRepository.save(
            UserTaskUnlocked.builder()
                .id(new UserTaskUnlockedId(userId, classroom.getId(), firstTask.getTask().getId()))
                .user(student.getUser())
                .classroom(classroom)
                .task(firstTask.getTask())
                .unlockedAt(LocalDateTime.now())
                .build()
        );
      }
    }
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

  @Transactional(readOnly = true)
  public List<ClassroomTaskResponseDTO> getActiveClassroomTasksWithUnlocked(UUID classroomId, UUID userId) {
    if (!classroomRepository.existsById(classroomId)) {
      throw new EntityNotFoundException("Classroom not found with id: " + classroomId);
    }

    List<UUID> unlockedTaskIds = getUnlockedTaskIds(classroomId, userId);

    return classroomTaskRepository.findAllByClassroom_IdAndIsActiveTrueOrderBySortOrderAsc(classroomId).stream()
        .map(ct -> ClassroomTaskMapper.toDTO(ct, unlockedTaskIds))
        .toList();
  }

  @Transactional(readOnly = true)
  public List<ClassroomTaskResponseDTO> getActiveClassroomTasksTestMode(UUID classroomId, UUID userId) {
    if (!classroomRepository.existsById(classroomId)) {
      throw new EntityNotFoundException("Classroom not found with id: " + classroomId);
    }

    List<ClassroomTask> allTasks = classroomTaskRepository
        .findAllByClassroom_IdAndIsActiveTrueOrderBySortOrderAsc(classroomId);

    List<UUID> allTaskIds = allTasks.stream()
        .map(ct -> ct.getTask().getId())
        .toList();

    return allTasks.stream()
        .map(ct -> ClassroomTaskMapper.toDTO(ct, allTaskIds))
        .toList();
  }

  @Transactional
  public ClassroomTaskResponseDTO createClassroomTask(UUID classroomId, ClassroomTaskCreateRequestDTO dto) {
    Classroom classroom = classroomRepository.findByIdAndDeletedAtIsNull(classroomId)
      .orElseThrow(() -> new EntityNotFoundException("Classroom not found with id: " + classroomId));

    Task task = taskRepository.findById(dto.getTaskId())
      .orElseThrow(() -> new EntityNotFoundException("Task not found with id: " + dto.getTaskId()));

    ClassroomTaskId classroomTaskId = new ClassroomTaskId(classroomId, task.getId());
    if (classroomTaskRepository.existsById(classroomTaskId)) {
      throw new IllegalArgumentException("Task is already assigned to this classroom");
    }

    short sortOrder = dto.getSortOrder() != null
      ? dto.getSortOrder()
      : (short) (classroomTaskRepository.findAllByClassroom_IdOrderBySortOrderAsc(classroomId).size() + 1);

    ClassroomTask classroomTask = ClassroomTask.builder()
      .id(classroomTaskId)
      .classroom(classroom)
      .task(task)
      .sortOrder(sortOrder)
      .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
      .build();

    classroomTaskRepository.save(classroomTask);

    // The trg_attempt_sync trigger only unlocks the next task in sortOrder when
    // a student passes the previous one. A task added after students have
    // already progressed past its sortOrder slot would never get unlocked.
    // Mirror the trigger's chain rule here so newly added tasks become
    // available to anyone who has already cleared the predecessor.
    unlockNewTaskForEligibleStudents(classroom, classroomTask);

    return ClassroomTaskMapper.toDTO(classroomTask);
  }

  private void unlockNewTaskForEligibleStudents(Classroom classroom, ClassroomTask newTask) {
    if (Boolean.FALSE.equals(newTask.getIsActive())) return;

    String stopLocation = newTask.getTask().getStopLocation();
    List<ClassroomTask> activeTasks = classroomTaskRepository
        .findAllByClassroom_IdAndIsActiveTrueOrderBySortOrderAsc(classroom.getId());

    List<ClassroomTask> earlierInStop = activeTasks.stream()
        .filter(ct -> stopLocation != null && stopLocation.equals(ct.getTask().getStopLocation()))
        .filter(ct -> ct.getSortOrder() < newTask.getSortOrder())
        .toList();

    ClassroomTask globalPredecessor = activeTasks.stream()
        .filter(ct -> ct.getSortOrder() < newTask.getSortOrder())
        .reduce((a, b) -> a.getSortOrder() >= b.getSortOrder() ? a : b)
        .orElse(null);

    List<ClassroomStudent> approvedStudents = classroomStudentRepository
        .findAllByClassroomAndIsAllowedIsTrue(classroom);

    for (ClassroomStudent student : approvedStudents) {
      UUID userId = student.getUser().getId();

      boolean shouldUnlock;
      if (!earlierInStop.isEmpty()) {
        // The new task slots into an existing stop. Unlock once the student
        // has already cleared every earlier active task in that stop, since
        // they would otherwise have nothing left to gate the new task on.
        shouldUnlock = earlierInStop.stream().allMatch(ct -> taskBestScoreRepository
            .existsByUserIdAndTaskIdAndClassroomIdAndFirstPassedAtIsNotNull(
                userId, ct.getTask().getId(), classroom.getId()));
      } else {
        // First active task in its stop (or only task in the classroom):
        // fall back to the global sortOrder chain.
        shouldUnlock = globalPredecessor == null
            || taskBestScoreRepository
                .existsByUserIdAndTaskIdAndClassroomIdAndFirstPassedAtIsNotNull(
                    userId, globalPredecessor.getTask().getId(), classroom.getId());
      }

      if (!shouldUnlock) continue;
      if (userTaskUnlockedRepository.existsByUser_IdAndClassroom_IdAndTask_Id(
          userId, classroom.getId(), newTask.getTask().getId())) continue;

      userTaskUnlockedRepository.save(
          UserTaskUnlocked.builder()
              .id(new UserTaskUnlockedId(userId, classroom.getId(), newTask.getTask().getId()))
              .user(student.getUser())
              .classroom(classroom)
              .task(newTask.getTask())
              .unlockedAt(LocalDateTime.now())
              .build()
      );
    }
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

  @Transactional(readOnly = true)
  public boolean isTaskUnlockedForUser(UUID classroomId, UUID taskId, UUID userId) {
    return userTaskUnlockedRepository.existsByUser_IdAndClassroom_IdAndTask_Id(userId, classroomId, taskId);
  }

  @Transactional(readOnly = true)
  public List<UUID> getUnlockedTaskIds(UUID classroomId, UUID userId) {
    return userTaskUnlockedRepository.findByUser_IdAndClassroom_Id(userId, classroomId)
        .stream()
        .map(ut -> ut.getId().getTaskId())
        .toList();
  }

  @Transactional(readOnly = true)
  public List<ClassroomTaskResponseDTO> getUnlockedClassroomTasks(UUID classroomId, UUID userId) {
    if (!classroomRepository.existsById(classroomId)) {
      throw new EntityNotFoundException("Classroom not found with id: " + classroomId);
    }

    List<UUID> unlockedTaskIds = getUnlockedTaskIds(classroomId, userId);

    return classroomTaskRepository.findAllByClassroom_IdAndIsActiveTrueOrderBySortOrderAsc(classroomId)
        .stream()
        .filter(ct -> unlockedTaskIds.contains(ct.getTask().getId()))
        .map(ct -> ClassroomTaskMapper.toDTO(ct, unlockedTaskIds))
        .toList();
  }

  @Transactional
  public void unlockTaskForUser(UUID classroomId, UUID taskId, UUID userId) {
    Classroom classroom = classroomRepository.findById(classroomId)
        .orElseThrow(() -> new EntityNotFoundException("Classroom not found with id: " + classroomId));

    Task task = taskRepository.findById(taskId)
        .orElseThrow(() -> new EntityNotFoundException("Task not found with id: " + taskId));

    User user = userRepository.findById(userId)
        .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

    if (isTaskUnlockedForUser(classroomId, taskId, userId)) {
      return;
    }

    UserTaskUnlocked unlocked = UserTaskUnlocked.builder()
        .id(new UserTaskUnlockedId(userId, classroomId, taskId))
        .user(user)
        .classroom(classroom)
        .task(task)
        .unlockedAt(LocalDateTime.now())
        .build();

    userTaskUnlockedRepository.save(unlocked);
  }

  private String generateJoinCode() {
    return UUID.randomUUID().toString().substring(0, 5).toUpperCase();
  }
}
