package ntnu.no.backend.service;

import jakarta.persistence.EntityNotFoundException;
import ntnu.no.backend.dto.mystery.MysteryReviewDTO;
import ntnu.no.backend.dto.mystery.MysterySubmissionRequestDTO;
import ntnu.no.backend.dto.mystery.MysterySubmissionResponseDTO;
import ntnu.no.backend.model.Classroom;
import ntnu.no.backend.model.MysterySubmission;
import ntnu.no.backend.model.auth.User;
import ntnu.no.backend.model.classroom.ClassroomStudent;
import ntnu.no.backend.model.enums.MysteryStatus;
import ntnu.no.backend.repository.ClassroomRepository;
import ntnu.no.backend.repository.ClassroomStudentRepository;
import ntnu.no.backend.repository.MysterySubmissionRepository;
import ntnu.no.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link MysteryService}.
 *
 * @author aninesst
 * @version 0.1
 * @since 0.1
 */
@ExtendWith(MockitoExtension.class)
class MysteryServiceTest {

  @Mock
  MysterySubmissionRepository mysterySubmissionRepository;

  @Mock
  ClassroomRepository classroomRepository;

  @Mock
  UserRepository userRepository;

  @Mock
  ClassroomStudentRepository classroomStudentRepository;

  @InjectMocks
  MysteryService mysteryService;

  @Test
  void submitMystery_savesPendingSubmissionAndReturnsDTO() {
    UUID classroomId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    Classroom classroom = Classroom.builder().id(classroomId).build();
    User user = User.builder().id(userId).displayName("Student").build();
    MysterySubmission saved = submission(UUID.randomUUID(), classroom, user, MysteryStatus.PENDING, false);

    when(classroomRepository.findByIdAndDeletedAtIsNull(classroomId)).thenReturn(Optional.of(classroom));
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(classroomStudentRepository.findById(any())).thenReturn(Optional.of(ClassroomStudent.builder().isAllowed(true).build()));
    when(mysterySubmissionRepository.save(any(MysterySubmission.class))).thenReturn(saved);

    MysterySubmissionResponseDTO result = mysteryService.submitMystery(request(classroomId), userId);

    assertThat(result.classroomId()).isEqualTo(classroomId);
    assertThat(result.submittedBy()).isEqualTo(userId);
    assertThat(result.mysStatus()).isEqualTo(MysteryStatus.PENDING);
    verify(mysterySubmissionRepository).save(any(MysterySubmission.class));
  }

