package ntnu.no.backend.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import ntnu.no.backend.controller.MysteryController;
import ntnu.no.backend.dto.mystery.MysteryReviewDTO;
import ntnu.no.backend.dto.mystery.MysterySubmissionRequestDTO;
import ntnu.no.backend.dto.mystery.MysterySubmissionResponseDTO;
import ntnu.no.backend.dto.mystery.MysteryTaskCreateRequestDTO;
import ntnu.no.backend.dto.task.ClassroomTaskResponseDTO;
import ntnu.no.backend.mapper.ClassroomTaskMapper;
import ntnu.no.backend.mapper.MysterySubmissionMapper;
import ntnu.no.backend.model.Classroom;
import ntnu.no.backend.model.ClassroomTask;
import ntnu.no.backend.model.ClassroomTaskId;
import ntnu.no.backend.model.MysterySubmission;
import ntnu.no.backend.model.Task;
import ntnu.no.backend.model.auth.User;
import ntnu.no.backend.model.classroom.ClassroomStudent;
import ntnu.no.backend.model.classroom.ClassroomStudentId;
import ntnu.no.backend.model.enums.MysteryStatus;
import ntnu.no.backend.repository.ClassroomRepository;
import ntnu.no.backend.repository.ClassroomStudentRepository;
import ntnu.no.backend.repository.ClassroomTaskRepository;
import ntnu.no.backend.repository.MysterySubmissionRepository;
import ntnu.no.backend.repository.TaskRepository;
import ntnu.no.backend.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.temporal.IsoFields;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for managing mystery submissions.
 * Database triggers handle notification creation and achievement awards.
 * This service must only persist submission/task state and return DTOs.
 *
 * @author aninesst
 * @version 0.2
 * @since 0.1
 * @see MysteryController
 * @see MysterySubmission
 */
@Service
@RequiredArgsConstructor
public class MysteryService {
  private final MysterySubmissionRepository mysterySubmissionRepository;
  private final ClassroomRepository classroomRepository;
  private final UserRepository userRepository;
  private final ClassroomStudentRepository classroomStudentRepository;
  private final TaskRepository taskRepository;
  private final ClassroomTaskRepository classroomTaskRepository;
  private final ObjectMapper objectMapper;

  /**
   * Submits a new mystery for the authenticated student.
   *
   * @param dto the submission request
   * @param userId the authenticated student id
   * @return the created submission DTO
   */
  @Transactional
  public MysterySubmissionResponseDTO submitMystery(MysterySubmissionRequestDTO dto, UUID userId) {
    Classroom classroom = classroomRepository.findByIdAndDeletedAtIsNull(dto.getClassroomId())
        .orElseThrow(() -> new EntityNotFoundException("Classroom not found with id: " + dto.getClassroomId()));

    User user = userRepository.findById(userId)
        .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

    verifyStudentMembership(dto.getClassroomId(), user);

    LocalDate now = LocalDate.now();

    MysterySubmission submission = MysterySubmission.builder()
        .classroom(classroom)
        .userSubmittedBy(user)
        .mysDesc(dto.getMysDesc())
        .attachmentUrl(dto.getAttachmentUrl())
        .mysStatus(MysteryStatus.PENDING)
        .isFeatured(false)
        .weekNumber(now.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR))
        .weekYear(now.get(IsoFields.WEEK_BASED_YEAR))
        .build();

