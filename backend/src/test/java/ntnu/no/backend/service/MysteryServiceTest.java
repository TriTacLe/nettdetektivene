package ntnu.no.backend.service;

import jakarta.persistence.EntityNotFoundException;
import ntnu.no.backend.dto.mystery.MysteryCompletionResponseDTO;
import ntnu.no.backend.dto.mystery.MysteryPlayDTO;
import ntnu.no.backend.dto.mystery.MysteryReviewActionDTO;
import ntnu.no.backend.dto.mystery.MysteryReviewDTO;
import ntnu.no.backend.dto.mystery.MysterySubmissionRequestDTO;
import ntnu.no.backend.model.Classroom;
import ntnu.no.backend.model.MysteryCompletion;
import ntnu.no.backend.model.MysterySubmission;
import ntnu.no.backend.model.auth.User;
import ntnu.no.backend.model.classroom.ClassroomStudent;
import ntnu.no.backend.model.enums.MysteryStatus;
import ntnu.no.backend.repository.ClassroomInstructorRepository;
import ntnu.no.backend.repository.ClassroomRepository;
import ntnu.no.backend.repository.ClassroomStudentRepository;
import ntnu.no.backend.repository.MysteryCompletionRepository;
import ntnu.no.backend.repository.MysterySubmissionRepository;
import ntnu.no.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MysteryServiceTest {

  @Mock MysterySubmissionRepository mysterySubmissionRepository;
  @Mock MysteryCompletionRepository mysteryCompletionRepository;
  @Mock ClassroomRepository classroomRepository;
  @Mock UserRepository userRepository;
  @Mock ClassroomStudentRepository classroomStudentRepository;
  @Mock ClassroomInstructorRepository classroomInstructorRepository;
  @Mock MysteryImageStorageService mysteryImageStorageService;

  @InjectMocks
  MysteryService mysteryService;

  @Test
  void submitMystery_savesPendingSubmissionWithImageAndReturnsDTO() {
    UUID classroomId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    Classroom classroom = Classroom.builder().id(classroomId).build();
    User user = User.builder().id(userId).displayName("Student").build();
    MysterySubmission saved = submission(UUID.randomUUID(), classroom, user, MysteryStatus.PENDING);
    MockMultipartFile image = imageFile();

    when(classroomRepository.findByIdAndDeletedAtIsNull(classroomId)).thenReturn(Optional.of(classroom));
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(classroomStudentRepository.findById(any())).thenReturn(Optional.of(ClassroomStudent.builder().isAllowed(true).build()));
    when(mysteryImageStorageService.storeImage(image)).thenReturn("/uploads/mysteries/abc/file.jpg");
    when(mysterySubmissionRepository.save(any(MysterySubmission.class))).thenReturn(saved);

    MysteryReviewDTO result = mysteryService.submitMystery(request(classroomId), image, userId);

    assertThat(result.classroomId()).isEqualTo(classroomId);
    assertThat(result.submittedBy()).isEqualTo(userId);
    assertThat(result.mysStatus()).isEqualTo(MysteryStatus.PENDING);
    assertThat(result.alternatives()).hasSize(3);
    verify(mysterySubmissionRepository).save(any(MysterySubmission.class));
  }

  @Test
  void submitMystery_throwsAccessDenied_whenStudentIsNotApprovedMember() {
    UUID classroomId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    Classroom classroom = Classroom.builder().id(classroomId).build();
    User user = User.builder().id(userId).build();

    when(classroomRepository.findByIdAndDeletedAtIsNull(classroomId)).thenReturn(Optional.of(classroom));
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(classroomStudentRepository.findById(any())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> mysteryService.submitMystery(request(classroomId), imageFile(), userId))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void getPlayList_returnsApprovedExceptOwn_andMarksCompleted() {
    UUID classroomId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    User user = User.builder().id(userId).build();
    Classroom classroom = Classroom.builder().id(classroomId).build();
    User other = User.builder().id(UUID.randomUUID()).build();
    UUID s1 = UUID.randomUUID();
    UUID s2 = UUID.randomUUID();
    MysterySubmission a = submission(s1, classroom, other, MysteryStatus.APPROVED);
    MysterySubmission b = submission(s2, classroom, other, MysteryStatus.APPROVED);

    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(classroomStudentRepository.findById(any())).thenReturn(Optional.of(ClassroomStudent.builder().isAllowed(true).build()));
    when(mysterySubmissionRepository.findByClassroom_IdAndMysStatusAndUserSubmittedBy_IdNot(classroomId, MysteryStatus.APPROVED, userId))
        .thenReturn(List.of(a, b));
    when(mysteryCompletionRepository.findByIdUserId(userId))
        .thenReturn(List.of(completion(userId, s1)));

    List<MysteryPlayDTO> result = mysteryService.getPlayList(classroomId, userId);

    assertThat(result).hasSize(2);
    assertThat(result.getFirst().completed()).isTrue();
    assertThat(result.get(1).completed()).isFalse();
  }

  @Test
  void completeMystery_correctPick_savesCompletionAndReturnsCompleted() {
    UUID submissionId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    UUID classroomId = UUID.randomUUID();
    Classroom classroom = Classroom.builder().id(classroomId).build();
    User other = User.builder().id(UUID.randomUUID()).build();
    User user = User.builder().id(userId).build();
    MysterySubmission existing = submission(submissionId, classroom, other, MysteryStatus.APPROVED);
    existing.setCorrectIndex((byte) 1);

    when(mysterySubmissionRepository.findById(submissionId)).thenReturn(Optional.of(existing));
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(classroomStudentRepository.findById(any())).thenReturn(Optional.of(ClassroomStudent.builder().isAllowed(true).build()));
    when(mysteryCompletionRepository.existsByIdUserIdAndIdSubmissionId(userId, submissionId)).thenReturn(false);

    MysteryCompletionResponseDTO result = mysteryService.completeMystery(submissionId, (byte) 1, userId);

    assertThat(result.correct()).isTrue();
    assertThat(result.completed()).isTrue();
    assertThat(result.alreadyCompleted()).isFalse();
    verify(mysteryCompletionRepository).saveAndFlush(any(MysteryCompletion.class));
  }

  @Test
  void completeMystery_wrongPick_doesNotSave() {
    UUID submissionId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    UUID classroomId = UUID.randomUUID();
    Classroom classroom = Classroom.builder().id(classroomId).build();
    User other = User.builder().id(UUID.randomUUID()).build();
    User user = User.builder().id(userId).build();
    MysterySubmission existing = submission(submissionId, classroom, other, MysteryStatus.APPROVED);
    existing.setCorrectIndex((byte) 0);

    when(mysterySubmissionRepository.findById(submissionId)).thenReturn(Optional.of(existing));
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(classroomStudentRepository.findById(any())).thenReturn(Optional.of(ClassroomStudent.builder().isAllowed(true).build()));
    when(mysteryCompletionRepository.existsByIdUserIdAndIdSubmissionId(userId, submissionId)).thenReturn(false);

    MysteryCompletionResponseDTO result = mysteryService.completeMystery(submissionId, (byte) 2, userId);

    assertThat(result.correct()).isFalse();
    assertThat(result.completed()).isFalse();
    verify(mysteryCompletionRepository, never()).saveAndFlush(any());
  }

  @Test
  void completeMystery_throwsAccessDenied_whenSubmittersOwn() {
    UUID submissionId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    UUID classroomId = UUID.randomUUID();
    Classroom classroom = Classroom.builder().id(classroomId).build();
    User user = User.builder().id(userId).build();
    MysterySubmission existing = submission(submissionId, classroom, user, MysteryStatus.APPROVED);

    when(mysterySubmissionRepository.findById(submissionId)).thenReturn(Optional.of(existing));
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(classroomStudentRepository.findById(any())).thenReturn(Optional.of(ClassroomStudent.builder().isAllowed(true).build()));

    assertThatThrownBy(() -> mysteryService.completeMystery(submissionId, (byte) 0, userId))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void reviewSubmission_approves() {
    UUID submissionId = UUID.randomUUID();
    UUID reviewerId = UUID.randomUUID();
    Classroom classroom = Classroom.builder().id(UUID.randomUUID()).build();
    User student = User.builder().id(UUID.randomUUID()).build();
    User reviewer = User.builder().id(reviewerId).build();
    MysterySubmission existing = submission(submissionId, classroom, student, MysteryStatus.PENDING);

    when(mysterySubmissionRepository.findById(submissionId)).thenReturn(Optional.of(existing));
    when(userRepository.findById(reviewerId)).thenReturn(Optional.of(reviewer));
    when(classroomInstructorRepository.existsById(any())).thenReturn(true);
    when(mysterySubmissionRepository.save(existing)).thenReturn(existing);

    MysteryReviewActionDTO action = MysteryReviewActionDTO.builder().mysStatus(MysteryStatus.APPROVED).build();
    MysteryReviewDTO result = mysteryService.reviewSubmission(submissionId, action, reviewerId);

    assertThat(result.mysStatus()).isEqualTo(MysteryStatus.APPROVED);
    assertThat(result.reviewedBy()).isEqualTo(reviewerId);
  }

  @Test
  void reviewSubmission_throwsAccessDenied_whenReviewerIsNotInstructorOfClassroom() {
    UUID submissionId = UUID.randomUUID();
    UUID reviewerId = UUID.randomUUID();
    Classroom classroom = Classroom.builder().id(UUID.randomUUID()).build();
    User student = User.builder().id(UUID.randomUUID()).build();
    User reviewer = User.builder().id(reviewerId).build();
    MysterySubmission existing = submission(submissionId, classroom, student, MysteryStatus.PENDING);

    when(mysterySubmissionRepository.findById(submissionId)).thenReturn(Optional.of(existing));
    when(userRepository.findById(reviewerId)).thenReturn(Optional.of(reviewer));
    when(classroomInstructorRepository.existsById(any())).thenReturn(false);

    MysteryReviewActionDTO action = MysteryReviewActionDTO.builder().mysStatus(MysteryStatus.APPROVED).build();
    assertThatThrownBy(() -> mysteryService.reviewSubmission(submissionId, action, reviewerId))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void getSubmissionsByClassroom_throwsAccessDenied_whenNotInstructor() {
    UUID classroomId = UUID.randomUUID();
    UUID instructorId = UUID.randomUUID();

    when(classroomInstructorRepository.existsById(any())).thenReturn(false);

    assertThatThrownBy(() -> mysteryService.getSubmissionsByClassroom(classroomId, instructorId))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void getPendingSubmissions_throwsAccessDenied_whenNotInstructor() {
    UUID classroomId = UUID.randomUUID();
    UUID instructorId = UUID.randomUUID();

    when(classroomInstructorRepository.existsById(any())).thenReturn(false);

    assertThatThrownBy(() -> mysteryService.getPendingSubmissions(classroomId, instructorId))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void completeMystery_correctPickRaceLost_returnsAlreadyCompleted() {
    UUID submissionId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    UUID classroomId = UUID.randomUUID();
    Classroom classroom = Classroom.builder().id(classroomId).build();
    User other = User.builder().id(UUID.randomUUID()).build();
    User user = User.builder().id(userId).build();
    MysterySubmission existing = submission(submissionId, classroom, other, MysteryStatus.APPROVED);
    existing.setCorrectIndex((byte) 1);

    when(mysterySubmissionRepository.findById(submissionId)).thenReturn(Optional.of(existing));
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(classroomStudentRepository.findById(any())).thenReturn(Optional.of(ClassroomStudent.builder().isAllowed(true).build()));
    when(mysteryCompletionRepository.existsByIdUserIdAndIdSubmissionId(userId, submissionId)).thenReturn(false);
    when(mysteryCompletionRepository.saveAndFlush(any(MysteryCompletion.class)))
        .thenThrow(new DataIntegrityViolationException("Duplicate primary key (parallel insert)"));

    MysteryCompletionResponseDTO result = mysteryService.completeMystery(submissionId, (byte) 1, userId);

    assertThat(result.correct()).isTrue();
    assertThat(result.completed()).isTrue();
    assertThat(result.alreadyCompleted()).isTrue();
  }

  @Test
  void reviewSubmission_throwsIllegalArgumentException_whenStatusIsPending() {
    UUID submissionId = UUID.randomUUID();
    UUID reviewerId = UUID.randomUUID();
    MysterySubmission existing = submission(
        submissionId,
        Classroom.builder().id(UUID.randomUUID()).build(),
        User.builder().id(UUID.randomUUID()).build(),
        MysteryStatus.PENDING
    );

    when(mysterySubmissionRepository.findById(submissionId)).thenReturn(Optional.of(existing));
    when(userRepository.findById(reviewerId)).thenReturn(Optional.of(User.builder().id(reviewerId).build()));
    when(classroomInstructorRepository.existsById(any())).thenReturn(true);

    assertThatThrownBy(() -> mysteryService.reviewSubmission(
        submissionId,
        MysteryReviewActionDTO.builder().mysStatus(MysteryStatus.PENDING).build(),
        reviewerId
    )).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void getMysteryForPlay_returnsPlayDTO_whenApprovedAndNotOwn() {
    UUID submissionId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    UUID classroomId = UUID.randomUUID();
    Classroom classroom = Classroom.builder().id(classroomId).build();
    User other = User.builder().id(UUID.randomUUID()).build();
    User user = User.builder().id(userId).build();
    MysterySubmission existing = submission(submissionId, classroom, other, MysteryStatus.APPROVED);

    when(mysterySubmissionRepository.findById(submissionId)).thenReturn(Optional.of(existing));
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(classroomStudentRepository.findById(any())).thenReturn(Optional.of(ClassroomStudent.builder().isAllowed(true).build()));
    when(mysteryCompletionRepository.existsByIdUserIdAndIdSubmissionId(userId, submissionId)).thenReturn(false);

    MysteryPlayDTO result = mysteryService.getMysteryForPlay(submissionId, userId);

    assertThat(result.id()).isEqualTo(submissionId);
    assertThat(result.alternatives()).hasSize(3);
    assertThat(result.completed()).isFalse();
  }

  @Test
  void getMysteryForPlay_throwsAccessDenied_whenNotApproved() {
    UUID submissionId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    UUID classroomId = UUID.randomUUID();
    Classroom classroom = Classroom.builder().id(classroomId).build();
    User other = User.builder().id(UUID.randomUUID()).build();
    User user = User.builder().id(userId).build();
    MysterySubmission existing = submission(submissionId, classroom, other, MysteryStatus.PENDING);

    when(mysterySubmissionRepository.findById(submissionId)).thenReturn(Optional.of(existing));
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(classroomStudentRepository.findById(any())).thenReturn(Optional.of(ClassroomStudent.builder().isAllowed(true).build()));

    assertThatThrownBy(() -> mysteryService.getMysteryForPlay(submissionId, userId))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void getMysteryForPlay_throwsEntityNotFound_whenMissing() {
    UUID submissionId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    when(mysterySubmissionRepository.findById(submissionId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> mysteryService.getMysteryForPlay(submissionId, userId))
        .isInstanceOf(EntityNotFoundException.class);
  }

  private MysterySubmissionRequestDTO request(UUID classroomId) {
    return MysterySubmissionRequestDTO.builder()
        .classroomId(classroomId)
        .alternative1("A")
        .alternative2("B")
        .alternative3("C")
        .correctIndex((byte) 0)
        .build();
  }

  private MysterySubmission submission(UUID id, Classroom classroom, User submittedBy, MysteryStatus status) {
    return MysterySubmission.builder()
        .id(id)
        .classroom(classroom)
        .userSubmittedBy(submittedBy)
        .imagePath("/uploads/mysteries/x/y.jpg")
        .alternative1("A")
        .alternative2("B")
        .alternative3("C")
        .correctIndex((byte) 0)
        .mysStatus(status)
        .build();
  }

  private MysteryCompletion completion(UUID userId, UUID submissionId) {
    MysteryCompletion mc = new MysteryCompletion();
    mc.setId(new ntnu.no.backend.model.MysteryCompletionId(userId, submissionId));
    return mc;
  }

  private MockMultipartFile imageFile() {
    return new MockMultipartFile("image", "x.jpg", "image/jpeg", new byte[]{1, 2, 3});
  }
}
