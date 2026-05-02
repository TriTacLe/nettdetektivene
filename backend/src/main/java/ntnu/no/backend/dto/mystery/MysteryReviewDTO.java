package ntnu.no.backend.dto.mystery;

import io.swagger.v3.oas.annotations.media.Schema;
import ntnu.no.backend.model.enums.MysteryStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Teacher-facing view of a mystery submission. Includes {@code correctIndex}
 * so reviewers can verify the answer makes sense before approving.
 */
@Schema(description = "Teacher-facing mystery submission with correct answer")
public record MysteryReviewDTO(
    UUID id,
    UUID classroomId,
    UUID submittedBy,
    String submittedByName,
    String imagePath,
    List<String> alternatives,
    byte correctIndex,
    MysteryStatus mysStatus,
    UUID reviewedBy,
    LocalDateTime reviewedAt,
    LocalDateTime submittedAt
) {}
