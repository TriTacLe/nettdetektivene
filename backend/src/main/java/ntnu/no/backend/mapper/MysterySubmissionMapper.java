package ntnu.no.backend.mapper;

import ntnu.no.backend.dto.mystery.MysterySubmissionResponseDTO;
import ntnu.no.backend.model.MysterySubmission;

/**
 * Utility class for mapping between {@link MysterySubmission} entities and related DTOs.
 *
 * @author tri
 * @version 0.1
 * @since 0.1
 * @see MysterySubmission
 * @see MysterySubmissionResponseDTO
 */
public class MysterySubmissionMapper {

  private MysterySubmissionMapper() {}

  /**
   * Converts a {@link MysterySubmission} entity to a {@link MysterySubmissionResponseDTO}.
   *
   * @param submission the mystery submission entity to convert
   * @return a {@link MysterySubmissionResponseDTO}, or {@code null} if {@code submission} is null
   */
  public static MysterySubmissionResponseDTO toDTO(MysterySubmission submission) {
    if (submission == null) return null;
    return new MysterySubmissionResponseDTO(
      submission.getId(),
      submission.getClassroom().getId(),
      submission.getUserSubmittedBy().getId(),
      submission.getMysDesc(),
      submission.getAttachmentUrl(),
      submission.getMysStatus(),
      submission.getUserReviewedBy() != null ? submission.getUserReviewedBy().getId() : null,
      submission.getReviewedAt(),
      submission.isFeatured(),
      submission.getWeekNumber(),
      submission.getWeekYear(),
      submission.getSubmittedAt()
    );
  }
}
