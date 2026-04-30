package ntnu.no.backend.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import ntnu.no.backend.dto.mystery.MysteryCompletionResponseDTO;
import ntnu.no.backend.dto.mystery.MysteryPlayDTO;
import ntnu.no.backend.dto.mystery.MysteryReviewActionDTO;
import ntnu.no.backend.dto.mystery.MysteryReviewDTO;
import ntnu.no.backend.dto.mystery.MysterySubmissionRequestDTO;
import ntnu.no.backend.mapper.MysterySubmissionMapper;
import ntnu.no.backend.model.Classroom;
import ntnu.no.backend.model.MysteryCompletion;
import ntnu.no.backend.model.MysteryCompletionId;
import ntnu.no.backend.model.MysterySubmission;
import ntnu.no.backend.model.auth.User;
import ntnu.no.backend.model.classroom.ClassroomInstructorId;
import ntnu.no.backend.model.classroom.ClassroomStudent;
import ntnu.no.backend.model.classroom.ClassroomStudentId;
import ntnu.no.backend.model.enums.MysteryStatus;
import ntnu.no.backend.repository.ClassroomInstructorRepository;
import ntnu.no.backend.repository.ClassroomRepository;
import ntnu.no.backend.repository.ClassroomStudentRepository;
import ntnu.no.backend.repository.MysteryCompletionRepository;
import ntnu.no.backend.repository.MysterySubmissionRepository;
import ntnu.no.backend.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Service for managing mystery submissions and completions.
 *
 * <p>Database triggers create notifications and award achievements; this
 * service must only persist state and return DTOs. The submitter never
 * earns an achievement — achievements are for solvers and are awarded
 * by {@code trg_mystery_completion} via {@code sp_award_achievement}.</p>
 */
@Service
@RequiredArgsConstructor
public class MysteryService {

  private final MysterySubmissionRepository mysterySubmissionRepository;
  private final MysteryCompletionRepository mysteryCompletionRepository;
  private final ClassroomRepository classroomRepository;
  private final UserRepository userRepository;
  private final ClassroomStudentRepository classroomStudentRepository;
  private final ClassroomInstructorRepository classroomInstructorRepository;
  private final MysteryImageStorageService mysteryImageStorageService;

  /**
   * Submits a new mystery from the authenticated student.
   */
  @Transactional
  public MysteryReviewDTO submitMystery(MysterySubmissionRequestDTO dto, MultipartFile image, UUID userId) {
    Classroom classroom = classroomRepository.findByIdAndDeletedAtIsNull(dto.getClassroomId())
        .orElseThrow(() -> new EntityNotFoundException("Classroom not found with id: " + dto.getClassroomId()));

    User user = userRepository.findById(userId)
        .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

    verifyStudentMembership(dto.getClassroomId(), user);

    String imagePath = mysteryImageStorageService.storeImage(image);

    MysterySubmission submission = MysterySubmission.builder()
        .classroom(classroom)
        .userSubmittedBy(user)
        .imagePath(imagePath)
        .alternative1(dto.getAlternative1())
        .alternative2(dto.getAlternative2())
        .alternative3(dto.getAlternative3())
        .correctIndex(dto.getCorrectIndex())
        .mysStatus(MysteryStatus.PENDING)
        .build();

    return MysterySubmissionMapper.toReviewDto(mysterySubmissionRepository.save(submission));
  }

  /**
   * Approved mysteries the given student can play (excludes own submissions).
   */
  @Transactional(readOnly = true)
  public List<MysteryPlayDTO> getPlayList(UUID classroomId, UUID userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

    verifyStudentMembership(classroomId, user);

    Set<UUID> completed = new HashSet<>();
    for (MysteryCompletion mc : mysteryCompletionRepository.findByIdUserId(userId)) {
      completed.add(mc.getId().getSubmissionId());
    }

    return mysterySubmissionRepository
        .findByClassroom_IdAndMysStatusAndUserSubmittedBy_IdNot(classroomId, MysteryStatus.APPROVED, userId)
        .stream()
        .map(submission -> MysterySubmissionMapper.toPlayDto(submission, completed.contains(submission.getId())))
        .toList();
  }