    return MysterySubmissionMapper.toDTO(mysterySubmissionRepository.save(submission));
  }

  /**
   * Returns all mystery submissions for a classroom.
   *
   * @param classroomId the classroom id
   * @return submission DTOs
   */
  @Transactional(readOnly = true)
  public List<MysterySubmissionResponseDTO> getSubmissionsByClassroom(UUID classroomId) {
    return mysterySubmissionRepository.findByClassroom_Id(classroomId)
        .stream()
        .map(MysterySubmissionMapper::toDTO)
        .toList();
  }

  /**
   * Returns the authenticated student's own submissions for a classroom.
   *
   * @param classroomId the classroom id
   * @param userId the authenticated student id
   * @return submission DTOs for the authenticated student
   */
  @Transactional(readOnly = true)
  public List<MysterySubmissionResponseDTO> getMySubmissions(UUID classroomId, UUID userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

    verifyStudentMembership(classroomId, user);

    return mysterySubmissionRepository.findByClassroom_IdAndUserSubmittedBy_Id(classroomId, userId)
        .stream()
        .map(MysterySubmissionMapper::toDTO)
        .toList();
  }

  /**
   * Returns pending mystery submissions for a classroom.
   *
   * @param classroomId the classroom id
   * @return pending submission DTOs
   */
  @Transactional(readOnly = true)
  public List<MysterySubmissionResponseDTO> getPendingSubmissions(UUID classroomId) {
    return mysterySubmissionRepository.findByClassroom_IdAndMysStatus(classroomId, MysteryStatus.PENDING)
        .stream()
        .map(MysterySubmissionMapper::toDTO)
        .toList();
  }

  /**
   * Reviews a mystery submission as an instructor.
   * Updating a submission to {@link MysteryStatus#APPROVED} lets the database trigger create
   * notifications and achievements. The service does not do that manually.
   *
   * @param submissionId the submission id
   * @param dto the review request
   * @param reviewerId the authenticated instructor id
   * @return reviewed submission DTO
   */
  @Transactional
  public MysterySubmissionResponseDTO reviewSubmission(UUID submissionId, MysteryReviewDTO dto, UUID reviewerId) {
    MysterySubmission submission = mysterySubmissionRepository.findById(submissionId)
        .orElseThrow(() -> new EntityNotFoundException("Mystery submission not found with id: " + submissionId));

    User reviewer = userRepository.findById(reviewerId)
        .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + reviewerId));

    if (dto.getMysStatus() == MysteryStatus.PENDING) {
      throw new IllegalArgumentException("Reviewed submission must be approved or rejected");
    }

    submission.setMysStatus(dto.getMysStatus());
    submission.setUserReviewedBy(reviewer);
    submission.setReviewedAt(LocalDateTime.now());
    submission.setFeatured(dto.isFeatured() && dto.getMysStatus() == MysteryStatus.APPROVED);

    return MysterySubmissionMapper.toDTO(mysterySubmissionRepository.save(submission));
  }

  /**
   * Creates a normal classroom task from a student mystery submission.
   * The generated task is assigned to the same classroom as the submission.
   * If the submission is still pending, it is marked approved so the database trigger can handle
   * approval-side effects; this method never creates notifications or achievements manually.
   *
   * @param submissionId the mystery submission id
   * @param dto task creation options
   * @param reviewerId the authenticated instructor id
   * @return the created classroom task DTO
   */
  @Transactional
  public ClassroomTaskResponseDTO createTaskFromSubmission(
      UUID submissionId,
      MysteryTaskCreateRequestDTO dto,
      UUID reviewerId
  ) {
    MysterySubmission submission = mysterySubmissionRepository.findById(submissionId)
        .orElseThrow(() -> new EntityNotFoundException("Mystery submission not found with id: " + submissionId));

    User reviewer = userRepository.findById(reviewerId)
        .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + reviewerId));

    Task task = Task.builder()
        .title(dto.getTitle())
        .taskType(dto.getTaskType())
        .stopLocation(dto.getStopLocation())
        .difficulty(dto.getDifficulty())
        .content(resolveTaskContent(submission, dto))
        .owner(reviewer)
        .isActive(true)
        .build();
    taskRepository.save(task);

    Classroom classroom = submission.getClassroom();
    ClassroomTaskId classroomTaskId = new ClassroomTaskId(classroom.getId(), task.getId());
    short sortOrder = dto.getSortOrder() != null
        ? dto.getSortOrder()
        : (short) (classroomTaskRepository.findAllByClassroom_IdOrderBySortOrderAsc(classroom.getId()).size() + 1);

    ClassroomTask classroomTask = ClassroomTask.builder()
        .id(classroomTaskId)
        .classroom(classroom)
        .task(task)
        .sortOrder(sortOrder)
        .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
        .build();
    classroomTaskRepository.save(classroomTask);

    if (submission.getMysStatus() == MysteryStatus.PENDING) {
      submission.setMysStatus(MysteryStatus.APPROVED);
      submission.setUserReviewedBy(reviewer);
      submission.setReviewedAt(LocalDateTime.now());
      submission.setFeatured(true);
      mysterySubmissionRepository.save(submission);
    }

    return ClassroomTaskMapper.toDTO(classroomTask);
  }

  /**
   * Returns the featured mystery submission for a classroom.
   *
   * @param classroomId the classroom id
   * @return the featured submission DTO
   */
  @Transactional(readOnly = true)
  public MysterySubmissionResponseDTO getFeaturedSubmission(UUID classroomId) {
    return mysterySubmissionRepository.findFirstByClassroom_IdAndIsFeaturedTrue(classroomId)
        .map(MysterySubmissionMapper::toDTO)
        .orElseThrow(() -> new EntityNotFoundException("Featured mystery submission not found for classroom: " + classroomId));
  }

  @Transactional(readOnly = true)
  public List<MysterySubmissionResponseDTO> getAllFeaturedSubmissions(UUID classroomId) {
    return mysterySubmissionRepository
        .findByClassroom_IdAndIsFeaturedTrueOrderByWeekYearDescWeekNumberDesc(classroomId)
        .stream()
        .map(MysterySubmissionMapper::toDTO)
        .toList();
  }

  private String resolveTaskContent(MysterySubmission submission, MysteryTaskCreateRequestDTO dto) {
    if (dto.getContent() != null && !dto.getContent().isBlank()) {
      return dto.getContent();
    }

    try {
      return objectMapper.writeValueAsString(Map.of(
          "description", submission.getMysDesc() != null ? submission.getMysDesc() : "",
          "sourceUrl", submission.getAttachmentUrl() != null ? submission.getAttachmentUrl() : "",
          "mysterySubmissionId", submission.getId().toString(),
          "weekNumber", submission.getWeekNumber(),
          "weekYear", submission.getWeekYear()
      ));
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Failed to serialize task content", e);
    }
  }

  private void verifyStudentMembership(UUID classroomId, User user) {
    ClassroomStudent membership = classroomStudentRepository.findById(
        ClassroomStudentId.builder()
            .classroomId(classroomId)
            .userId(user.getId())
            .build()
    ).orElse(null);

    if (membership == null || !membership.isAllowed()) {
      throw new AccessDeniedException("You are not an approved member of this classroom");
    }
  }
}
