package ntnu.no.backend.dto.classroom;

import io.swagger.v3.oas.annotations.media.Schema;
import ntnu.no.backend.model.enums.NameChangeStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO representing a student's membership in a classroom.
 *
 * @param userId the student's user ID, used for admin operations
 * @param displayName the student's display name in this classroom
 * @param isAllowed whether the student has been approved by an instructor
 * @param nameChangeStatus the current status of a display name change request
 * @param requestedDisplayName the display name the student has requested, if any
 * @param joinedAt timestamp of when the student joined the classroom
 *
 * @author norakirknes
 * @version 0.1
 * @since 0.1
 */
@Schema(description = "Represents a student's membership in a classroom")
public record ClassroomStudentResponseDTO(
    UUID userId,
    String displayName,
    boolean isAllowed,
    NameChangeStatus nameChangeStatus,
    String requestedDisplayName,
    LocalDateTime joinedAt
) {}
