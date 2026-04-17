package ntnu.no.backend.dto.mystery;

import io.swagger.v3.oas.annotations.media.Schema;
import ntnu.no.backend.model.enums.MysteryStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO representing a mystery submission.
 *
 * @param id unique identifier of the submission
 * @param classroomId unique identifier of the classroom
 * @param submittedBy unique identifier of the student who submitted
 * @param mysDesc description of the mystery
 * @param attachmentUrl optional URL to an attachment
 * @param mysStatus current review status
 * @param reviewedBy unique identifier of the instructor who reviewed, may be null
 * @param reviewedAt timestamp of when the submission was reviewed, may be null
 * @param isFeatured whether the submission has been featured
 * @param weekNumber week number associated with this mystery
 * @param weekYear year associated with this mystery
 * @param submittedAt timestamp of when the submission was created
 *
 * @author tri
 * @version 0.1
 * @since 0.1
 */
@Schema(description = "Represents a mystery submission made by a student")
public record MysterySubmissionResponseDTO(
    UUID id,
    UUID classroomId,
    UUID submittedBy,
    String mysDesc,
    String attachmentUrl,
    MysteryStatus mysStatus,
    UUID reviewedBy,
    LocalDateTime reviewedAt,
    boolean isFeatured,
    int weekNumber,
    int weekYear,
    LocalDateTime submittedAt
) {}
