package ntnu.no.backend.mapper;

import ntnu.no.backend.dto.mystery.MysteryPlayDTO;
import ntnu.no.backend.dto.mystery.MysteryReviewDTO;
import ntnu.no.backend.model.MysterySubmission;

import java.util.List;

/**
 * Maps {@link MysterySubmission} entities to the audience-specific DTOs.
 *
 * <p>Two distinct DTOs exist on purpose: {@link MysteryPlayDTO} for students
 * (no {@code correctIndex}) and {@link MysteryReviewDTO} for teachers
 * (includes {@code correctIndex}). Mixing them up would leak the answer.</p>
 */
public class MysterySubmissionMapper {

  private MysterySubmissionMapper() {}

  public static MysteryPlayDTO toPlayDto(MysterySubmission submission, boolean completed) {
    if (submission == null) return null;
    return new MysteryPlayDTO(
        submission.getId(),
        submission.getClassroom().getId(),
        submission.getImagePath(),
        List.of(submission.getAlternative1(), submission.getAlternative2(), submission.getAlternative3()),
        completed
    );
  }

  public static MysteryReviewDTO toReviewDto(MysterySubmission submission) {
    if (submission == null) return null;
    return new MysteryReviewDTO(
        submission.getId(),
        submission.getClassroom().getId(),
        submission.getUserSubmittedBy().getId(),
        submission.getUserSubmittedBy().getDisplayName(),
        submission.getImagePath(),
        List.of(submission.getAlternative1(), submission.getAlternative2(), submission.getAlternative3()),
        submission.getCorrectIndex(),
        submission.getMysStatus(),
        submission.getUserReviewedBy() != null ? submission.getUserReviewedBy().getId() : null,
        submission.getReviewedAt(),
        submission.getSubmittedAt()
    );
  }
}