  /**
   * Single approved mystery for play. Verifies membership and that the
   * student is not the submitter.
   */
  @Transactional(readOnly = true)
  public MysteryPlayDTO getMysteryForPlay(UUID submissionId, UUID userId) {
    MysterySubmission submission = mysterySubmissionRepository.findById(submissionId)
        .orElseThrow(() -> new EntityNotFoundException("Mystery submission not found with id: " + submissionId));

    User user = userRepository.findById(userId)
        .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

    verifyStudentMembership(submission.getClassroom().getId(), user);

    if (submission.getMysStatus() != MysteryStatus.APPROVED) {
      throw new AccessDeniedException("Mystery is not available for play");
    }
    if (submission.getUserSubmittedBy().getId().equals(userId)) {
      throw new AccessDeniedException("You cannot play your own mystery");
    }

    boolean completed = mysteryCompletionRepository.existsByIdUserIdAndIdSubmissionId(userId, submissionId);
    return MysterySubmissionMapper.toPlayDto(submission, completed);
  }

  /**
   * Records the student's pick. If correct and not already completed,
   * inserts into {@code mystery_completion} (the trigger then awards
   * the matching achievement).
   */
  @Transactional
  public MysteryCompletionResponseDTO completeMystery(UUID submissionId, byte selectedIndex, UUID userId) {
    MysterySubmission submission = mysterySubmissionRepository.findById(submissionId)
        .orElseThrow(() -> new EntityNotFoundException("Mystery submission not found with id: " + submissionId));

    User user = userRepository.findById(userId)
        .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

    verifyStudentMembership(submission.getClassroom().getId(), user);

    if (submission.getMysStatus() != MysteryStatus.APPROVED) {
      throw new AccessDeniedException("Mystery is not available for play");
    }
    if (submission.getUserSubmittedBy().getId().equals(userId)) {
      throw new AccessDeniedException("You cannot play your own mystery");
    }

    boolean alreadyCompleted = mysteryCompletionRepository.existsByIdUserIdAndIdSubmissionId(userId, submissionId);
    boolean correct = selectedIndex == submission.getCorrectIndex();

    if (correct && !alreadyCompleted) {
      MysteryCompletion completion = MysteryCompletion.builder()
          .id(new MysteryCompletionId(userId, submissionId))
          .user(user)
          .submission(submission)
          .build();
      try {
        mysteryCompletionRepository.saveAndFlush(completion);
      } catch (DataIntegrityViolationException raceLost) {
        // Concurrent correct pick from the same user already inserted the row
        // (composite PK on user_id+submission_id). Treat as already completed.
        alreadyCompleted = true;
      }
    }

    return new MysteryCompletionResponseDTO(correct, correct || alreadyCompleted, alreadyCompleted);
  }

  @Transactional(readOnly = true)
  public List<MysteryReviewDTO> getSubmissionsByClassroom(UUID classroomId, UUID instructorId) {
    verifyInstructorOfClassroom(classroomId, instructorId);
    return mysterySubmissionRepository.findByClassroom_Id(classroomId)
        .stream()
        .map(MysterySubmissionMapper::toReviewDto)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<MysteryReviewDTO> getMySubmissions(UUID classroomId, UUID userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

    verifyStudentMembership(classroomId, user);

    return mysterySubmissionRepository.findByClassroom_IdAndUserSubmittedBy_Id(classroomId, userId)
        .stream()
        .map(MysterySubmissionMapper::toReviewDto)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<MysteryReviewDTO> getPendingSubmissions(UUID classroomId, UUID instructorId) {
    verifyInstructorOfClassroom(classroomId, instructorId);
    return mysterySubmissionRepository.findByClassroom_IdAndMysStatus(classroomId, MysteryStatus.PENDING)
        .stream()
        .map(MysterySubmissionMapper::toReviewDto)
        .toList();
  }

  @Transactional
  public MysteryReviewDTO reviewSubmission(UUID submissionId, MysteryReviewActionDTO dto, UUID reviewerId) {
    MysterySubmission submission = mysterySubmissionRepository.findById(submissionId)
        .orElseThrow(() -> new EntityNotFoundException("Mystery submission not found with id: " + submissionId));

    User reviewer = userRepository.findById(reviewerId)
        .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + reviewerId));

    verifyInstructorOfClassroom(submission.getClassroom().getId(), reviewerId);

    if (dto.getMysStatus() == MysteryStatus.PENDING) {
      throw new IllegalArgumentException("Reviewed submission must be approved or rejected");
    }

    submission.setMysStatus(dto.getMysStatus());
    submission.setUserReviewedBy(reviewer);
    submission.setReviewedAt(LocalDateTime.now());

    return MysterySubmissionMapper.toReviewDto(mysterySubmissionRepository.save(submission));
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

  private void verifyInstructorOfClassroom(UUID classroomId, UUID instructorId) {
    boolean assigned = classroomInstructorRepository.existsById(
        ClassroomInstructorId.builder()
            .classroomId(classroomId)
            .userId(instructorId)
            .build()
    );
    if (!assigned) {
      throw new AccessDeniedException("You are not an instructor of this classroom");
    }
  }
}