  @Test
  void submitMystery_throwsAccessDenied_whenStudentIsNotApprovedMember() {
    UUID classroomId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    Classroom classroom = Classroom.builder().id(classroomId).build();
    User user = User.builder().id(userId).displayName("Student").build();

    when(classroomRepository.findByIdAndDeletedAtIsNull(classroomId)).thenReturn(Optional.of(classroom));
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(classroomStudentRepository.findById(any())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> mysteryService.submitMystery(request(classroomId), userId))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void submitMystery_throwsEntityNotFound_whenClassroomNotFound() {
    UUID classroomId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    when(classroomRepository.findByIdAndDeletedAtIsNull(classroomId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> mysteryService.submitMystery(request(classroomId), userId))
        .isInstanceOf(EntityNotFoundException.class);
  }

  @Test
  void getSubmissionsByClassroom_returnsMappedDTOs() {
    UUID classroomId = UUID.randomUUID();
    Classroom classroom = Classroom.builder().id(classroomId).build();
    User user = User.builder().id(UUID.randomUUID()).build();

    when(mysterySubmissionRepository.findByClassroom_Id(classroomId))
        .thenReturn(List.of(submission(UUID.randomUUID(), classroom, user, MysteryStatus.PENDING, false)));

    List<MysterySubmissionResponseDTO> result = mysteryService.getSubmissionsByClassroom(classroomId);

    assertThat(result).hasSize(1);
    assertThat(result.getFirst().classroomId()).isEqualTo(classroomId);
  }

  @Test
  void getPendingSubmissions_returnsOnlyPendingDTOs() {
    UUID classroomId = UUID.randomUUID();
    Classroom classroom = Classroom.builder().id(classroomId).build();
    User user = User.builder().id(UUID.randomUUID()).build();

    when(mysterySubmissionRepository.findByClassroom_IdAndMysStatus(classroomId, MysteryStatus.PENDING))
        .thenReturn(List.of(submission(UUID.randomUUID(), classroom, user, MysteryStatus.PENDING, false)));

    List<MysterySubmissionResponseDTO> result = mysteryService.getPendingSubmissions(classroomId);

    assertThat(result).hasSize(1);
    assertThat(result.getFirst().mysStatus()).isEqualTo(MysteryStatus.PENDING);
  }

  @Test
  void reviewSubmission_approvesAndFeaturesSubmission() {
    UUID submissionId = UUID.randomUUID();
    UUID reviewerId = UUID.randomUUID();
    Classroom classroom = Classroom.builder().id(UUID.randomUUID()).build();
    User student = User.builder().id(UUID.randomUUID()).build();
    User reviewer = User.builder().id(reviewerId).build();
    MysterySubmission existing = submission(submissionId, classroom, student, MysteryStatus.PENDING, false);

    when(mysterySubmissionRepository.findById(submissionId)).thenReturn(Optional.of(existing));
    when(userRepository.findById(reviewerId)).thenReturn(Optional.of(reviewer));
    when(mysterySubmissionRepository.save(existing)).thenReturn(existing);

    MysteryReviewDTO reviewDTO = MysteryReviewDTO.builder()
        .mysStatus(MysteryStatus.APPROVED)
        .featured(true)
        .build();

    MysterySubmissionResponseDTO result = mysteryService.reviewSubmission(submissionId, reviewDTO, reviewerId);

    assertThat(result.mysStatus()).isEqualTo(MysteryStatus.APPROVED);
    assertThat(result.reviewedBy()).isEqualTo(reviewerId);
    assertThat(result.isFeatured()).isTrue();
    assertThat(result.reviewedAt()).isNotNull();
  }

  @Test
  void reviewSubmission_rejectsWithoutFeaturingSubmission() {
    UUID submissionId = UUID.randomUUID();
    UUID reviewerId = UUID.randomUUID();
    Classroom classroom = Classroom.builder().id(UUID.randomUUID()).build();
    User student = User.builder().id(UUID.randomUUID()).build();
    User reviewer = User.builder().id(reviewerId).build();
    MysterySubmission existing = submission(submissionId, classroom, student, MysteryStatus.PENDING, false);

    when(mysterySubmissionRepository.findById(submissionId)).thenReturn(Optional.of(existing));
    when(userRepository.findById(reviewerId)).thenReturn(Optional.of(reviewer));
    when(mysterySubmissionRepository.save(existing)).thenReturn(existing);

    MysteryReviewDTO reviewDTO = MysteryReviewDTO.builder()
        .mysStatus(MysteryStatus.REJECTED)
        .featured(true)
        .build();

    MysterySubmissionResponseDTO result = mysteryService.reviewSubmission(submissionId, reviewDTO, reviewerId);

    assertThat(result.mysStatus()).isEqualTo(MysteryStatus.REJECTED);
    assertThat(result.isFeatured()).isFalse();
  }

  @Test
  void reviewSubmission_throwsIllegalArgumentException_whenStatusIsPending() {
    UUID submissionId = UUID.randomUUID();
    UUID reviewerId = UUID.randomUUID();
    MysterySubmission existing = submission(
        submissionId,
        Classroom.builder().id(UUID.randomUUID()).build(),
        User.builder().id(UUID.randomUUID()).build(),
        MysteryStatus.PENDING,
        false
    );

    when(mysterySubmissionRepository.findById(submissionId)).thenReturn(Optional.of(existing));
    when(userRepository.findById(reviewerId)).thenReturn(Optional.of(User.builder().id(reviewerId).build()));

    assertThatThrownBy(() -> mysteryService.reviewSubmission(
        submissionId,
        MysteryReviewDTO.builder().mysStatus(MysteryStatus.PENDING).build(),
        reviewerId
    )).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void getFeaturedSubmission_returnsFeaturedDTO() {
    UUID classroomId = UUID.randomUUID();
    Classroom classroom = Classroom.builder().id(classroomId).build();
    User user = User.builder().id(UUID.randomUUID()).build();
    MysterySubmission featured = submission(UUID.randomUUID(), classroom, user, MysteryStatus.APPROVED, true);

    when(mysterySubmissionRepository.findFirstByClassroom_IdAndIsFeaturedTrue(classroomId))
        .thenReturn(Optional.of(featured));

    MysterySubmissionResponseDTO result = mysteryService.getFeaturedSubmission(classroomId);

    assertThat(result.isFeatured()).isTrue();
    assertThat(result.classroomId()).isEqualTo(classroomId);
  }

  @Test
  void getFeaturedSubmission_throwsEntityNotFound_whenMissing() {
    UUID classroomId = UUID.randomUUID();

    when(mysterySubmissionRepository.findFirstByClassroom_IdAndIsFeaturedTrue(classroomId))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> mysteryService.getFeaturedSubmission(classroomId))
        .isInstanceOf(EntityNotFoundException.class);
  }

  private MysterySubmissionRequestDTO request(UUID classroomId) {
    return MysterySubmissionRequestDTO.builder()
        .classroomId(classroomId)
        .mysDesc("Suspicious message")
        .attachmentUrl("https://example.com/file.png")
        .build();
  }

  private MysterySubmission submission(
      UUID id,
      Classroom classroom,
      User submittedBy,
      MysteryStatus status,
      boolean featured
  ) {
    return MysterySubmission.builder()
        .id(id)
        .classroom(classroom)
        .userSubmittedBy(submittedBy)
        .mysDesc("Suspicious message")
        .attachmentUrl("https://example.com/file.png")
        .mysStatus(status)
        .isFeatured(featured)
        .weekNumber(17)
        .weekYear(2026)
        .build();
  }
}
